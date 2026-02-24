package com.example.free2party.data.repository

import android.net.Uri
import com.example.free2party.data.model.User
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.exception.UserNotFoundException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {
    private lateinit var repository: UserRepositoryImpl
    private val auth: FirebaseAuth = mockk()
    private val db: FirebaseFirestore = mockk()
    private val storage: FirebaseStorage = mockk()
    private val firebaseUser: FirebaseUser = mockk()
    private val usersCollection: CollectionReference = mockk()
    private val userDoc: DocumentReference = mockk()

    @Before
    fun setup() {
        every { db.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns userDoc
        
        repository = UserRepositoryImpl(auth, db, storage)
        
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkStatic(Uri::class)
    }

    @Test
    fun `getUserById fails if user not logged in`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.getUserById("uid")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `getUserById returns user when successful`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "me"
        
        val user = User(uid = "target", firstName = "John")
        val docSnapshot: DocumentSnapshot = mockk()
        every { userDoc.get() } returns Tasks.forResult(docSnapshot)
        every { docSnapshot.exists() } returns true
        every { docSnapshot.toObject(User::class.java) } returns user
        every { docSnapshot.id } returns "target"

        val result = repository.getUserById("target")
        assertTrue(result.isSuccess)
        assertEquals("John", result.getOrNull()?.firstName)
    }

    @Test
    fun `getUserById fails when user does not exist`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "me"
        
        val docSnapshot: DocumentSnapshot = mockk()
        every { userDoc.get() } returns Tasks.forResult(docSnapshot)
        every { docSnapshot.exists() } returns false

        val result = repository.getUserById("nonexistent")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserNotFoundException)
    }

    @Test
    fun `getUserByEmail returns user when successful`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "me"
        
        val email = "john@test.com"
        val user = User(uid = "target", email = email)
        val querySnapshot: QuerySnapshot = mockk()
        every { usersCollection.whereEqualTo("email", email).get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.documents } returns listOf(mockk {
            every { toObject(User::class.java) } returns user
            every { id } returns "target"
        })

        val result = repository.getUserByEmail(email)
        assertTrue(result.isSuccess)
        assertEquals("target", result.getOrNull()?.uid)
    }

    @Test
    fun `updateUser fails if session invalid`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.updateUser(User())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `updateUser success for valid data`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "me"
        
        every { userDoc.set(any(), SetOptions.merge()) } returns Tasks.forResult(null)

        val result = repository.updateUser(User(firstName = "Jane"))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `toggleAvailability success`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "me"
        
        every { userDoc.set(any<Map<String, Any>>(), SetOptions.merge()) } returns Tasks.forResult(null)

        val result = repository.toggleAvailability(true)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `uploadProfilePicture success`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "me"
        
        val uri: Uri = mockk()
        val storageRef: StorageReference = mockk()
        val profilePicRef: StorageReference = mockk()
        val uploadTask: UploadTask = mockk()
        
        every { storage.reference } returns storageRef
        every { storageRef.child("profile_pictures/me.jpg") } returns profilePicRef
        every { profilePicRef.putFile(uri) } returns uploadTask
        
        // Properly mock the await() for StorageTask/UploadTask
        coEvery { uploadTask.await() } returns mockk()
        
        val downloadUrl = "https://test.com/pic.jpg"
        val mockUri = mockk<Uri>()
        every { Uri.parse(downloadUrl) } returns mockUri
        every { mockUri.toString() } returns downloadUrl
        every { profilePicRef.downloadUrl } returns Tasks.forResult(mockUri)

        val result = repository.uploadProfilePicture(uri)
        assertTrue(result.isSuccess)
        assertEquals(downloadUrl, result.getOrNull())
    }

    @Test
    fun `getCurrentUserStatus emits status correctly`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "me"
        
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        every { userDoc.addSnapshotListener(capture(listenerSlot)) } returns registration

        val statusFlow = repository.getCurrentUserStatus()
        
        val snapshot = mockk<DocumentSnapshot>()
        every { snapshot.getBoolean("isFreeNow") } returns true
        
        launch(UnconfinedTestDispatcher()) {
            val isFree = statusFlow.first()
            assertEquals(true, isFree)
        }
        
        listenerSlot.captured.onEvent(snapshot, null)
    }

    @Test
    fun `observeUser emits user correctly`() = runTest {
        val uid = "some_user"
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        every { usersCollection.document(uid) } returns userDoc
        every { userDoc.addSnapshotListener(capture(listenerSlot)) } returns registration

        val userFlow = repository.observeUser(uid)
        
        val user = User(uid = uid, firstName = "Test")
        val snapshot = mockk<DocumentSnapshot>()
        every { snapshot.toObject(User::class.java) } returns user
        every { snapshot.id } returns uid

        launch(UnconfinedTestDispatcher()) {
            val result = userFlow.first()
            assertEquals("Test", result.firstName)
        }
        
        listenerSlot.captured.onEvent(snapshot, null)
    }

    @Test
    fun `createUserProfile success`() = runTest {
        val user = User(uid = "new_user", email = "new@test.com")
        every { usersCollection.document("new_user") } returns userDoc
        every { userDoc.set(user, any()) } returns Tasks.forResult(null)

        val result = repository.createUserProfile(user)
        assertTrue(result.isSuccess)
    }
}

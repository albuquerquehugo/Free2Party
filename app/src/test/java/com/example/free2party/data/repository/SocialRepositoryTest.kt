package com.example.free2party.data.repository

import android.util.Log
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.data.model.User
import com.example.free2party.exception.CannotAddSelfException
import com.example.free2party.exception.FriendRequestAlreadyAcceptedException
import com.example.free2party.exception.FriendRequestAlreadySentException
import com.example.free2party.exception.UnauthorizedException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SocialRepositoryTest {
    private lateinit var repository: SocialRepositoryImpl
    private val db: FirebaseFirestore = mockk()
    private val userRepository: UserRepository = mockk()
    private val usersCollection: CollectionReference = mockk()
    private val userDoc: DocumentReference = mockk()
    private val friendsCollection: CollectionReference = mockk()
    private val friendDoc: DocumentReference = mockk()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        
        every { userRepository.currentUserId } returns "me123"
        every { db.collection("users") } returns usersCollection
        every { usersCollection.document("me123") } returns userDoc
        every { userDoc.collection("friends") } returns friendsCollection
        
        repository = SocialRepositoryImpl(db, userRepository)
        
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkStatic(FieldValue::class)
        every { FieldValue.serverTimestamp() } returns mockk()
    }

    @Test
    fun `sendFriendRequest fails if user not logged in`() = runTest {
        every { userRepository.currentUserId } returns ""
        val result = repository.sendFriendRequest("test@test.com")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `sendFriendRequest fails if adding self`() = runTest {
        val email = "me@test.com"
        val me = User(uid = "me123", email = email)
        coEvery { userRepository.getUserByEmail(email) } returns Result.success(me)

        val result = repository.sendFriendRequest(email)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CannotAddSelfException)
    }

    @Test
    fun `sendFriendRequest fails if already accepted friends`() = runTest {
        val targetEmail = "friend@test.com"
        val targetUser = User(uid = "friend123", email = targetEmail, firstName = "Friend", lastName = "User")
        val me = User(uid = "me123", firstName = "Me", lastName = "Too")
        
        coEvery { userRepository.getUserByEmail(targetEmail) } returns Result.success(targetUser)
        coEvery { userRepository.getUserById("me123") } returns Result.success(me)
        
        val blockedByReceiverRef = mockk<DocumentReference>()
        val blockedByCurrentUserRef = mockk<DocumentReference>()
        val existingFriendRef = mockk<DocumentReference>()
        
        every { db.collection("users").document("friend123").collection("blocked").document("me123") } returns blockedByReceiverRef
        every { db.collection("users").document("me123").collection("blocked").document("friend123") } returns blockedByCurrentUserRef
        every { db.collection("users").document("me123").collection("friends").document("friend123") } returns existingFriendRef
        
        val blockedByReceiverDoc = mockk<DocumentSnapshot>()
        val blockedByCurrentUserDoc = mockk<DocumentSnapshot>()
        val existingFriendDoc = mockk<DocumentSnapshot>()
        
        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            
            every { transaction.get(blockedByReceiverRef) } returns blockedByReceiverDoc
            every { transaction.get(blockedByCurrentUserRef) } returns blockedByCurrentUserDoc
            every { transaction.get(existingFriendRef) } returns existingFriendDoc
            
            every { blockedByReceiverDoc.exists() } returns false
            every { blockedByCurrentUserDoc.exists() } returns false
            every { existingFriendDoc.exists() } returns true
            every { existingFriendDoc.getString("inviteStatus") } returns InviteStatus.ACCEPTED.name
            
            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.sendFriendRequest(targetEmail)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FriendRequestAlreadyAcceptedException)
    }

    @Test
    fun `sendFriendRequest fails if already sent request`() = runTest {
        val targetEmail = "friend@test.com"
        val targetUser = User(uid = "friend123", email = targetEmail, firstName = "Friend", lastName = "User")
        val me = User(uid = "me123", firstName = "Me", lastName = "Too")
        
        coEvery { userRepository.getUserByEmail(targetEmail) } returns Result.success(targetUser)
        coEvery { userRepository.getUserById("me123") } returns Result.success(me)

        val blockedByReceiverRef = mockk<DocumentReference>()
        val blockedByCurrentUserRef = mockk<DocumentReference>()
        val existingFriendRef = mockk<DocumentReference>()
        
        every { db.collection("users").document("friend123").collection("blocked").document("me123") } returns blockedByReceiverRef
        every { db.collection("users").document("me123").collection("blocked").document("friend123") } returns blockedByCurrentUserRef
        every { db.collection("users").document("me123").collection("friends").document("friend123") } returns existingFriendRef
        
        val blockedByReceiverDoc = mockk<DocumentSnapshot>()
        val blockedByCurrentUserDoc = mockk<DocumentSnapshot>()
        val existingFriendDoc = mockk<DocumentSnapshot>()
        
        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            
            every { transaction.get(blockedByReceiverRef) } returns blockedByReceiverDoc
            every { transaction.get(blockedByCurrentUserRef) } returns blockedByCurrentUserDoc
            every { transaction.get(existingFriendRef) } returns existingFriendDoc
            
            every { blockedByReceiverDoc.exists() } returns false
            every { blockedByCurrentUserDoc.exists() } returns false
            every { existingFriendDoc.exists() } returns true
            every { existingFriendDoc.getString("inviteStatus") } returns InviteStatus.INVITED.name
            
            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.sendFriendRequest(targetEmail)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FriendRequestAlreadySentException)
    }

    @Test
    fun `cancelFriendRequest calls transaction correctly`() = runTest {
        val friendId = "friend123"
        val requestId = "me123_$friendId"
        val friendRequestsCollection: CollectionReference = mockk()
        val requestDoc: DocumentReference = mockk()
        
        every { db.collection("friendRequests") } returns friendRequestsCollection
        every { friendRequestsCollection.document(requestId) } returns requestDoc
        every { friendsCollection.document(friendId) } returns friendDoc

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            function.apply(transaction)
            Tasks.forResult(Unit)
        }
        every { transaction.delete(any()) } returns transaction

        val result = repository.cancelFriendRequest(friendId)
        
        assertTrue(result.isSuccess)
        verify { transaction.delete(requestDoc) }
        verify { transaction.delete(friendDoc) }
    }

    @Test
    fun `removeFriend removes both directions and requests`() = runTest {
        val friendId = "friend123"
        val me = User(uid = "me123", firstName = "Me", lastName = "Too", email = "me@test.com")
        coEvery { userRepository.getUserById("me123") } returns Result.success(me)
        
        val otherUserDoc: DocumentReference = mockk()
        val otherFriendsCollection: CollectionReference = mockk()
        val otherFriendDoc: DocumentReference = mockk()
        val otherNotificationsCollection: CollectionReference = mockk()
        
        every { usersCollection.document(friendId) } returns otherUserDoc
        every { otherUserDoc.collection("friends") } returns otherFriendsCollection
        every { otherUserDoc.collection("notifications") } returns otherNotificationsCollection
        every { otherNotificationsCollection.document() } returns mockk(relaxed = true)
        every { otherFriendsCollection.document("me123") } returns otherFriendDoc
        every { friendsCollection.document(friendId) } returns friendDoc
        
        val friendRequestsCollection: CollectionReference = mockk()
        every { db.collection("friendRequests") } returns friendRequestsCollection
        every { friendRequestsCollection.document(any()) } returns mockk(relaxed = true)

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            
            every { transaction.delete(any()) } returns transaction
            every { transaction.set(any(), any()) } returns transaction

            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.removeFriend(friendId)
        
        assertTrue(result.isSuccess)
    }
}

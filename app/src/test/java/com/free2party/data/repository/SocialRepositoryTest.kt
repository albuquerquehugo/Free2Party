package com.free2party.data.repository

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.free2party.data.model.InviteStatus
import com.free2party.data.model.User
import com.free2party.exception.CannotAddSelfException
import com.free2party.exception.FriendRequestAlreadyAcceptedException
import com.free2party.exception.FriendRequestAlreadySentException
import com.free2party.exception.UnauthorizedException
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
import com.google.firebase.firestore.QuerySnapshot

@OptIn(ExperimentalCoroutinesApi::class)
class SocialRepositoryTest {
    private lateinit var repository: SocialRepositoryImpl
    private val db: FirebaseFirestore = mockk()
    private val userRepository: UserRepository = mockk()
    private val planRepository: PlanRepository = mockk()
    private val usersCollection: CollectionReference = mockk()
    private val userDoc: DocumentReference = mockk()
    private val friendsCollection: CollectionReference = mockk()
    private val friendDoc: DocumentReference = mockk()

    private val context: Context = mockk()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        
        every { userRepository.currentUserId } returns "me123"
        every { db.collection("users") } returns usersCollection
        every { usersCollection.document("me123") } returns userDoc
        every { userDoc.collection("friends") } returns friendsCollection
        
        repository = SocialRepositoryImpl(db, userRepository, planRepository, context)
        
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkStatic(FieldValue::class)
        every { FieldValue.serverTimestamp() } returns mockk()

        every { context.getString(any()) } returns "Mock String"
        every { context.getString(any(), any()) } returns "Mock String"
        every { context.getString(any(), any(), any()) } returns "Mock String"

        val resources: Resources = mockk()
        every { context.resources } returns resources
        every { resources.getString(any()) } returns "Mock String"
        every { resources.getString(any(), any()) } returns "Mock String"
        every { resources.getString(any(), any(), any()) } returns "Mock String"
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
        val incomingRequestRef = mockk<DocumentReference>()
        
        every { db.collection("users").document("friend123").collection("blocked").document("me123") } returns blockedByReceiverRef
        every { db.collection("users").document("me123").collection("blocked").document("friend123") } returns blockedByCurrentUserRef
        every { db.collection("users").document("me123").collection("friends").document("friend123") } returns existingFriendRef
        every { db.collection("friendRequests").document("friend123_me123") } returns incomingRequestRef
        
        val blockedByReceiverDoc = mockk<DocumentSnapshot>()
        val blockedByCurrentUserDoc = mockk<DocumentSnapshot>()
        val existingFriendDoc = mockk<DocumentSnapshot>()
        val incomingRequestDoc = mockk<DocumentSnapshot>()
        
        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            
            every { transaction.get(blockedByReceiverRef) } returns blockedByReceiverDoc
            every { transaction.get(blockedByCurrentUserRef) } returns blockedByCurrentUserDoc
            every { transaction.get(existingFriendRef) } returns existingFriendDoc
            every { transaction.get(incomingRequestRef) } returns incomingRequestDoc
            
            every { blockedByReceiverDoc.exists() } returns false
            every { blockedByCurrentUserDoc.exists() } returns false
            every { existingFriendDoc.exists() } returns true
            every { existingFriendDoc.getString("inviteStatus") } returns InviteStatus.ACCEPTED.name
            every { incomingRequestDoc.exists() } returns false
            
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
        val incomingRequestRef = mockk<DocumentReference>()
        
        every { db.collection("users").document("friend123").collection("blocked").document("me123") } returns blockedByReceiverRef
        every { db.collection("users").document("me123").collection("blocked").document("friend123") } returns blockedByCurrentUserRef
        every { db.collection("users").document("me123").collection("friends").document("friend123") } returns existingFriendRef
        every { db.collection("friendRequests").document("friend123_me123") } returns incomingRequestRef
        
        val blockedByReceiverDoc = mockk<DocumentSnapshot>()
        val blockedByCurrentUserDoc = mockk<DocumentSnapshot>()
        val existingFriendDoc = mockk<DocumentSnapshot>()
        val incomingRequestDoc = mockk<DocumentSnapshot>()
        
        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            
            every { transaction.get(blockedByReceiverRef) } returns blockedByReceiverDoc
            every { transaction.get(blockedByCurrentUserRef) } returns blockedByCurrentUserDoc
            every { transaction.get(existingFriendRef) } returns existingFriendDoc
            every { transaction.get(incomingRequestRef) } returns incomingRequestDoc
            
            every { blockedByReceiverDoc.exists() } returns false
            every { blockedByCurrentUserDoc.exists() } returns false
            every { existingFriendDoc.exists() } returns true
            every { existingFriendDoc.getString("inviteStatus") } returns InviteStatus.INVITED.name
            every { incomingRequestDoc.exists() } returns false
            
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

        val circlesCollection = mockk<CollectionReference>()
        val circlesQuery = mockk<com.google.firebase.firestore.Query>()
        val circlesSnapshot = mockk<QuerySnapshot> {
            every { documents } returns emptyList()
            every { isEmpty } returns true
        }
        every { userDoc.collection("circles") } returns circlesCollection
        every { circlesCollection.whereArrayContains("friendIds", friendId) } returns circlesQuery
        every { circlesQuery.get() } returns Tasks.forResult(circlesSnapshot)

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            
            every { transaction.delete(any()) } returns transaction
            every { transaction.set(any(), any()) } returns transaction

            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.removeFriend(friendId)
        
        if (result.isFailure) {
            println("Test failed with: ${result.exceptionOrNull()}")
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `searchUsers normalizes query and calls firestore with searchKeywords array-contains`() = runTest {
        val query = "João"
        val normalizedQuery = "joao"
        
        coEvery { userRepository.getUserById("me123") } returns Result.success(User(uid = "me123", countryCode = "US"))

        val querySnapshot: QuerySnapshot = mockk {
            every { documents } returns emptyList()
            every { size() } returns 0
        }

        val mockQuery = mockk<com.google.firebase.firestore.Query> {
            every { whereArrayContains("searchKeywords", normalizedQuery) } returns this
            every { limit(any()) } returns this
            every { get() } returns Tasks.forResult(querySnapshot)
        }

        every { usersCollection.whereArrayContains("searchKeywords", normalizedQuery) } returns mockQuery

        // Mock friends and blocked sub-collections for the current user
        val friendsSubCollection = mockk<CollectionReference>()
        val blockedSubCollection = mockk<CollectionReference>()
        val emptySnapshot = mockk<QuerySnapshot> {
            every { documents } returns emptyList()
        }

        every { userDoc.collection("friends") } returns friendsSubCollection
        every { userDoc.collection("blocked") } returns blockedSubCollection
        every { friendsSubCollection.get() } returns Tasks.forResult(emptySnapshot)
        every { blockedSubCollection.get() } returns Tasks.forResult(emptySnapshot)

        val result = repository.searchUsers(query)

        assertTrue(result.isSuccess)
        verify { usersCollection.whereArrayContains("searchKeywords", normalizedQuery) }
    }

    @Test
    fun `searchUsers scores and sorts results correctly based on match strength, country and profile pic`() = runTest {
        val query = "Alice"
        val normalizedQuery = "alice"
        
        coEvery { userRepository.getUserById("me123") } returns Result.success(User(uid = "me123", countryCode = "US"))

        // Mock candidates
        val doc1 = mockk<DocumentSnapshot> {
            every { id } returns "alice1"
            every { toObject(User::class.java) } returns User(
                uid = "alice1",
                firstName = "Alice",
                lastName = "Green",
                countryCode = "US",
                profilePicUrl = ""
            )
        }
        val doc2 = mockk<DocumentSnapshot> {
            every { id } returns "alice2"
            every { toObject(User::class.java) } returns User(
                uid = "alice2",
                firstName = "Alice",
                lastName = "Blue",
                countryCode = "CA",
                profilePicUrl = "pic_url"
            )
        }
        val doc3 = mockk<DocumentSnapshot> {
            every { id } returns "bob"
            every { toObject(User::class.java) } returns User(
                uid = "bob",
                firstName = "Bob",
                lastName = "Alice",
                countryCode = "US",
                profilePicUrl = ""
            )
        }
        val doc4 = mockk<DocumentSnapshot> {
            every { id } returns "alicia"
            every { toObject(User::class.java) } returns User(
                uid = "alicia",
                firstName = "Alicia",
                lastName = "White",
                countryCode = "US",
                profilePicUrl = ""
            )
        }

        val querySnapshot: QuerySnapshot = mockk {
            every { documents } returns listOf(doc4, doc2, doc3, doc1) // unsorted
            every { size() } returns 4
        }

        val mockQuery = mockk<com.google.firebase.firestore.Query> {
            every { whereArrayContains("searchKeywords", normalizedQuery) } returns this
            every { limit(any()) } returns this
            every { get() } returns Tasks.forResult(querySnapshot)
        }

        every { usersCollection.whereArrayContains("searchKeywords", normalizedQuery) } returns mockQuery

        val friendsSubCollection = mockk<CollectionReference>()
        val blockedSubCollection = mockk<CollectionReference>()
        val emptySnapshot = mockk<QuerySnapshot> {
            every { documents } returns emptyList()
        }

        every { userDoc.collection("friends") } returns friendsSubCollection
        every { userDoc.collection("blocked") } returns blockedSubCollection
        every { friendsSubCollection.get() } returns Tasks.forResult(emptySnapshot)
        every { blockedSubCollection.get() } returns Tasks.forResult(emptySnapshot)

        val result = repository.searchUsers(query)

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        
        // Expected order: 
        // 1. alice1 (Score 1100 - Exact first name match + same country)
        // 2. alice2 (Score 1050 - Exact first name match + profile pic)
        // 3. bob (Score 900 - Exact last name match + same country)
        // 4. alicia (Score 600 - Prefix match + same country)
        org.junit.Assert.assertEquals(4, list.size)
        org.junit.Assert.assertEquals("alice1", list[0].uid)
        org.junit.Assert.assertEquals("alice2", list[1].uid)
        org.junit.Assert.assertEquals("bob", list[2].uid)
        org.junit.Assert.assertEquals("alicia", list[3].uid)
    }

    @Test
    fun `searchUsers scores and sorts results based on relationship status`() = runTest {
        val query = "Alice"
        val normalizedQuery = "alice"
        
        coEvery { userRepository.getUserById("me123") } returns Result.success(User(uid = "me123", countryCode = "US"))

        // Mock candidates
        val doc1 = mockk<DocumentSnapshot> {
            every { id } returns "alice1"
            every { toObject(User::class.java) } returns User(
                uid = "alice1",
                firstName = "Alice",
                lastName = "Green",
                countryCode = "US"
            )
        }
        val doc2 = mockk<DocumentSnapshot> {
            every { id } returns "alice2"
            every { toObject(User::class.java) } returns User(
                uid = "alice2",
                firstName = "Alice",
                lastName = "Blue",
                countryCode = "CA"
            )
        }
        val doc3 = mockk<DocumentSnapshot> {
            every { id } returns "alice3"
            every { toObject(User::class.java) } returns User(
                uid = "alice3",
                firstName = "Alice",
                lastName = "Red",
                countryCode = "US"
            )
        }
        val doc4 = mockk<DocumentSnapshot> {
            every { id } returns "alice4"
            every { toObject(User::class.java) } returns User(
                uid = "alice4",
                firstName = "Alice",
                lastName = "Yellow",
                countryCode = "CA"
            )
        }

        val querySnapshot: QuerySnapshot = mockk {
            every { documents } returns listOf(doc3, doc4, doc1, doc2)
            every { size() } returns 4
        }

        val mockQuery = mockk<com.google.firebase.firestore.Query> {
            every { whereArrayContains("searchKeywords", normalizedQuery) } returns this
            every { limit(any()) } returns this
            every { get() } returns Tasks.forResult(querySnapshot)
        }

        every { usersCollection.whereArrayContains("searchKeywords", normalizedQuery) } returns mockQuery

        val friendsSubCollection = mockk<CollectionReference>()
        val friendsSnapshot = mockk<QuerySnapshot> {
            val fDoc2 = mockk<DocumentSnapshot> {
                every { id } returns "alice2"
                every { getString("inviteStatus") } returns InviteStatus.ACCEPTED.name
            }
            val fDoc4 = mockk<DocumentSnapshot> {
                every { id } returns "alice4"
                every { getString("inviteStatus") } returns InviteStatus.INVITED.name
            }
            every { documents } returns listOf(fDoc2, fDoc4)
        }
        every { userDoc.collection("friends") } returns friendsSubCollection
        every { friendsSubCollection.get() } returns Tasks.forResult(friendsSnapshot)

        val blockedSubCollection = mockk<CollectionReference>()
        val blockedSnapshot = mockk<QuerySnapshot> {
            val bDoc3 = mockk<DocumentSnapshot> {
                every { id } returns "alice3"
            }
            every { documents } returns listOf(bDoc3)
        }
        every { userDoc.collection("blocked") } returns blockedSubCollection
        every { blockedSubCollection.get() } returns Tasks.forResult(blockedSnapshot)

        val result = repository.searchUsers(query)

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        
        org.junit.Assert.assertEquals(4, list.size)
        org.junit.Assert.assertEquals("alice2", list[0].uid)
        org.junit.Assert.assertEquals("alice4", list[1].uid)
        org.junit.Assert.assertEquals("alice1", list[2].uid)
        org.junit.Assert.assertEquals("alice3", list[3].uid)
    }
}

package com.example.free2party.data.repository

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
    private val planRepository: PlanRepository = mockk()
    private val usersCollection: CollectionReference = mockk()
    private val userDoc: DocumentReference = mockk()
    private val friendsCollection: CollectionReference = mockk()
    private val friendDoc: DocumentReference = mockk()

    @Before
    fun setup() {
        every { userRepository.currentUserId } returns "me123"
        every { db.collection("users") } returns usersCollection
        every { usersCollection.document("me123") } returns userDoc
        every { userDoc.collection("friends") } returns friendsCollection
        
        repository = SocialRepositoryImpl(db, userRepository, planRepository)
        
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
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
        val targetUser = User(uid = "friend123", email = targetEmail)
        
        coEvery { userRepository.getUserByEmail(targetEmail) } returns Result.success(targetUser)
        every { friendsCollection.document("friend123") } returns friendDoc
        val friendSnapshot = mockk<DocumentSnapshot>()
        every { friendDoc.get() } returns Tasks.forResult(friendSnapshot)
        every { friendSnapshot.exists() } returns true
        every { friendSnapshot.getString("inviteStatus") } returns InviteStatus.ACCEPTED.name

        val result = repository.sendFriendRequest(targetEmail)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FriendRequestAlreadyAcceptedException)
    }

    @Test
    fun `sendFriendRequest fails if already sent request`() = runTest {
        val targetEmail = "friend@test.com"
        val targetUser = User(uid = "friend123", email = targetEmail)
        
        coEvery { userRepository.getUserByEmail(targetEmail) } returns Result.success(targetUser)
        every { friendsCollection.document("friend123") } returns friendDoc
        val friendSnapshot = mockk<DocumentSnapshot>()
        every { friendDoc.get() } returns Tasks.forResult(friendSnapshot)
        every { friendSnapshot.exists() } returns true
        every { friendSnapshot.getString("inviteStatus") } returns InviteStatus.INVITED.name

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
        val otherUserDoc: DocumentReference = mockk()
        val otherFriendsCollection: CollectionReference = mockk()
        val otherFriendDoc: DocumentReference = mockk()
        
        every { usersCollection.document(friendId) } returns otherUserDoc
        every { otherUserDoc.collection("friends") } returns otherFriendsCollection
        every { otherFriendsCollection.document("me123") } returns otherFriendDoc
        every { friendsCollection.document(friendId) } returns friendDoc
        
        val friendRequestsCollection: CollectionReference = mockk()
        every { db.collection("friendRequests") } returns friendRequestsCollection
        every { friendRequestsCollection.document(any()) } returns mockk(relaxed = true)

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            function.apply(transaction)
            Tasks.forResult(Unit)
        }
        every { transaction.delete(any()) } returns transaction

        val result = repository.removeFriend(friendId)
        
        assertTrue(result.isSuccess)
        verify { transaction.delete(friendDoc) }
        verify { transaction.delete(otherFriendDoc) }
    }
}

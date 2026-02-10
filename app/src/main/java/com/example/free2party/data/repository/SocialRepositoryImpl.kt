package com.example.free2party.data.repository

import android.util.Log
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.exception.CannotAddSelfException
import com.example.free2party.exception.DatabaseOperationException
import com.example.free2party.exception.FriendRequestAlreadyAcceptedException
import com.example.free2party.exception.FriendRequestAlreadySentException
import com.example.free2party.exception.NetworkUnavailableException
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.exception.UserNotFoundException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SocialRepositoryImpl(
    private val db: FirebaseFirestore,
    private val userRepository: UserRepository
) : SocialRepository {

    private val currentUserId: String
        get() = userRepository.currentUserId

    override fun getIncomingFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("friendRequests")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("friendRequestStatus", FriendRequestStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToSocialException(error))
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FriendRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    override fun getFriendsList(): Flow<List<FriendInfo>> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val friendsMap = mutableMapOf<String, FriendInfo>()
        val friendListeners = mutableMapOf<String, ListenerRegistration>()
        val inviteStatuses = mutableMapOf<String, InviteStatus>()

        val collectionListener = db.collection("users").document(currentUserId)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToSocialException(error))
                    return@addSnapshotListener
                }

                val currentFriendIds = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()

                // Cleanup removed friends
                friendsMap.keys.filter { it !in currentFriendIds }.toList().forEach { id ->
                    friendsMap.remove(id)
                    inviteStatuses.remove(id)
                    friendListeners[id]?.remove()
                    friendListeners.remove(id)
                }

                snapshot?.documents?.forEach { doc ->
                    val friendId = doc.id
                    val inviteStatusValue =
                        doc.getString("inviteStatus") ?: InviteStatus.ACCEPTED.name
                    val inviteStatus = try {
                        InviteStatus.valueOf(inviteStatusValue)
                    } catch (e: Exception) {
                        Log.e(
                            "SocialRepository",
                            "Invalid invite status in DB: $inviteStatusValue",
                            e
                        )
                        InviteStatus.ACCEPTED
                    }

                    val oldStatus = inviteStatuses[friendId]
                    inviteStatuses[friendId] = inviteStatus

                    if (!friendListeners.containsKey(friendId)) {
                        val listener = db.collection("users").document(friendId)
                            .addSnapshotListener { friendSnapshot, _ ->
                                if (friendSnapshot != null && friendSnapshot.exists()) {
                                    val info = FriendInfo(
                                        uid = friendId,
                                        name = friendSnapshot.getString("name") ?: "Unknown",
                                        isFreeNow = friendSnapshot.getBoolean("isFreeNow") ?: false,
                                        inviteStatus = inviteStatuses[friendId]
                                            ?: InviteStatus.ACCEPTED
                                    )
                                    friendsMap[friendId] = info
                                    trySend(friendsMap.values.toList().sortedBy { it.name })
                                }
                            }
                        friendListeners[friendId] = listener
                    } else if (oldStatus != inviteStatus) {
                        // Status updated for an existing friend in our map
                        val existing = friendsMap[friendId]
                        if (existing != null) {
                            friendsMap[friendId] = existing.copy(inviteStatus = inviteStatus)
                            trySend(friendsMap.values.toList().sortedBy { it.name })
                        }
                    }
                }

                trySend(friendsMap.values.toList().sortedBy { it.name })
            }

        awaitClose {
            collectionListener.remove()
            friendListeners.values.forEach { it.remove() }
        }
    }

    override suspend fun sendFriendRequest(friendEmail: String): Result<Unit> = try {
        validateSession()

        // Find request receiver using UserRepository
        val receiver = userRepository.getUserByEmail(friendEmail).getOrThrow()

        // Check if adding self
        if (receiver.uid == currentUserId) throw CannotAddSelfException()

        // Check if already invited or added
        val existingFriendDoc = db.collection("users").document(currentUserId)
            .collection("friends").document(receiver.uid).get().await()
        if (existingFriendDoc.exists()) {
            val inviteStatus = existingFriendDoc.getString("inviteStatus")
            if (inviteStatus == InviteStatus.ACCEPTED.name) throw FriendRequestAlreadyAcceptedException()
            else if (inviteStatus == InviteStatus.INVITED.name) throw FriendRequestAlreadySentException()
        }

        // Find current user (yourself) using UserRepository
        val sender = userRepository.getUserById(currentUserId).getOrThrow()
        val requestId = "${currentUserId}_${receiver.uid}"

        db.runTransaction { transaction ->
            // Create Friend Request with sender details
            val requestRef = db.collection("friendRequests").document(requestId)
            transaction.set(
                requestRef, FriendRequest(
                    id = requestId,
                    senderId = currentUserId,
                    senderName = sender.name,
                    senderEmail = sender.email,
                    receiverId = receiver.uid,
                    friendRequestStatus = FriendRequestStatus.PENDING
                )
            )

            // Add to sender's friends list as INVITED
            val senderFriendRef = db.collection("users").document(currentUserId)
                .collection("friends").document(receiver.uid)
            transaction.set(
                senderFriendRef, mapOf(
                    "uid" to receiver.uid,
                    "name" to receiver.name,
                    "inviteStatus" to InviteStatus.INVITED.name,
                    "addedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun cancelFriendRequest(friendId: String): Result<Unit> = try {
        validateSession()

        val requestId = "${currentUserId}_${friendId}"
        db.runTransaction { transaction ->
            // Delete the Friend Request document
            val requestRef = db.collection("friendRequests").document(requestId)
            transaction.delete(requestRef)

            // Remove item from sender's (current user) friends sub-collection
            val senderFriendRef = db.collection("users").document(currentUserId)
                .collection("friends").document(friendId)
            transaction.delete(senderFriendRef)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun updateFriendRequestStatus(
        requestId: String,
        friendRequestStatus: FriendRequestStatus
    ): Result<Unit> = try {
        validateSession()

        db.runTransaction { transaction ->
            val requestRef = db.collection("friendRequests").document(requestId)
            val requestDoc = transaction.get(requestRef)
            val senderId = requestDoc.getString("senderId") ?: return@runTransaction
            val senderName = requestDoc.getString("senderName") ?: "Unknown"

            if (friendRequestStatus == FriendRequestStatus.ACCEPTED) {
                // Update request status to ACCEPTED
                transaction.update(requestRef, "friendRequestStatus", FriendRequestStatus.ACCEPTED)

                // Update sender's record in receiver's list as ACCEPTED
                val receiverFriendRef = db.collection("users").document(currentUserId)
                    .collection("friends").document(senderId)
                transaction.set(
                    receiverFriendRef, mapOf(
                        "uid" to senderId,
                        "name" to senderName,
                        "inviteStatus" to InviteStatus.ACCEPTED.name,
                        "addedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Update receiver's record in sender's list to ACCEPTED
                val senderFriendRef = db.collection("users").document(senderId)
                    .collection("friends").document(currentUserId)
                transaction.update(senderFriendRef, "inviteStatus", InviteStatus.ACCEPTED.name)

            } else { // DECLINED
                // Delete the request
                transaction.delete(requestRef)

                // Remove from sender's list
                val senderFriendRef = db.collection("users").document(senderId)
                    .collection("friends").document(currentUserId)
                transaction.delete(senderFriendRef)
            }
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> = try {
        validateSession()
        db.runTransaction { transaction ->
            // Remove friend from current user's friends list
            val myFriendRef = db.collection("users").document(currentUserId)
                .collection("friends").document(friendId)
            transaction.delete(myFriendRef)

            // Remove current user from friend's friends list
            val theirFriendRef = db.collection("users").document(friendId)
                .collection("friends").document(currentUserId)
            transaction.delete(theirFriendRef)

            // Also delete any friend request between them (cleanup)
            // It could be sender_receiver or receiver_sender
            transaction.delete(
                db.collection("friendRequests").document("${currentUserId}_${friendId}")
            )
            transaction.delete(
                db.collection("friendRequests").document("${friendId}_${currentUserId}")
            )
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    private fun validateSession() {
        if (currentUserId.isBlank()) throw UnauthorizedException()
    }

    private fun mapToSocialException(e: Exception): Exception {
        return when (e) {
            is FirebaseNetworkException -> NetworkUnavailableException()
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> UnauthorizedException("You don't have permission for this social action")
                    FirebaseFirestoreException.Code.UNAVAILABLE -> NetworkUnavailableException()
                    else -> DatabaseOperationException(
                        e.localizedMessage ?: "Social database error"
                    )
                }
            }

            is CannotAddSelfException,
            is FriendRequestAlreadyAcceptedException,
            is FriendRequestAlreadySentException,
            is UserNotFoundException,
            is UnauthorizedException -> e

            else -> DatabaseOperationException(e.localizedMessage ?: "Unknown social error")
        }
    }
}

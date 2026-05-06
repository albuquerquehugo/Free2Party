package com.example.free2party.data.repository

import android.util.Log
import com.example.free2party.R
import com.example.free2party.data.model.BlockedUser
import com.example.free2party.data.model.Circle
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.data.model.UserRelationship
import com.example.free2party.data.model.Notification
import com.example.free2party.data.model.UserSearchResult
import com.example.free2party.data.model.NotificationType
import com.example.free2party.exception.CannotAddSelfException
import com.example.free2party.exception.DatabaseOperationException
import com.example.free2party.exception.FriendRequestAlreadyAcceptedException
import com.example.free2party.exception.FriendRequestAlreadySentException
import com.example.free2party.exception.FriendRequestPendingException
import com.example.free2party.exception.FriendRequestBlockedException
import com.example.free2party.exception.FriendRequestNotFoundException
import com.example.free2party.exception.InfrastructureException
import com.example.free2party.exception.NetworkUnavailableException
import com.example.free2party.exception.SocialException
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.util.removeAccents
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val userRepository: UserRepository,
    @param:ApplicationContext private val context: android.content.Context
) : SocialRepository {

    private val currentUserId: String
        get() = userRepository.currentUserId

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getIncomingFriendRequests(): Flow<List<FriendRequest>> {
        if (currentUserId.isBlank()) return flowOf(emptyList())

        val blockedUsersFlow = callbackFlow {
            val listener = db.collection("users").document(currentUserId)
                .collection("blocked")
                .addSnapshotListener { snapshot, _ ->
                    val blockedIds = snapshot?.documents?.map { it.id } ?: emptyList()
                    trySend(blockedIds)
                }
            awaitClose { listener.remove() }
        }

        val requestsFlow = callbackFlow {
            val listener = db.collection("friendRequests")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("friendRequestStatus", FriendRequestStatus.PENDING.name)
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

        return combine(requestsFlow, blockedUsersFlow) { requests, blockedIds ->
            requests.filter { it.senderId !in blockedIds }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFriendsList(): Flow<List<FriendInfo>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener = db.collection("users").document(currentUserId)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToSocialException(error))
                    return@addSnapshotListener
                }

                val friendStubs = snapshot?.toObjects(FriendInfo::class.java) ?: emptyList()
                trySend(friendStubs)
            }

        awaitClose { listener.remove() }
    }.flatMapLatest { friendStubs ->
        if (friendStubs.isEmpty()) return@flatMapLatest flowOf(emptyList())

        // For each friend, observe their actual user document to get real-time status
        val friendFlows = friendStubs.map { stub ->
            userRepository.observeUser(stub.uid).map { user ->
                stub.copy(
                    name = user.fullName,
                    email = user.email,
                    isFreeNow = user.isFreeNow,
                    isStatusFromPlan = user.isStatusFromPlan,
                    socials = user.socials,
                    phoneNumber = user.phoneNumber,
                    profilePicUrl = user.profilePicUrl
                )
            }.catch { emit(stub) } // Fallback to stub if user doc can't be read
        }

        combine(friendFlows) { it.toList() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNotifications(): Flow<List<Notification>> {
        if (currentUserId.isBlank()) return flowOf(emptyList())

        return db.collection("users").document(currentUserId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListenerFlow()
            .catch { emit(emptyList()) }
    }

    private fun Query.addSnapshotListenerFlow(): Flow<List<Notification>> = callbackFlow {
        val listener = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapToSocialException(error))
                return@addSnapshotListener
            }
            val notifications = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Notification::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(notifications)
        }
        awaitClose { listener.remove() }
    }

    override fun getOutgoingFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener = db.collection("friendRequests")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("friendRequestStatus", FriendRequestStatus.PENDING.name)
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

    override fun getBlockedUsers(): Flow<List<BlockedUser>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener = db.collection("users").document(currentUserId)
            .collection("blocked")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToSocialException(error))
                    return@addSnapshotListener
                }

                val blockedStubs = snapshot?.toObjects(BlockedUser::class.java) ?: emptyList()
                trySend(blockedStubs)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun searchUsers(query: String): Result<List<UserSearchResult>> = try {
        validateSession()
        if (query.isBlank()) {
            Result.success(emptyList())
        } else {
            val normalizedQuery = query.trim().lowercase().removeAccents()
            Log.d("SocialRepository", "Searching users with normalized query: $normalizedQuery")

            val querySnapshot = db.collection("users")
                .whereArrayContains("searchKeywords", normalizedQuery)
                .limit(20)
                .get().await()

            val friendsSnapshot = db.collection("users").document(currentUserId)
                .collection("friends").get().await()
            val friendsMap = friendsSnapshot.documents.associateBy(
                { it.id },
                { it.getString("inviteStatus") ?: InviteStatus.ACCEPTED.name }
            )

            val blockedSnapshot = db.collection("users").document(currentUserId)
                .collection("blocked").get().await()
            val blockedIds = blockedSnapshot.documents.map { it.id }.toSet()

            val results = querySnapshot.documents
                .mapNotNull { doc ->
                    val uid = doc.id
                    val relationship = when {
                        uid in blockedIds -> UserRelationship.BLOCKED
                        friendsMap.containsKey(uid) -> {
                            if (friendsMap[uid] == InviteStatus.INVITED.name) UserRelationship.INVITED
                            else UserRelationship.FRIEND
                        }
                        else -> UserRelationship.NONE
                    }

                    doc.toObject(UserSearchResult::class.java)?.copy(
                        uid = uid,
                        profilePicUrl = doc.getString("profilePicUrl") ?: "",
                        relationship = relationship
                    )
                }
                .filter { it.uid != currentUserId }
                .take(10)

            Log.d("SocialRepository", "Total results found: ${results.size}")
            Result.success(results)
        }
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Log.e("SocialRepository", "Error searching users", e)
        Result.failure(mapToSocialException(e))
    }

    override suspend fun sendFriendRequest(friendEmail: String): Result<Unit> = try {
        validateSession()
        val receiverResult = userRepository.getUserByEmail(friendEmail)
        if (receiverResult.isFailure) return Result.failure(receiverResult.exceptionOrNull()!!)
        val receiver = receiverResult.getOrThrow()
        if (receiver.uid == currentUserId) throw CannotAddSelfException()

        val senderResult = userRepository.getUserById(currentUserId)
        if (senderResult.isFailure) return Result.failure(senderResult.exceptionOrNull()!!)
        val sender = senderResult.getOrThrow()
        val requestId = "${currentUserId}_${receiver.uid}"

        db.runTransaction { transaction ->
            // Check if receiver has blocked current user
            val blockedByReceiverRef = db.collection("users").document(receiver.uid)
                .collection("blocked").document(currentUserId)
            val blockedByReceiverDoc = transaction.get(blockedByReceiverRef)

            // If current user has blocked receiver, unblock them first
            val blockedByCurrentUserRef = db.collection("users").document(currentUserId)
                .collection("blocked").document(receiver.uid)
            val blockedByCurrentUserDoc = transaction.get(blockedByCurrentUserRef)

            val existingFriendRef = db.collection("users").document(currentUserId)
                .collection("friends").document(receiver.uid)
            val existingFriendDoc = transaction.get(existingFriendRef)

            // Check if there's a pending incoming request from this user
            val incomingRequestRef = db.collection("friendRequests").document("${receiver.uid}_$currentUserId")
            val incomingRequestDoc = transaction.get(incomingRequestRef)

            // Check if receiver has blocked current user
            if (blockedByReceiverDoc.exists()) {
                throw FriendRequestBlockedException()
            }

            // If current user has blocked receiver, unblock them first
            if (blockedByCurrentUserDoc.exists()) {
                transaction.delete(blockedByCurrentUserRef)
            }

            if (incomingRequestDoc.exists()) {
                throw FriendRequestPendingException()
            }

            if (existingFriendDoc.exists()) {
                val inviteStatus = existingFriendDoc.getString("inviteStatus")
                if (inviteStatus == InviteStatus.ACCEPTED.name) throw FriendRequestAlreadyAcceptedException()
                else if (inviteStatus == InviteStatus.INVITED.name) throw FriendRequestAlreadySentException()
            }

            val requestRef = db.collection("friendRequests").document(requestId)
            transaction.set(
                requestRef, mapOf(
                    "id" to requestId,
                    "senderId" to currentUserId,
                    "senderName" to sender.fullName,
                    "senderEmail" to sender.email,
                    "senderProfilePicUrl" to sender.profilePicUrl,
                    "receiverId" to receiver.uid,
                    "receiverName" to receiver.fullName,
                    "receiverProfilePicUrl" to receiver.profilePicUrl,
                    "friendRequestStatus" to FriendRequestStatus.PENDING.name,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            )

            val senderFriendRef = db.collection("users").document(currentUserId)
                .collection("friends").document(receiver.uid)
            transaction.set(
                senderFriendRef, mapOf(
                    "uid" to receiver.uid,
                    "name" to receiver.fullName,
                    "email" to receiver.email,
                    "profilePicUrl" to receiver.profilePicUrl,
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
        val requestId = "${currentUserId}_$friendId"
        db.runTransaction { transaction ->
            transaction.delete(db.collection("friendRequests").document(requestId))
            transaction.delete(
                db.collection("users").document(currentUserId).collection("friends")
                    .document(friendId)
            )
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
            if (!requestDoc.exists()) {
                Log.e("SocialRepositoryImpl", "Friend request doc not found: $requestId")
                throw FriendRequestNotFoundException()
            }
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: throw FriendRequestNotFoundException()

            val senderFriendRef = db.collection("users").document(request.senderId)
                .collection("friends").document(request.receiverId)

            val receiverRef = db.collection("users").document(request.receiverId)
            val receiverDoc = transaction.get(receiverRef)
            val receiverFirstName = receiverDoc.getString("firstName") ?: ""
            val receiverLastName = receiverDoc.getString("lastName") ?: ""
            val receiverName = "$receiverFirstName $receiverLastName".trim().ifBlank { "Someone" }
            val receiverEmail = receiverDoc.getString("email") ?: ""

            if (friendRequestStatus == FriendRequestStatus.ACCEPTED) {
                // Update sender's friend document to ACCEPTED
                transaction.update(senderFriendRef, "inviteStatus", InviteStatus.ACCEPTED.name)

                // Create a friend document for the receiver (current user)
                val receiverFriendRef = db.collection("users").document(request.receiverId)
                    .collection("friends").document(request.senderId)
                transaction.set(
                    receiverFriendRef, mapOf(
                        "uid" to request.senderId,
                        "name" to request.senderName,
                        "email" to request.senderEmail,
                        "profilePicUrl" to request.senderProfilePicUrl,
                        "inviteStatus" to InviteStatus.ACCEPTED.name,
                        "addedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Delete the friend request document
                transaction.delete(requestRef)

                // Create notification for sender
                val senderNotifRef = db.collection("users").document(request.senderId)
                    .collection("notifications").document()
                transaction.set(
                    senderNotifRef, mapOf(
                        "title" to context.getString(R.string.title_friend_request_accepted),
                        "message" to context.getString(
                            R.string.notification_friend_request_accepted_body,
                            receiverName,
                            receiverEmail
                        ),
                        "isRead" to false,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "type" to NotificationType.FRIEND_ADDED.name
                    )
                )
            } else {
                // If declined, remove the sender's invited status and the request
                transaction.delete(requestRef)
                transaction.delete(senderFriendRef)

                // Create notification for sender
                val senderNotifRef = db.collection("users").document(request.senderId)
                    .collection("notifications").document()
                transaction.set(
                    senderNotifRef, mapOf(
                        "title" to context.getString(R.string.title_friend_request_declined),
                        "message" to context.getString(
                            R.string.notification_friend_request_declined_body,
                            receiverName,
                            receiverEmail
                        ),
                        "isRead" to false,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "type" to NotificationType.FRIEND_DECLINED.name,
                        "isSilent" to true
                    )
                )
            }
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun declineAndBlockFriendRequest(requestId: String): Result<Unit> = try {
        validateSession()
        db.runTransaction { transaction ->
            val requestRef = db.collection("friendRequests").document(requestId)
            val requestDoc = transaction.get(requestRef)
            if (!requestDoc.exists()) throw FriendRequestNotFoundException()

            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: throw FriendRequestNotFoundException()

            val receiverRef = db.collection("users").document(request.receiverId)
            val receiverDoc = transaction.get(receiverRef)

            // Delete the friend request
            transaction.delete(requestRef)

            // Remove the sender's "invited" status for the current user
            val senderFriendRef = db.collection("users").document(request.senderId)
                .collection("friends").document(request.receiverId)
            transaction.delete(senderFriendRef)

            // Add sender to the receiver's (current user) blocked list
            val blockedRef = db.collection("users").document(request.receiverId)
                .collection("blocked").document(request.senderId)
            transaction.set(
                blockedRef, mapOf(
                    "uid" to request.senderId,
                    "name" to request.senderName,
                    "blockedAt" to FieldValue.serverTimestamp()
                )
            )

            // Create notification for sender
            val receiverFirstName = receiverDoc.getString("firstName") ?: ""
            val receiverLastName = receiverDoc.getString("lastName") ?: ""
            val receiverName = "$receiverFirstName $receiverLastName".trim().ifBlank { "Someone" }
            val receiverEmail = receiverDoc.getString("email") ?: ""

            val senderNotifRef = db.collection("users").document(request.senderId)
                .collection("notifications").document()
            transaction.set(
                senderNotifRef, mapOf(
                    "title" to context.getString(R.string.title_friend_request_declined),
                    "message" to context.getString(
                        R.string.notification_friend_request_declined_body,
                        receiverName,
                        receiverEmail
                    ),
                    "isRead" to false,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "type" to NotificationType.FRIEND_DECLINED.name,
                    "isSilent" to true
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun unblockUser(userId: String): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .collection("blocked").document(userId)
            .delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> = try {
        validateSession()

        val sender = userRepository.getUserById(currentUserId).getOrThrow()

        db.runTransaction { transaction ->
            transaction.delete(
                db.collection("users").document(currentUserId).collection("friends")
                    .document(friendId)
            )
            transaction.delete(
                db.collection("users").document(friendId).collection("friends")
                    .document(currentUserId)
            )
            transaction.delete(
                db.collection("friendRequests").document("${currentUserId}_${friendId}")
            )
            transaction.delete(
                db.collection("friendRequests").document("${friendId}_${currentUserId}")
            )

            val receiverNotifRef = db.collection("users").document(friendId)
                .collection("notifications").document()
            transaction.set(
                receiverNotifRef, mapOf(
                    "title" to context.getString(R.string.title_friend_removed),
                    "message" to context.getString(
                        R.string.notification_friend_removed_body,
                        sender.fullName,
                        sender.email
                    ),
                    "isRead" to false,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "type" to NotificationType.FRIEND_REMOVED.name,
                    "isSilent" to true
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun removeAndBlockFriend(friendId: String): Result<Unit> = try {
        validateSession()

        val friendDoc = db.collection("users").document(currentUserId)
            .collection("friends").document(friendId).get().await()
        val friendName = friendDoc.getString("name") ?: "Someone"

        db.runTransaction { transaction ->
            transaction.delete(
                db.collection("users").document(currentUserId).collection("friends")
                    .document(friendId)
            )
            transaction.delete(
                db.collection("users").document(friendId).collection("friends")
                    .document(currentUserId)
            )
            transaction.delete(
                db.collection("friendRequests").document("${currentUserId}_${friendId}")
            )
            transaction.delete(
                db.collection("friendRequests").document("${friendId}_${currentUserId}")
            )
            val blockedRef = db.collection("users").document(currentUserId)
                .collection("blocked").document(friendId)
            transaction.set(
                blockedRef, mapOf(
                    "uid" to friendId,
                    "name" to friendName,
                    "blockedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .collection("notifications").document(notificationId)
            .update("isRead", true).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun markNotificationAsUnread(notificationId: String): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .collection("notifications").document(notificationId)
            .update("isRead", false).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun markNotificationsAsRead(notificationIds: List<String>): Result<Unit> =
        try {
            validateSession()
            if (notificationIds.isEmpty()) Result.success(Unit)
            else {
                val batch = db.batch()
                notificationIds.forEach { id ->
                    val ref = db.collection("users").document(currentUserId)
                        .collection("notifications").document(id)
                    batch.update(ref, "isRead", true)
                }
                batch.commit().await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(mapToSocialException(e))
        }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .collection("notifications").document(notificationId)
            .delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override fun getCircles(): Flow<List<Circle>> {
        if (currentUserId.isBlank()) return flowOf(emptyList())

        return db.collection("users").document(currentUserId)
            .collection("circles")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListenerFlowCircles()
    }

    private fun Query.addSnapshotListenerFlowCircles(): Flow<List<Circle>> = callbackFlow {
        val listener = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapToSocialException(error))
                return@addSnapshotListener
            }
            val circlesList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Circle::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(circlesList)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun createCircle(name: String, friendIds: List<String>): Result<String> = try {
        validateSession()
        val circleRef = db.collection("users").document(currentUserId)
            .collection("circles").document()
        
        val circle = Circle(
            id = circleRef.id,
            name = name,
            ownerId = currentUserId,
            friendIds = friendIds
        )
        
        circleRef.set(circle).await()
        Result.success(circleRef.id)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun updateCircle(circleId: String, name: String, friendIds: List<String>): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .collection("circles").document(circleId)
            .update(
                mapOf(
                    "name" to name,
                    "friendIds" to friendIds
                )
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun deleteCircle(circleId: String): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .collection("circles").document(circleId)
            .delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    private fun validateSession() {
        if (currentUserId.isBlank()) throw UnauthorizedException()
    }

    private fun mapToSocialException(e: Throwable): Exception {
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

            is InfrastructureException -> e
            is SocialException -> e
            else -> DatabaseOperationException(e.localizedMessage ?: "Unknown social error")
        }
    }
}

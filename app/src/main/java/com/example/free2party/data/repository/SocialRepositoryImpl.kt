package com.example.free2party.data.repository

import android.util.Log
import com.example.free2party.BuildConfig
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.exception.CannotAddSelfException
import com.example.free2party.exception.DatabaseOperationException
import com.example.free2party.exception.FriendRequestAlreadyAcceptedException
import com.example.free2party.exception.FriendRequestAlreadySentException
import com.example.free2party.exception.NetworkUnavailableException
import com.example.free2party.exception.SocialException
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.util.isPlanActive
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class SocialRepositoryImpl(
    private val db: FirebaseFirestore,
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFriendsList(): Flow<List<FriendInfo>> {
        if (currentUserId.isBlank()) return flowOf(emptyList())

        val tickerFlow = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(BuildConfig.updateFrequency)
            }
        }

        return callbackFlow {
            val collectionListener = db.collection("users").document(currentUserId)
                .collection("friends")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(mapToSocialException(error))
                        return@addSnapshotListener
                    }
                    val friendIds = snapshot?.documents?.map { it.id } ?: emptyList()
                    val inviteStatuses = snapshot?.documents?.associate {
                        it.id to (it.getString("inviteStatus")?.let { status ->
                            try {
                                InviteStatus.valueOf(status)
                            } catch (e: Exception) {
                                Log.e("SocialRepositoryImpl", "Error parsing invite status", e)
                                InviteStatus.ACCEPTED
                            }
                        } ?: InviteStatus.ACCEPTED)
                    } ?: emptyMap()

                    trySend(friendIds to inviteStatuses)
                }
            awaitClose { collectionListener.remove() }
        }.flatMapLatest { (friendIds, inviteStatuses) ->
            if (friendIds.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val friendFlows = friendIds.map { friendId ->
                combine(
                    userRepository.observeUser(friendId),
                    planRepository.getPublicPlans(friendId),
                    planRepository.getAllPlans(friendId),
                    tickerFlow
                ) { user, publicPlans, allPlans, currentTime ->
                    val hasActiveSharedPlan = publicPlans.any { isPlanActive(it, currentTime) }
                    val hasAnyActivePlan = allPlans.any { isPlanActive(it, currentTime) }

                    FriendInfo(
                        uid = friendId,
                        name = user.fullName.ifBlank { "Unknown" },
                        isFreeNow = hasActiveSharedPlan || (user.isFreeNow && !hasAnyActivePlan),
                        inviteStatus = inviteStatuses[friendId] ?: InviteStatus.ACCEPTED
                    )
                }
            }
            combine(friendFlows) { it.toList().sortedBy { friend -> friend.name } }
        }
    }

    override suspend fun sendFriendRequest(friendEmail: String): Result<Unit> = try {
        validateSession()
        val receiver = userRepository.getUserByEmail(friendEmail).getOrThrow()
        if (receiver.uid == currentUserId) throw CannotAddSelfException()

        val existingFriendDoc = db.collection("users").document(currentUserId)
            .collection("friends").document(receiver.uid).get().await()
        if (existingFriendDoc.exists()) {
            val inviteStatus = existingFriendDoc.getString("inviteStatus")
            if (inviteStatus == InviteStatus.ACCEPTED.name) throw FriendRequestAlreadyAcceptedException()
            else if (inviteStatus == InviteStatus.INVITED.name) throw FriendRequestAlreadySentException()
        }

        val sender = userRepository.getUserById(currentUserId).getOrThrow()
        val requestId = "${currentUserId}_${receiver.uid}"

        db.runTransaction { transaction ->
            val requestRef = db.collection("friendRequests").document(requestId)
            transaction.set(
                requestRef, FriendRequest(
                    id = requestId,
                    senderId = currentUserId,
                    senderName = sender.fullName,
                    senderEmail = sender.email,
                    receiverId = receiver.uid,
                    friendRequestStatus = FriendRequestStatus.PENDING
                )
            )

            val senderFriendRef = db.collection("users").document(currentUserId)
                .collection("friends").document(receiver.uid)
            transaction.set(
                senderFriendRef, mapOf(
                    "uid" to receiver.uid,
                    "name" to receiver.fullName,
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
            val senderId = requestDoc.getString("senderId") ?: return@runTransaction
            val senderName = requestDoc.getString("senderName") ?: "Unknown"

            if (friendRequestStatus == FriendRequestStatus.ACCEPTED) {
                transaction.update(requestRef, "friendRequestStatus", FriendRequestStatus.ACCEPTED)
                transaction.set(
                    db.collection("users").document(currentUserId).collection("friends")
                        .document(senderId),
                    mapOf(
                        "uid" to senderId,
                        "name" to senderName,
                        "inviteStatus" to InviteStatus.ACCEPTED.name,
                        "addedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    db.collection("users").document(senderId).collection("friends")
                        .document(currentUserId), "inviteStatus", InviteStatus.ACCEPTED.name
                )
            } else {
                transaction.delete(requestRef)
                transaction.delete(
                    db.collection("users").document(senderId).collection("friends")
                        .document(currentUserId)
                )
            }
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToSocialException(e))
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> = try {
        validateSession()
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

            is SocialException -> e
            else -> DatabaseOperationException(e.localizedMessage ?: "Unknown social error")
        }
    }
}

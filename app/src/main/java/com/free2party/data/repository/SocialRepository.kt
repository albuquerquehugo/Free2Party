package com.free2party.data.repository

import com.free2party.data.model.BlockedUser
import com.free2party.data.model.Circle
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.FriendRequest
import com.free2party.data.model.FriendRequestStatus
import com.free2party.data.model.Notification
import com.free2party.data.model.UserSearchResult
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getIncomingFriendRequests(): Flow<List<FriendRequest>>
    fun getFriendsList(): Flow<List<FriendInfo>>
    fun getNotifications(): Flow<List<Notification>>
    fun getOutgoingFriendRequests(): Flow<List<FriendRequest>>
    fun getBlockedUsers(): Flow<List<BlockedUser>>

    suspend fun searchUsers(query: String): Result<List<UserSearchResult>>
    suspend fun sendFriendRequest(friendEmail: String): Result<Unit>
    suspend fun cancelFriendRequest(friendId: String): Result<Unit>
    suspend fun updateFriendRequestStatus(
        requestId: String,
        friendRequestStatus: FriendRequestStatus
    ): Result<Unit>

    suspend fun declineAndBlockFriendRequest(requestId: String): Result<Unit>
    suspend fun unblockUser(userId: String): Result<Unit>

    suspend fun reportUser(userId: String, reason: String): Result<Unit>

    suspend fun removeFriend(friendId: String): Result<Unit>
    suspend fun removeAndBlockFriend(friendId: String): Result<Unit>

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    suspend fun markNotificationAsUnread(notificationId: String): Result<Unit>
    suspend fun markNotificationsAsRead(notificationIds: List<String>): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    suspend fun updateUserSocialContext(
        newFullName: String? = null,
        newProfilePicUrl: String? = null
    ): Result<Unit>

    fun getCircles(): Flow<List<Circle>>
    suspend fun createCircle(name: String, friendIds: List<String>): Result<String>
    suspend fun updateCircle(circleId: String, name: String, friendIds: List<String>): Result<Unit>
    suspend fun deleteCircle(circleId: String): Result<Unit>
}

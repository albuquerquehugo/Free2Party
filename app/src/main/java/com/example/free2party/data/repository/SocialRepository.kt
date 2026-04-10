package com.example.free2party.data.repository

import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.model.Notification
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getIncomingFriendRequests(): Flow<List<FriendRequest>>
    fun getFriendsList(): Flow<List<FriendInfo>>
    fun getNotifications(): Flow<List<Notification>>
    fun getOutgoingFriendRequests(): Flow<List<FriendRequest>>

    suspend fun sendFriendRequest(friendEmail: String): Result<Unit>
    suspend fun cancelFriendRequest(friendId: String): Result<Unit>
    suspend fun updateFriendRequestStatus(
        requestId: String,
        friendRequestStatus: FriendRequestStatus
    ): Result<Unit>

    suspend fun declineAndBlockFriendRequest(requestId: String): Result<Unit>

    suspend fun removeFriend(friendId: String): Result<Unit>

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    suspend fun markNotificationAsUnread(notificationId: String): Result<Unit>
    suspend fun markNotificationsAsRead(notificationIds: List<String>): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
}

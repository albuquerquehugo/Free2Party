package com.example.free2party.data.model

import com.google.firebase.Timestamp

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val receiverId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val friendRequestStatus: FriendRequestStatus = FriendRequestStatus.PENDING
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

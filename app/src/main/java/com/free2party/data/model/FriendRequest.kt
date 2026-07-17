package com.free2party.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val senderProfilePicUrl: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverProfilePicUrl: String = "",
    @get:PropertyName("isViewed")
    @set:PropertyName("isViewed")
    var isViewed: Boolean = false,
    @ServerTimestamp
    val timestamp: Date? = null,
    val friendRequestStatus: FriendRequestStatus = FriendRequestStatus.PENDING
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

package com.example.free2party.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @ServerTimestamp
    val timestamp: Date? = null,
    val type: NotificationType = NotificationType.GENERAL,
    @get:PropertyName("isSilent")
    @set:PropertyName("isSilent")
    var isSilent: Boolean = false
)

enum class NotificationType {
    GENERAL,
    FRIEND_ADDED,
    FRIEND_DECLINED,
    FRIEND_REMOVED,
    FRIEND_REQUEST_RECEIVED
}

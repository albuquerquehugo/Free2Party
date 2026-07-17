package com.free2party.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notification(
    val id: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val title: String = "",
    val eventId: String = "",
    val message: String = "",
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @get:PropertyName("isSilent")
    @set:PropertyName("isSilent")
    var isSilent: Boolean = false,
    @get:PropertyName("isViewed")
    @set:PropertyName("isViewed")
    var isViewed: Boolean = false,
    @ServerTimestamp
    val timestamp: Date? = null
)

enum class NotificationType {
    // Social / Friends
    FRIEND_REQUEST_RECEIVED,
    FRIEND_ACCEPTED,
    FRIEND_DECLINED,
    FRIEND_REMOVED,

    // Events
    EVENT_INVITE,
    EVENT_COMMENT,

    // System & Misc
    GENERAL,
}

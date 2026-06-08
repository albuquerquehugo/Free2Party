package com.free2party.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class EventType {
    PUBLIC,
    PRIVATE
}

enum class GuestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

data class EventLink(
    val title: String = "",
    val url: String = ""
)

@IgnoreExtraProperties
data class Event(
    val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostProfilePic: String = "",
    val hostEmail: String = "",
    val type: EventType = EventType.PRIVATE,
    val title: String = "",
    val description: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val timezone: String = "",
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Stored as Map<String, String> for guestId -> GuestStatus.name to avoid Firestore serialization issues
    val guests: Map<String, String> = emptyMap(),
    val guestIds: List<String> = emptyList(),
    val usefulLinks: List<EventLink> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null
)

@IgnoreExtraProperties
data class EventComment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePic: String = "",
    val text: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)

@IgnoreExtraProperties
data class EventPhoto(
    val id: String = "",
    val uploadedBy: String = "",
    val url: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)

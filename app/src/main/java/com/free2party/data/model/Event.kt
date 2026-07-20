package com.free2party.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    val guests: Map<String, String> = emptyMap(),
    val guestIds: List<String> = emptyList(),
    val invitedGuestIds: List<String>? = null,
    val usefulLinks: List<EventLink> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null
)

fun Event.isEnded(): Boolean {
    val tz = if (timezone.isNotBlank()) {
        runCatching { TimeZone.getTimeZone(timezone) }.getOrDefault(TimeZone.getDefault())
    } else {
        TimeZone.getDefault()
    }

    val targetDateStr = endDate.ifBlank { startDate }
    if (targetDateStr.isBlank()) return false

    val endMillis: Long? = if (endTime.isNotBlank()) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = tz
        }
        runCatching { sdf.parse("$targetDateStr $endTime")?.time }.getOrNull()
    } else {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = tz
        }
        runCatching {
            sdf.parse(targetDateStr)?.let { date ->
                val calendar = Calendar.getInstance(tz).apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                calendar.timeInMillis
            }
        }.getOrNull()
    }

    return endMillis != null && System.currentTimeMillis() > endMillis
}

@IgnoreExtraProperties
data class EventComment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePic: String = "",
    val text: String = "",
    val edited: Boolean = false,
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

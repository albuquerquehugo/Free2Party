package com.example.free2party.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a group of friends for filtering availability and privacy settings.
 */
@IgnoreExtraProperties
data class Circle(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val friendIds: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null
)

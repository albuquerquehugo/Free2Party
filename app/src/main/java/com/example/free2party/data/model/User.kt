package com.example.free2party.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserSettings(
    val use24HourFormat: Boolean = true
)

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val bio: String = "",
    val profilePicUrl: String = "",
    @get:PropertyName("isFreeNow")
    val isFreeNow: Boolean = false,
    val settings: UserSettings = UserSettings(),
    @ServerTimestamp
    val createdAt: Date? = null
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

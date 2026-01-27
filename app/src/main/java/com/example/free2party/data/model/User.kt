package com.example.free2party.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profilePicUrl: String = "",
    // We store the "Current Status" inside the user object for fast friend-list lookups
    val isFreeNow: Boolean = false,
    val currentStatusNote: String = "", // e.g., "Down for tacos!"
    @ServerTimestamp val lastStatusUpdate: Date? = null
)

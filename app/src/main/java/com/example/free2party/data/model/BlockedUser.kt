package com.example.free2party.data.model

import com.google.firebase.Timestamp

data class BlockedUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String = "",
    val blockedAt: Timestamp? = null
)

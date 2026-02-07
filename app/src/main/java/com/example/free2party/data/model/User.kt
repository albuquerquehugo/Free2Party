package com.example.free2party.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String = "",
    @get:PropertyName("isFreeNow")
    val isFreeNow: Boolean = false
)

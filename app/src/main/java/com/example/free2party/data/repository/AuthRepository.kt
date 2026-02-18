package com.example.free2party.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    val currentUser: FirebaseUser?
    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        profilePicUri: Uri? = null
    ): Result<FirebaseUser>

    suspend fun login(email: String, password: String): Result<FirebaseUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    fun logout()
}

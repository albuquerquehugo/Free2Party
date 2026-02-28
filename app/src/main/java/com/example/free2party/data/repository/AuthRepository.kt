package com.example.free2party.data.repository

import android.net.Uri
import com.example.free2party.data.model.UserSocials
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    val currentUser: FirebaseUser?
    suspend fun register(
        profilePicUri: Uri? = null,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        countryCode: String = "",
        phoneNumber: String = "",
        birthday: String = "",
        bio: String = "",
        socials: UserSocials = UserSocials()
    ): Result<FirebaseUser>

    suspend fun login(email: String, password: String): Result<FirebaseUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    fun logout()
}

package com.example.free2party.data.repository

import android.net.Uri
import android.util.Log
import com.example.free2party.data.model.Gender
import com.example.free2party.data.model.User
import com.example.free2party.data.model.UserSocials
import com.example.free2party.exception.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {
    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun register(
        profilePicUri: Uri?,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        countryCode: String,
        phoneNumber: String,
        birthday: String,
        bio: String,
        gender: Gender,
        socials: UserSocials
    ): Result<FirebaseUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw UserNullException()

        firebaseUser.sendEmailVerification().await()

        var profilePicUrl = ""
        if (profilePicUri != null) {
            profilePicUrl = userRepository.uploadProfilePicture(profilePicUri).getOrDefault("")
        }

        val newUser = User(
            uid = firebaseUser.uid,
            profilePicUrl = profilePicUrl,
            firstName = firstName,
            lastName = lastName,
            email = email,
            countryCode = countryCode,
            phoneNumber = phoneNumber,
            birthday = birthday,
            gender = gender,
            socials = socials,
            bio = bio,
            isFreeNow = false
            // createdAt is handled by @ServerTimestamp in User model
        )

        userRepository.createUserProfile(newUser).getOrThrow()

        // Sign out the user immediately after registration so they have to log in and verify
        auth.signOut()

        Result.success(firebaseUser)
    } catch (e: Exception) {
        Log.e("AuthRepository", "Registration failed", e)
        Result.failure(mapToAuthException(e))
    }

    override suspend fun login(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw UserNullException()
        if (!user.isEmailVerified) {
            auth.signOut()
            throw EmailNotVerifiedException()
        }
        Result.success(user)
    } catch (e: Exception) {
        Log.e("AuthRepository", "Login failed", e)
        Result.failure(mapToAuthException(e))
    }

    override suspend fun signInWithGoogle(credential: AuthCredential): Result<FirebaseUser> = try {
        val result = auth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: throw UserNullException()

        // Check if user profile already exists
        val userProfileResult = userRepository.getUserById(firebaseUser.uid)
        if (userProfileResult.isFailure) {
            // If user profile doesn't exist, create a basic one from Google data
            val newUser = User(
                uid = firebaseUser.uid,
                profilePicUrl = firebaseUser.photoUrl?.toString() ?: "",
                firstName = firebaseUser.displayName?.split(" ")?.getOrNull(0) ?: "",
                lastName = firebaseUser.displayName?.split(" ")?.getOrNull(1) ?: "",
                email = firebaseUser.email ?: "",
                isFreeNow = false
            )
            userRepository.createUserProfile(newUser).getOrThrow()
        }

        Result.success(firebaseUser)
    } catch (e: Exception) {
        Log.e("AuthRepository", "Google Sign-In failed", e)
        Result.failure(mapToAuthException(e))
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("AuthRepository", "Password reset failed", e)
        Result.failure(mapToAuthException(e))
    }

    override suspend fun resendVerificationEmail(email: String, password: String): Result<Unit> =
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw UserNullException()
            if (!user.isEmailVerified) {
                user.sendEmailVerification().await()
                auth.signOut()
                Result.success(Unit)
            } else {
                auth.signOut()
                Result.failure(Exception("Email already verified"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Resend verification failed", e)
            Result.failure(mapToAuthException(e))
        }

    override fun logout() {
        auth.signOut()
    }

    private fun mapToAuthException(e: Exception): Exception {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> WeakPasswordException()
            is FirebaseAuthUserCollisionException -> EmailAlreadyInUseException()
            is FirebaseAuthInvalidCredentialsException -> InvalidCredentialsException()
            is FirebaseAuthInvalidUserException -> {
                when (e.errorCode) {
                    "ERROR_USER_DISABLED" -> UserDisabledException()
                    "ERROR_USER_NOT_FOUND" -> InvalidCredentialsException()
                    else -> UnknownAuthException(e.localizedMessage ?: "User error")
                }
            }

            is FirebaseNetworkException -> AuthNetworkException()
            is AuthException -> e
            else -> UnknownAuthException(e.localizedMessage ?: "Unknown error")
        }
    }
}

package com.free2party.data.repository

import android.net.Uri
import android.util.Log
import com.free2party.data.model.Gender
import com.free2party.data.model.User
import com.free2party.data.model.UserSocials
import com.free2party.exception.*
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
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

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
            isFreeNow = false,
            registrationMethod = "email"
            // createdAt is handled by @ServerTimestamp in User model
        )

        try {
            userRepository.createUserProfile(newUser).getOrThrow()
        } catch (e: Exception) {
            // Rollback: Delete the auth user if profile creation fails
            firebaseUser.delete().await()
            throw e
        }

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
        val firebaseUser = withTimeout(15000.milliseconds) {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw UserNullException()

            // Force a token refresh to ensure Firestore has the latest authentication state
            // and reduce the chance of PERMISSION_DENIED during the following checks.
            try {
                user.getIdToken(true).await()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Token refresh failed during Google sign-in (non-fatal)", e)
            }

            // Check if user profile already exists - with a small retry logic for slow networks/older devices
            var userProfileResult = userRepository.getUserById(user.uid)

            if (userProfileResult.isFailure && userProfileResult.exceptionOrNull() !is UserNotFoundException) {
                kotlinx.coroutines.delay(1000.milliseconds) // Brief pause to allow Firestore/Auth synchronization
                userProfileResult = userRepository.getUserById(user.uid)
            }

            val (extractedFirstName, extractedLastName) = extractNamesFromGoogleUser(user)

            if (userProfileResult.isFailure) {
                val exception = userProfileResult.exceptionOrNull()
                if (exception is UserNotFoundException) {
                    // If user profile doesn't exist, create a basic one from Google data
                    val newUser = User(
                        uid = user.uid,
                        profilePicUrl = user.photoUrl?.toString() ?: "",
                        firstName = extractedFirstName,
                        lastName = extractedLastName,
                        email = user.email ?: "",
                        isFreeNow = false,
                        registrationMethod = "google"
                    )
                    userRepository.createUserProfile(newUser).getOrThrow()
                } else {
                    // If it's still failing with something else (like PERMISSION_DENIED), 
                    // it might be a real issue or persistent sync problem.
                    throw exception ?: Exception("Failed to verify user profile")
                }
            } else {
                // If profile already exists, repair missing names or searchKeywords if needed
                val existingUser = userProfileResult.getOrNull()
                if (existingUser != null) {
                    var needsUpdate = false
                    var updatedUser = existingUser
                    if (existingUser.firstName.isBlank() && existingUser.lastName.isBlank() && extractedFirstName.isNotBlank()) {
                        updatedUser = updatedUser.copy(
                            firstName = extractedFirstName,
                            lastName = extractedLastName
                        )
                        needsUpdate = true
                    }
                    if (updatedUser.searchKeywords.isEmpty()) {
                        updatedUser = updatedUser.copy(
                            searchKeywords = updatedUser.generateSearchKeywords()
                        )
                        needsUpdate = true
                    }
                    if (needsUpdate) {
                        userRepository.updateUser(updatedUser)
                    }
                }
            }
            user
        }
        Result.success(firebaseUser)
    } catch (e: Exception) {
        Log.e("AuthRepository", "Google Sign-In failed or timed out", e)
        Result.failure(mapToAuthException(e))
    }

    private fun extractNamesFromGoogleUser(user: FirebaseUser): Pair<String, String> {
        val displayName = user.displayName?.trim()
        if (!displayName.isNullOrBlank()) {
            val nameParts = displayName.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val firstName = nameParts.firstOrNull() ?: ""
            val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""
            if (firstName.isNotBlank()) {
                return Pair(firstName, lastName)
            }
        }

        // Fallback to email username if displayName is null or blank
        val email = user.email?.trim().orEmpty()
        if (email.isNotBlank()) {
            val emailUsername = email.substringBefore("@")
            val parts = emailUsername.split(".", "_", "-").filter { it.isNotBlank() }
            val firstName = parts.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""
            val lastName = if (parts.size > 1) parts.drop(1).joinToString(" ") { part ->
                part.replaceFirstChar { it.uppercase() }
            } else ""
            return Pair(firstName, lastName)
        }

        return Pair("", "")
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

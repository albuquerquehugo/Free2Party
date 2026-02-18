package com.example.free2party.data.repository

import com.example.free2party.data.model.User
import com.example.free2party.exception.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {
    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<FirebaseUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw UserNullException()

        val newUser = User(
            uid = firebaseUser.uid,
            firstName = firstName,
            lastName = lastName,
            email = email,
            isFreeNow = false
            // createdAt is handled by @ServerTimestamp in User model
        )

        // Use UserRepository to handle profile creation
        userRepository.createUserProfile(newUser).getOrThrow()

        Result.success(firebaseUser)
    } catch (e: Exception) {
        Result.failure(mapToAuthException(e))
    }

    override suspend fun login(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw UserNullException()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(mapToAuthException(e))
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
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

package com.example.free2party.data.repository

import android.net.Uri
import com.example.free2party.data.model.User
import com.example.free2party.exception.DatabaseOperationException
import com.example.free2party.exception.NetworkUnavailableException
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.exception.UserNotFoundException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    override val currentUserId: String,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {
    override fun getCurrentUserStatus(): Flow<Boolean> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToUserException(error))
                    return@addSnapshotListener
                }
                val isFree = snapshot?.getBoolean("isFreeNow") ?: false
                trySend(isFree)
            }
        awaitClose { listener.remove() }
    }

    override fun observeUser(uid: String): Flow<User> = callbackFlow {
        if (uid.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToUserException(error))
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                if (user != null) {
                    trySend(user.copy(uid = snapshot.id))
                } else {
                    close(UserNotFoundException())
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createUserProfile(user: User): Result<Unit> = try {
        db.collection("users").document(user.uid).set(user, SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun getUserById(uid: String): Result<User> = try {
        validateSession()
        val doc = db.collection("users").document(uid).get().await()
        if (!doc.exists()) throw UserNotFoundException()
        val user = doc.toObject(User::class.java) ?: throw UserNotFoundException()
        Result.success(user.copy(uid = doc.id))
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun getUserByEmail(email: String): Result<User> = try {
        validateSession()
        val query = db.collection("users").whereEqualTo("email", email).get().await()
        val doc = query.documents.firstOrNull() ?: throw UserNotFoundException()
        val user = doc.toObject(User::class.java) ?: throw UserNotFoundException()
        Result.success(user.copy(uid = doc.id))
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun updateUser(user: User): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .set(user, SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun toggleAvailability(isFree: Boolean): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .set(mapOf("isFreeNow" to isFree), SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun uploadProfilePicture(uri: Uri): Result<String> = try {
        validateSession()
        val ref = storage.reference.child("profile_pictures/$currentUserId.jpg")
        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    private fun validateSession() {
        if (currentUserId.isBlank()) throw UnauthorizedException()
    }

    private fun mapToUserException(e: Exception): Exception {
        return when (e) {
            is FirebaseNetworkException -> NetworkUnavailableException()
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.NOT_FOUND -> UserNotFoundException()
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> UnauthorizedException("You don't have permission to access this user")
                    FirebaseFirestoreException.Code.UNAVAILABLE -> NetworkUnavailableException()
                    else -> DatabaseOperationException(e.localizedMessage ?: "Database error")
                }
            }

            is UserNotFoundException, is UnauthorizedException -> e
            else -> DatabaseOperationException(e.localizedMessage ?: "Unknown error")
        }
    }
}

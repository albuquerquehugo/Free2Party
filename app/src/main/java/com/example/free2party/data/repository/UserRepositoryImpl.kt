package com.example.free2party.data.repository

import android.net.Uri
import android.util.Log
import com.example.free2party.data.model.User
import com.example.free2party.exception.DatabaseOperationException
import com.example.free2party.exception.NetworkUnavailableException
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.exception.UserNotFoundException
import com.example.free2party.exception.RecentLoginRequiredException
import com.example.free2party.util.removeAccents
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {

    override val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    override val userIdFlow: Flow<String> = callbackFlow {
        val listener = FirebaseAuth.IdTokenListener { firebaseAuth: FirebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid ?: "")
        }
        auth.addIdTokenListener(listener)
        trySend(auth.currentUser?.uid ?: "")
        awaitClose { auth.removeIdTokenListener(listener) }
    }

    override fun getCurrentUserStatus(): Flow<Boolean> = callbackFlow {
        val uid = currentUserId
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
                    Log.d("UserRepository", "observeUser: User $uid not found yet, waiting...")
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
        val normalizedEmail = email.lowercase().removeAccents()
        val query = db.collection("users").whereEqualTo("emailLowercase", normalizedEmail).get().await()
        val doc = query.documents.firstOrNull() ?: throw UserNotFoundException()
        val user = doc.toObject(User::class.java) ?: throw UserNotFoundException()
        Result.success(user.copy(uid = doc.id))
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun updateUser(user: User): Result<Unit> = try {
        val uid = validateSession()
        db.collection("users").document(uid)
            .set(user, SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun toggleAvailability(isFree: Boolean): Result<Unit> = try {
        val uid = validateSession()
        db.collection("users").document(uid)
            .set(mapOf("isFreeNow" to isFree), SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun uploadProfilePicture(uri: Uri): Result<String> = try {
        val uid = validateSession()
        val ref = storage.reference.child("profile_pictures/$uid.jpg")
        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> = try {
        val uid = currentUserId
        if (uid.isNotBlank()) {
            db.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    override suspend fun deleteAccount(): Result<Unit> = try {
        val uid = validateSession()
        val user = auth.currentUser ?: throw UnauthorizedException()

        // Collect all documents to delete (including sub-collections)
        val userRef = db.collection("users").document(uid)
        val collections = listOf("friends", "blocked", "notifications", "plans")
        
        val allDocsToDelete = mutableListOf(userRef)
        for (coll in collections) {
            val snapshot = userRef.collection(coll).get().await()
            allDocsToDelete.addAll(snapshot.documents.map { it.reference })
        }

        // Find and delete friend requests involving this user
        val sentRequests = db.collection("friendRequests")
            .whereEqualTo("senderId", uid).get().await()
        val receivedRequests = db.collection("friendRequests")
            .whereEqualTo("receiverId", uid).get().await()
        
        allDocsToDelete.addAll(sentRequests.documents.map { it.reference })
        allDocsToDelete.addAll(receivedRequests.documents.map { it.reference })

        // Clean up stubs that other users have for this user in their "friends" collections
        val relatedUserIds = mutableSetOf<String>()
        sentRequests.documents.forEach { doc -> doc.getString("receiverId")?.let { relatedUserIds.add(it) } }
        receivedRequests.documents.forEach { doc -> doc.getString("senderId")?.let { relatedUserIds.add(it) } }
        
        // Also check current established friends to be thorough
        val friendsSnapshot = userRef.collection("friends").get().await()
        friendsSnapshot.documents.forEach { doc -> relatedUserIds.add(doc.id) }
        
        relatedUserIds.forEach { otherId ->
            allDocsToDelete.add(
                db.collection("users").document(otherId)
                    .collection("friends").document(uid)
            )
        }

        // Get profile picture URL to check if we should even try to delete it
        val userDoc = userRef.get().await()
        val profilePicUrl = userDoc.getString("profilePicUrl")

        // Delete from Storage if it exists
        if (!profilePicUrl.isNullOrBlank()) {
            try {
                storage.reference.child("profile_pictures/$uid.jpg").delete().await()
            } catch (_: Exception) {
                // Ignore cleanup errors for storage, especially 404s
                Log.d("UserRepository", "Note: Profile picture cleanup skipped or not found.")
            }
        }

        // Perform Firestore deletion in batches
        allDocsToDelete.chunked(500).forEach { chunk ->
            db.runBatch { batch ->
                chunk.forEach { batch.delete(it) }
            }.await()
        }

        // Delete the Auth user last.
        user.delete().await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToUserException(e))
    }

    private fun validateSession(): String {
        val uid = currentUserId
        if (uid.isBlank()) throw UnauthorizedException()
        return uid
    }

    private fun mapToUserException(e: Exception): Exception {
        return when (e) {
            is FirebaseAuthRecentLoginRequiredException -> RecentLoginRequiredException()
            is FirebaseNetworkException -> NetworkUnavailableException()
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.NOT_FOUND -> UserNotFoundException()
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> UnauthorizedException("You don't have permission to access this user")
                    FirebaseFirestoreException.Code.UNAVAILABLE -> NetworkUnavailableException()
                    else -> DatabaseOperationException(e.localizedMessage ?: "Database error")
                }
            }
            is StorageException -> {
                when (e.errorCode) {
                    StorageException.ERROR_NOT_AUTHENTICATED -> UnauthorizedException("Not authenticated")
                    StorageException.ERROR_NOT_AUTHORIZED -> UnauthorizedException("Permission denied on storage")
                    StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> NetworkUnavailableException("Upload timed out")
                    else -> DatabaseOperationException("Storage error: ${e.message}")
                }
            }
            is UserNotFoundException, is UnauthorizedException -> e
            else -> DatabaseOperationException(e.localizedMessage ?: "Unknown error")
        }
    }
}

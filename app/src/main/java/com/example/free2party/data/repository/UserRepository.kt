package com.example.free2party.data.repository

import com.example.free2party.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val currentUserId: String
    fun getCurrentUserStatus(): Flow<Boolean>
    fun observeUser(uid: String): Flow<User>
    suspend fun createUserProfile(user: User): Result<Unit>
    suspend fun getUserById(uid: String): Result<User>
    suspend fun getUserByEmail(email: String): Result<User>
    suspend fun updateUserName(name: String): Result<Unit>
    suspend fun toggleAvailability(isFree: Boolean): Result<Unit>
}

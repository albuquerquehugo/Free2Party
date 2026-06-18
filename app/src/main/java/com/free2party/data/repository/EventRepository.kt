package com.free2party.data.repository

import android.net.Uri
import com.free2party.data.model.Event
import com.free2party.data.model.EventComment
import com.free2party.data.model.EventPhoto
import com.free2party.data.model.GuestStatus
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<List<Event>>
    fun getEventDetails(eventId: String): Flow<Event>
    fun getComments(eventId: String): Flow<List<EventComment>>
    fun getPhotos(eventId: String): Flow<List<EventPhoto>>
    suspend fun saveEvent(event: Event): Result<String>
    suspend fun updateEvent(event: Event): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
    suspend fun respondToEvent(eventId: String, status: GuestStatus): Result<Unit>
    suspend fun addComment(eventId: String, text: String): Result<Unit>
    suspend fun editComment(eventId: String, commentId: String, newText: String): Result<Unit>
    suspend fun deleteComment(eventId: String, commentId: String): Result<Unit>
    suspend fun uploadPhoto(eventId: String, uri: Uri): Result<Unit>
    suspend fun deletePhoto(eventId: String, photoId: String, storageUrl: String): Result<Unit>
}

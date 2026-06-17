package com.free2party.data.repository

import android.net.Uri
import com.free2party.data.model.Event
import com.free2party.data.model.EventComment
import com.free2party.data.model.EventPhoto
import com.free2party.data.model.EventType
import com.free2party.data.model.GuestStatus
import com.free2party.exception.DatabaseOperationException
import com.free2party.exception.EventNotFoundException
import com.free2party.exception.InvalidEventDataException
import com.free2party.exception.PastEventDateTimeException
import com.free2party.exception.GuestsMandatoryPrivateException
import com.free2party.exception.LocationMandatoryException
import com.free2party.exception.NetworkUnavailableException
import com.free2party.exception.UnauthorizedException
import com.free2party.util.isDateTimeInPast
import com.free2party.util.parseDateToMillis
import com.free2party.util.parseTimeToMinutes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import android.content.Context
import com.free2party.R
import com.free2party.data.model.NotificationType
import com.google.firebase.firestore.FieldValue
import dagger.hilt.android.qualifiers.ApplicationContext

class EventRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @param:ApplicationContext private val context: Context
) : EventRepository {

    private val currentUserId: String get() = auth.currentUser?.uid ?: ""

    override fun getEvents(): Flow<List<Event>> {
        val uid = currentUserId
        if (uid.isBlank()) {
            return callbackFlow {
                close(UnauthorizedException())
            }
        }

        // Listener for hosted events
        val hostedFlow = callbackFlow {
            val listener = db.collection("events")
                .whereEqualTo("hostId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(mapToEventException(error))
                        return@addSnapshotListener
                    }
                    val events = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Event::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(events)
                }
            awaitClose { listener.remove() }
        }

        // Listener for events where user is pending/guest
        val guestFlow = callbackFlow {
            val listener = db.collection("events")
                .whereArrayContains("guestIds", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(mapToEventException(error))
                        return@addSnapshotListener
                    }
                    val events = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Event::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(events)
                }
            awaitClose { listener.remove() }
        }

        // Listener for public events
        val publicFlow = callbackFlow {
            val listener = db.collection("events")
                .whereEqualTo("type", EventType.PUBLIC.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(mapToEventException(error))
                        return@addSnapshotListener
                    }
                    val events = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Event::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(events)
                }
            awaitClose { listener.remove() }
        }

        // Combine all flows and remove duplicates, sorting by startDate/startTime
        return combine(hostedFlow, guestFlow, publicFlow) { hosted, guest, public ->
            val allEvents = (hosted + guest + public).distinctBy { it.id }
            allEvents.sortedWith(compareBy({ it.startDate }, { it.startTime }))
        }
    }

    override fun getEventDetails(eventId: String): Flow<Event> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("events").document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToEventException(error))
                    return@addSnapshotListener
                }
                val event = snapshot?.toObject(Event::class.java)?.copy(id = snapshot.id)
                if (event != null) {
                    trySend(event)
                } else {
                    close(EventNotFoundException())
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getComments(eventId: String): Flow<List<EventComment>> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("events").document(eventId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToEventException(error))
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(EventComment::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override fun getPhotos(eventId: String): Flow<List<EventPhoto>> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("events").document(eventId)
            .collection("photos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToEventException(error))
                    return@addSnapshotListener
                }
                val photos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(EventPhoto::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(photos)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveEvent(event: Event): Result<String> = try {
        validateSession()
        validateEventDetails(event)

        val docRef = db.collection("events").document()
        // Ensure guestIds matches guests keys list
        val guestIdsList = event.guests.keys.toList()
        val eventWithDetails = event.copy(
            id = docRef.id,
            hostId = currentUserId,
            guestIds = guestIdsList,
            invitedGuestIds = guestIdsList
        )

        db.runTransaction { transaction ->
            transaction.set(docRef, eventWithDetails)

            // Create notification for each guest ID (except the host)
            guestIdsList.filter { it != currentUserId }.forEach { guestId ->
                val notifRef = db.collection("users").document(guestId)
                    .collection("notifications").document()
                transaction.set(
                    notifRef, mapOf(
                        "id" to notifRef.id,
                        "title" to context.getString(R.string.notification_event_invitation_title),
                        "message" to context.getString(
                            R.string.notification_event_invitation_body,
                            eventWithDetails.hostName.ifBlank { "Someone" },
                            eventWithDetails.hostEmail,
                            eventWithDetails.title
                        ),
                        "isRead" to false,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "type" to NotificationType.EVENT_INVITE.name
                    )
                )
            }
        }.await()

        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(mapToEventException(e))
    }

    override suspend fun updateEvent(event: Event): Result<Unit> = try {
        validateSession()
        validateEventDetails(event)

        val docRef = db.collection("events").document(event.id)
        val guestIdsList = event.guests.keys.toList()
        val updatedData = mapOf(
            "title" to event.title,
            "description" to event.description,
            "type" to event.type.name,
            "startDate" to event.startDate,
            "startTime" to event.startTime,
            "endDate" to event.endDate,
            "endTime" to event.endTime,
            "timezone" to event.timezone,
            "locationName" to event.locationName,
            "latitude" to event.latitude,
            "longitude" to event.longitude,
            "guests" to event.guests,
            "guestIds" to guestIdsList,
            "invitedGuestIds" to guestIdsList,
            "usefulLinks" to event.usefulLinks.map { mapOf("title" to it.title, "url" to it.url) }
        )

        db.runTransaction { transaction ->
            val oldDoc = transaction.get(docRef)
            val oldGuestIds =
                (oldDoc.get("guestIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            // Perform the update
            transaction.update(docRef, updatedData)

            // Send notification only to newly added guest IDs (excluding host)
            val newGuestIds = guestIdsList.filter { (it !in oldGuestIds) && (it != currentUserId) }
            val hostName = oldDoc.getString("hostName") ?: "Someone"
            val hostEmail = oldDoc.getString("hostEmail") ?: ""

            newGuestIds.forEach { guestId ->
                val notifRef = db.collection("users").document(guestId)
                    .collection("notifications").document()
                transaction.set(
                    notifRef, mapOf(
                        "id" to notifRef.id,
                        "title" to context.getString(R.string.notification_event_invitation_title),
                        "message" to context.getString(
                            R.string.notification_event_invitation_body,
                            hostName,
                            hostEmail,
                            event.title
                        ),
                        "isRead" to false,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "type" to NotificationType.EVENT_INVITE.name
                    )
                )
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToEventException(e))
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = try {
        validateSession()

        // 1. Delete all comments
        val commentsSnapshot = db.collection("events").document(eventId)
            .collection("comments").get().await()
        commentsSnapshot.documents.forEach { doc ->
            doc.reference.delete().await()
        }

        // 2. Delete all photos in Storage & Firestore subcollection
        val photosSnapshot = db.collection("events").document(eventId)
            .collection("photos").get().await()
        photosSnapshot.documents.forEach { doc ->
            val photo = doc.toObject(EventPhoto::class.java)
            if (photo != null) {
                try {
                    storage.getReferenceFromUrl(photo.url).delete().await()
                } catch (_: Exception) {
                }
            }
            doc.reference.delete().await()
        }

        // 3. Delete event document itself
        db.collection("events").document(eventId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToEventException(e))
    }

    override suspend fun respondToEvent(eventId: String, status: GuestStatus): Result<Unit> = try {
        val uid = validateSession()
        val docRef = db.collection("events").document(eventId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val guestIds =
                (snapshot.get("guestIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val invitedGuestIds =
                (snapshot.get("invitedGuestIds") as? List<*>)?.filterIsInstance<String>()
            val isPublic = snapshot.getString("type") == EventType.PUBLIC.name

            val updates = mutableMapOf<String, Any>()
            if (isPublic && invitedGuestIds == null) {
                updates["invitedGuestIds"] = guestIds
            }

            if (isPublic && !guestIds.contains(uid)) {
                updates["guests.$uid"] = status.name
                updates["guestIds"] = FieldValue.arrayUnion(uid)
                transaction.update(docRef, updates)
            } else {
                if (updates.isNotEmpty()) {
                    updates["guests.$uid"] = status.name
                    transaction.update(docRef, updates)
                } else {
                    transaction.update(docRef, "guests.$uid", status.name)
                }
            }
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToEventException(e))
    }

    override suspend fun addComment(eventId: String, text: String): Result<Unit> = try {
        val uid = validateSession()
        if (text.isBlank()) throw InvalidEventDataException("Comment text cannot be empty")

        // Fetch user info for comment caching
        val userDoc = db.collection("users").document(uid).get().await()
        val userName =
            "${userDoc.getString("firstName") ?: ""} ${userDoc.getString("lastName") ?: ""}".trim()
        val userProfilePic = userDoc.getString("profilePicUrl") ?: ""

        val commentRef = db.collection("events").document(eventId)
            .collection("comments").document()

        val comment = EventComment(
            id = commentRef.id,
            userId = uid,
            userName = userName,
            userProfilePic = userProfilePic,
            text = text,
            createdAt = Date()
        )
        commentRef.set(comment).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToEventException(e))
    }

    override suspend fun deleteComment(eventId: String, commentId: String): Result<Unit> = try {
        validateSession()
        db.collection("events").document(eventId)
            .collection("comments").document(commentId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToEventException(e))
    }

    override suspend fun uploadPhoto(eventId: String, uri: Uri): Result<Unit> = try {
        val uid = validateSession()
        val photoId = UUID.randomUUID().toString()
        val ref = storage.reference.child("event_photos/$eventId/$photoId.jpg")

        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        val photoRef = db.collection("events").document(eventId)
            .collection("photos").document(photoId)

        val photo = EventPhoto(
            id = photoId,
            uploadedBy = uid,
            url = downloadUrl,
            createdAt = Date()
        )
        photoRef.set(photo).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToEventException(e))
    }

    override suspend fun deletePhoto(
        eventId: String,
        photoId: String,
        storageUrl: String
    ): Result<Unit> = try {
        validateSession()
        try {
            storage.getReferenceFromUrl(storageUrl).delete().await()
        } catch (_: Exception) {
        }

        db.collection("events").document(eventId)
            .collection("photos").document(photoId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToEventException(e))
    }

    private fun validateSession(): String {
        val uid = currentUserId
        if (uid.isBlank()) throw UnauthorizedException()
        return uid
    }

    private fun validateEventDetails(event: Event) {
        if (event.title.isBlank()) throw InvalidEventDataException("Title cannot be empty")
        if (event.startDate.isBlank() || event.endDate.isBlank())
            throw InvalidEventDataException("Dates cannot be empty")
        if (event.startTime.isBlank() || event.endTime.isBlank())
            throw InvalidEventDataException("Times cannot be empty")

        if (event.type == EventType.PRIVATE && event.guests.isEmpty()) {
            throw GuestsMandatoryPrivateException()
        }

        if (event.locationName.isBlank() || event.latitude == null || event.longitude == null) {
            throw LocationMandatoryException()
        }

        val startDateMillis = parseDateToMillis(event.startDate)
            ?: throw InvalidEventDataException("Invalid start date format")
        val endDateMillis = parseDateToMillis(event.endDate)
            ?: throw InvalidEventDataException("Invalid end date format")

        if (isDateTimeInPast(startDateMillis, event.startTime)) {
            throw PastEventDateTimeException()
        }

        if (startDateMillis > endDateMillis) {
            throw InvalidEventDataException("End date must be after start date")
        }

        val startTimeMinutes = parseTimeToMinutes(event.startTime)
            ?: throw InvalidEventDataException("Invalid start time format")
        val endTimeMinutes = parseTimeToMinutes(event.endTime)
            ?: throw InvalidEventDataException("Invalid end time format")

        if (startDateMillis == endDateMillis && startTimeMinutes >= endTimeMinutes) {
            throw InvalidEventDataException("End time must be after start time")
        }
    }

    private fun mapToEventException(e: Exception): Exception {
        return when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.NOT_FOUND -> EventNotFoundException()
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> UnauthorizedException()
                    FirebaseFirestoreException.Code.UNAVAILABLE -> NetworkUnavailableException()
                    else -> DatabaseOperationException(e.localizedMessage ?: "Database error")
                }
            }

            is FirebaseNetworkException -> NetworkUnavailableException()
            is StorageException -> {
                when (e.errorCode) {
                    StorageException.ERROR_NOT_AUTHENTICATED -> UnauthorizedException()
                    StorageException.ERROR_NOT_AUTHORIZED -> UnauthorizedException()
                    StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> NetworkUnavailableException()
                    else -> DatabaseOperationException("Storage error: ${e.message}")
                }
            }

            else -> e
        }
    }
}

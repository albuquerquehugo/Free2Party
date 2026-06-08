package com.free2party.data.repository

import com.free2party.data.model.Event
import com.free2party.data.model.GuestStatus
import com.free2party.exception.InvalidEventDataException
import com.free2party.exception.UnauthorizedException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryTest {
    private lateinit var repository: EventRepositoryImpl
    private val auth: FirebaseAuth = mockk()
    private val db: FirebaseFirestore = mockk()
    private val storage: FirebaseStorage = mockk()
    private val firebaseUser: FirebaseUser = mockk()
    private val eventsCollection: CollectionReference = mockk()
    private val eventDoc: DocumentReference = mockk()

    @Before
    fun setup() {
        every { db.collection("events") } returns eventsCollection
        every { eventsCollection.document(any()) } returns eventDoc
        every { eventsCollection.document() } returns eventDoc
        
        repository = EventRepositoryImpl(auth, db, storage)
        
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `saveEvent fails if user not logged in`() = runTest {
        every { auth.currentUser } returns null
        val event = Event(
            title = "Test Event",
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00"
        )
        val result = repository.saveEvent(event)
        assertTrue("Expected failure for unauthorized user", result.isFailure)
        assertTrue("Expected UnauthorizedException", result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `saveEvent fails if title is blank`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            title = "",
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00"
        )
        val result = repository.saveEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventDataException)
    }

    @Test
    fun `saveEvent succeeds with valid details`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            title = "Awesome Party",
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00"
        )
        every { eventDoc.id } returns "newEventId"
        every { eventDoc.set(any()) } returns Tasks.forResult(null)

        val result = repository.saveEvent(event)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == "newEventId")
    }

    @Test
    fun `updateEvent fails if user not logged in`() = runTest {
        every { auth.currentUser } returns null
        val event = Event(id = "123")
        val result = repository.updateEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `deleteEvent fails if user not logged in`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.deleteEvent("123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `respondToEvent fails if user not logged in`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.respondToEvent("123", GuestStatus.ACCEPTED)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `respondToEvent succeeds if logged in`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"
        
        every { eventsCollection.document("123") } returns eventDoc
        every { eventDoc.update("guests.testUser", "ACCEPTED") } returns Tasks.forResult(null)

        val result = repository.respondToEvent("123", GuestStatus.ACCEPTED)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addComment fails if text is blank`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val result = repository.addComment("123", "   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventDataException)
    }
}

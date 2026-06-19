package com.free2party.data.repository

import com.free2party.data.model.Event
import com.free2party.data.model.GuestStatus
import com.free2party.data.model.EventType
import com.free2party.exception.InvalidEventDataException
import com.free2party.exception.PastEventDateTimeException
import com.free2party.exception.UnauthorizedException
import com.free2party.exception.GuestsMandatoryPrivateException
import com.free2party.exception.LocationMandatoryException
import com.free2party.exception.EventAlreadyStartedException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.storage.FirebaseStorage
import android.content.Context
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
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        every { db.collection("events") } returns eventsCollection
        every { eventsCollection.document(any()) } returns eventDoc
        every { eventsCollection.document() } returns eventDoc

        val usersCollection = mockk<CollectionReference>(relaxed = true)
        val userDoc = mockk<DocumentReference>(relaxed = true)
        val notificationsCollection = mockk<CollectionReference>(relaxed = true)
        val notifDoc = mockk<DocumentReference>(relaxed = true)
        every { db.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns userDoc
        every { userDoc.collection("notifications") } returns notificationsCollection
        every { notificationsCollection.document() } returns notifDoc

        repository = EventRepositoryImpl(auth, db, storage, context)

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
        assertTrue(
            "Expected UnauthorizedException",
            result.exceptionOrNull() is UnauthorizedException
        )
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
    fun `saveEvent fails if start date is in the past`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            title = "Test Event",
            startDate = "2020-01-01",
            endDate = "2020-01-01",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        val result = repository.saveEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PastEventDateTimeException)
    }

    @Test
    fun `saveEvent fails if end date is before start date`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            title = "Test Event",
            startDate = "2026-07-10",
            endDate = "2026-07-09",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        val result = repository.saveEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventDataException)
    }

    @Test
    fun `saveEvent fails if end time is before start time on same day`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            title = "Test Event",
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "11:00",
            endTime = "10:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
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
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        every { eventDoc.id } returns "newEventId"

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            every { transaction.set(any(), any()) } returns transaction
            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.saveEvent(event)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == "newEventId")
    }

    @Test
    fun `saveEvent succeeds with valid details for public event with empty guests`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            title = "Awesome Public Party",
            type = EventType.PUBLIC,
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = emptyMap()
        )
        every { eventDoc.id } returns "newEventId"

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            every { transaction.set(any(), any()) } returns transaction
            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.saveEvent(event)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == "newEventId")
    }

    @Test
    fun `saveEvent fails if private event has no guests`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            title = "Awesome Party",
            type = EventType.PRIVATE,
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = emptyMap()
        )
        val result = repository.saveEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GuestsMandatoryPrivateException)
    }

    @Test
    fun `saveEvent fails if location is missing`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            title = "Awesome Party",
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "",
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        val result = repository.saveEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is LocationMandatoryException)
    }

    @Test
    fun `updateEvent fails if location is missing`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            id = "event123",
            title = "Awesome Party Updated",
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "",
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        val result = repository.updateEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is LocationMandatoryException)
    }

    @Test
    fun `updateEvent succeeds with valid details`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            id = "event123",
            title = "Awesome Party Updated",
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        every { eventsCollection.document("event123") } returns eventDoc
        val oldDoc = mockk<DocumentSnapshot>()
        every { oldDoc.exists() } returns true
        every { oldDoc.getString("startDate") } returns "2026-07-10"
        every { oldDoc.getString("startTime") } returns "10:00"
        every { oldDoc.get("guestIds") } returns emptyList<String>()
        every { oldDoc.getString("hostName") } returns "Host Name"
        every { oldDoc.getString("hostEmail") } returns "host@test.com"

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            every { transaction.get(eventDoc) } returns oldDoc
            every { transaction.update(eventDoc, any<Map<String, Any?>>()) } returns transaction
            every { transaction.set(any(), any()) } returns transaction
            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.updateEvent(event)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `updateEvent succeeds with valid details for public event with empty guests`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            id = "event123",
            title = "Awesome Public Party Updated",
            type = EventType.PUBLIC,
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = emptyMap()
        )
        every { eventsCollection.document("event123") } returns eventDoc
        val oldDoc = mockk<DocumentSnapshot>()
        every { oldDoc.exists() } returns true
        every { oldDoc.getString("startDate") } returns "2026-07-10"
        every { oldDoc.getString("startTime") } returns "10:00"
        every { oldDoc.get("guestIds") } returns emptyList<String>()
        every { oldDoc.getString("hostName") } returns "Host Name"
        every { oldDoc.getString("hostEmail") } returns "host@test.com"

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            every { transaction.get(eventDoc) } returns oldDoc
            every { transaction.update(eventDoc, any<Map<String, Any?>>()) } returns transaction
            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.updateEvent(event)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `updateEvent fails if private event has no guests`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            id = "event123",
            title = "Awesome Party Updated",
            type = EventType.PRIVATE,
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = emptyMap()
        )
        val result = repository.updateEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GuestsMandatoryPrivateException)
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
    fun `updateEvent fails if start date is in the past`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            id = "event123",
            title = "Awesome Party Updated",
            startDate = "2020-01-01",
            endDate = "2020-01-01",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        val result = repository.updateEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PastEventDateTimeException)
    }

    @Test
    fun `updateEvent fails if existing event has already started`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            id = "event123",
            title = "Awesome Party Updated",
            startDate = "2026-07-10",
            endDate = "2026-07-10",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        every { eventsCollection.document("event123") } returns eventDoc
        val oldDoc = mockk<DocumentSnapshot>()
        every { oldDoc.exists() } returns true
        every { oldDoc.getString("startDate") } returns "2020-01-01"
        every { oldDoc.getString("startTime") } returns "10:00"
        every { oldDoc.get("guestIds") } returns emptyList<String>()
        every { oldDoc.getString("hostName") } returns "Host Name"
        every { oldDoc.getString("hostEmail") } returns "host@test.com"

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            every { transaction.get(eventDoc) } returns oldDoc
            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.updateEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is EventAlreadyStartedException)
    }

    @Test
    fun `updateEvent fails if end date is before start date`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val event = Event(
            id = "event123",
            title = "Awesome Party Updated",
            startDate = "2026-07-10",
            endDate = "2026-07-09",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Somewhere",
            latitude = 10.0,
            longitude = 10.0,
            guests = mapOf("guest1" to GuestStatus.PENDING.name)
        )
        val result = repository.updateEvent(event)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventDataException)
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
    fun `respondToEvent succeeds and updates guests status only if already guest or private`() =
        runTest {
            every { auth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns "testUser"

            every { eventsCollection.document("123") } returns eventDoc
            val oldDoc = mockk<DocumentSnapshot>()
            every { oldDoc.get("guestIds") } returns listOf("testUser")
            every { oldDoc.get("invitedGuestIds") } returns listOf("testUser")
            every { oldDoc.getString("type") } returns "PRIVATE"

            val transaction = mockk<Transaction>()
            every { db.runTransaction<Unit>(any()) } answers {
                val function = firstArg<Transaction.Function<Unit>>()
                every { transaction.get(eventDoc) } returns oldDoc
                every {
                    transaction.update(
                        eventDoc,
                        "guests.testUser",
                        "ACCEPTED"
                    )
                } returns transaction
                function.apply(transaction)
                Tasks.forResult(Unit)
            }

            val result = repository.respondToEvent("123", GuestStatus.ACCEPTED)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `respondToEvent adds guest to guestIds if event is public and user not in guestIds`() =
        runTest {
            every { auth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns "testUser"

            every { eventsCollection.document("123") } returns eventDoc
            val oldDoc = mockk<DocumentSnapshot>()
            every { oldDoc.get("guestIds") } returns emptyList<String>()
            every { oldDoc.get("invitedGuestIds") } returns emptyList<String>()
            every { oldDoc.getString("type") } returns "PUBLIC"

            val transaction = mockk<Transaction>()
            every { db.runTransaction<Unit>(any()) } answers {
                val function = firstArg<Transaction.Function<Unit>>()
                every { transaction.get(eventDoc) } returns oldDoc
                every { transaction.update(eventDoc, any<Map<String, Any>>()) } returns transaction
                function.apply(transaction)
                Tasks.forResult(Unit)
            }

            val result = repository.respondToEvent("123", GuestStatus.ACCEPTED)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `respondToEvent adds guest to guestIds if event is public and user declines`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        every { eventsCollection.document("123") } returns eventDoc
        val oldDoc = mockk<DocumentSnapshot>()
        every { oldDoc.get("guestIds") } returns emptyList<String>()
        every { oldDoc.get("invitedGuestIds") } returns emptyList<String>()
        every { oldDoc.getString("type") } returns "PUBLIC"

        val transaction = mockk<Transaction>()
        every { db.runTransaction<Unit>(any()) } answers {
            val function = firstArg<Transaction.Function<Unit>>()
            every { transaction.get(eventDoc) } returns oldDoc
            every { transaction.update(eventDoc, any<Map<String, Any>>()) } returns transaction
            function.apply(transaction)
            Tasks.forResult(Unit)
        }

        val result = repository.respondToEvent("123", GuestStatus.DECLINED)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `respondToEvent updates guest status only if event is public, user is invited and status is declined`() =
        runTest {
            every { auth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns "testUser"

            every { eventsCollection.document("123") } returns eventDoc
            val oldDoc = mockk<DocumentSnapshot>()
            every { oldDoc.get("guestIds") } returns listOf("testUser")
            every { oldDoc.get("invitedGuestIds") } returns listOf("testUser")
            every { oldDoc.getString("type") } returns "PUBLIC"

            val transaction = mockk<Transaction>()
            every { db.runTransaction<Unit>(any()) } answers {
                val function = firstArg<Transaction.Function<Unit>>()
                every { transaction.get(eventDoc) } returns oldDoc
                every {
                    transaction.update(
                        eventDoc,
                        "guests.testUser",
                        "DECLINED"
                    )
                } returns transaction
                function.apply(transaction)
                Tasks.forResult(Unit)
            }

            val result = repository.respondToEvent("123", GuestStatus.DECLINED)
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

    @Test
    fun `editComment fails if text is blank`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val result = repository.editComment("123", "commentId", "   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidEventDataException)
    }
}

package com.example.free2party.data.repository

import com.example.free2party.data.model.FuturePlan
import com.example.free2party.exception.InvalidPlanDataException
import com.example.free2party.exception.OverlappingPlanException
import com.example.free2party.exception.PastDateTimeException
import com.example.free2party.exception.UnauthorizedException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanRepositoryTest {
    private lateinit var repository: PlanRepositoryImpl
    private val auth: FirebaseAuth = mockk()
    private val db: FirebaseFirestore = mockk()
    private val firebaseUser: FirebaseUser = mockk()
    private val usersCollection: CollectionReference = mockk()
    private val userDoc: DocumentReference = mockk()
    private val plansCollection: CollectionReference = mockk()

    @Before
    fun setup() {
        every { db.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns userDoc
        every { userDoc.collection("plans") } returns plansCollection
        
        repository = PlanRepositoryImpl(auth, db)
        
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `savePlan fails if user not logged in`() = runTest {
        every { auth.currentUser } returns null
        val plan = FuturePlan(
            startDate = "2026-01-01",
            endDate = "2026-01-01",
            startTime = "10:00",
            endTime = "11:00",
            note = "Test"
        )
        val result = repository.savePlan(plan)
        assertTrue("Expected failure for unauthorized user", result.isFailure)
        assertTrue("Expected UnauthorizedException", result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `savePlan fails if start date is in the past`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"
        
        val plan = FuturePlan(
            startDate = "2020-01-01",
            endDate = "2020-01-01",
            startTime = "10:00",
            endTime = "11:00"
        )
        val result = repository.savePlan(plan)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PastDateTimeException)
    }

    @Test
    fun `savePlan fails if end date is before start date`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"
        
        val plan = FuturePlan(
            startDate = "2026-05-10",
            endDate = "2026-05-09",
            startTime = "10:00",
            endTime = "11:00"
        )
        val result = repository.savePlan(plan)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidPlanDataException)
    }

    @Test
    fun `savePlan fails if plan overlaps with existing plan`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val newPlan = FuturePlan(
            startDate = "2026-05-10",
            endDate = "2026-05-10",
            startTime = "10:00",
            endTime = "12:00"
        )

        val existingPlan = FuturePlan(
            id = "existing",
            startDate = "2026-05-10",
            endDate = "2026-05-10",
            startTime = "11:00",
            endTime = "13:00"
        )

        val querySnapshot: QuerySnapshot = mockk()
        every { plansCollection.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.documents } returns listOf(mockk {
            every { toObject(FuturePlan::class.java) } returns existingPlan
            every { id } returns "existing"
        })

        val result = repository.savePlan(newPlan)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OverlappingPlanException)
    }

    @Test
    fun `savePlan success if no overlap and data valid`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val plan = FuturePlan(
            startDate = "2026-05-10",
            endDate = "2026-05-10",
            startTime = "10:00",
            endTime = "11:00",
            note = "Valid plan"
        )

        val querySnapshot: QuerySnapshot = mockk()
        every { plansCollection.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.documents } returns emptyList()

        val planDoc: DocumentReference = mockk()
        every { plansCollection.document() } returns planDoc
        every { planDoc.id } returns "newId"
        every { planDoc.set(any()) } returns Tasks.forResult(null)

        val result = repository.savePlan(plan)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `updatePlan fails if session invalid`() = runTest {
        every { auth.currentUser } returns null
        val plan = FuturePlan(id = "123")
        val result = repository.updatePlan(plan)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `updatePlan fails on overlap with other plans`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"

        val editingPlan = FuturePlan(
            id = "editing",
            startDate = "2026-05-10",
            endDate = "2026-05-10",
            startTime = "10:00",
            endTime = "12:00"
        )

        val otherPlan = FuturePlan(
            id = "other",
            startDate = "2026-05-10",
            endDate = "2026-05-10",
            startTime = "11:00",
            endTime = "13:00"
        )

        val querySnapshot: QuerySnapshot = mockk()
        every { plansCollection.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.documents } returns listOf(
            mockk { 
                every { toObject(FuturePlan::class.java) } returns editingPlan
                every { id } returns "editing"
            },
            mockk { 
                every { toObject(FuturePlan::class.java) } returns otherPlan
                every { id } returns "other"
            }
        )

        val result = repository.updatePlan(editingPlan)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OverlappingPlanException)
    }

    @Test
    fun `deletePlan fails if user not logged in`() = runTest {
        every { auth.currentUser } returns null
        val result = repository.deletePlan("123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `deletePlan success if logged in`() = runTest {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "testUser"
        
        val planDoc: DocumentReference = mockk()
        every { plansCollection.document("123") } returns planDoc
        every { planDoc.delete() } returns Tasks.forResult(null)
        
        val result = repository.deletePlan("123")
        assertTrue(result.isSuccess)
    }
}

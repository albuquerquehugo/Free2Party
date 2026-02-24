package com.example.free2party.ui.screens.calendar

import com.example.free2party.data.model.FuturePlan
import com.example.free2party.data.model.PlanVisibility
import com.example.free2party.data.model.User
import com.example.free2party.data.repository.PlanRepository
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private lateinit var viewModel: CalendarViewModel
    private val planRepository: PlanRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val socialRepository: SocialRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val plansFlow = MutableStateFlow<List<FuturePlan>>(emptyList())
    private val userFlow = MutableStateFlow(User(uid = "testUser"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        plansFlow.value = emptyList()
        userFlow.value = User(uid = "testUser")
        
        // Default behavior for getPlans and observeUser to prevent issues during init
        every { planRepository.getOwnPlans() } returns plansFlow
        every { planRepository.getPublicPlans(any()) } returns plansFlow
        every { userRepository.currentUserId } returns "testUser"
        every { userRepository.observeUser(any()) } returns userFlow
        every { socialRepository.getFriendsList() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `moveToNextMonth increments month correctly`() = runTest {
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        viewModel.displayedMonth = Calendar.JANUARY
        viewModel.displayedYear = 2026

        viewModel.moveToNextMonth()

        assertEquals(Calendar.FEBRUARY, viewModel.displayedMonth)
        assertEquals(2026, viewModel.displayedYear)
    }

    @Test
    fun `moveToNextMonth rolls over year correctly`() = runTest {
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        viewModel.displayedMonth = Calendar.DECEMBER
        viewModel.displayedYear = 2025

        viewModel.moveToNextMonth()

        assertEquals(Calendar.JANUARY, viewModel.displayedMonth)
        assertEquals(2026, viewModel.displayedYear)
    }

    @Test
    fun `moveToPreviousMonth decrements month correctly`() = runTest {
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        viewModel.displayedMonth = Calendar.FEBRUARY
        viewModel.displayedYear = 2026

        viewModel.moveToPreviousMonth()

        assertEquals(Calendar.JANUARY, viewModel.displayedMonth)
        assertEquals(2026, viewModel.displayedYear)
    }

    @Test
    fun `moveToPreviousMonth rolls over year correctly`() = runTest {
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        viewModel.displayedMonth = Calendar.JANUARY
        viewModel.displayedYear = 2026

        viewModel.moveToPreviousMonth()

        assertEquals(Calendar.DECEMBER, viewModel.displayedMonth)
        assertEquals(2025, viewModel.displayedYear)
    }

    @Test
    fun `getPlannedDaysForMonth identifies correct days`() = runTest {
        val plans = listOf(
            FuturePlan(
                startDate = "2026-05-10",
                endDate = "2026-05-12",
                startTime = "10:00",
                endTime = "18:00"
            ),
            FuturePlan(
                startDate = "2026-05-20",
                endDate = "2026-05-20",
                startTime = "08:00",
                endTime = "09:00"
            )
        )
        
        plansFlow.value = plans
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")

        val plannedDays = viewModel.getPlannedDaysForMonth(2026, Calendar.MAY)

        assertEquals(setOf(10, 11, 12, 20), plannedDays)
    }

    @Test
    fun `getPlannedDaysForMonth handles exclusive end date at midnight`() = runTest {
        val plans = listOf(
            FuturePlan(
                startDate = "2026-05-10",
                endDate = "2026-05-12",
                startTime = "10:00",
                endTime = "00:00"
            )
        )
        plansFlow.value = plans
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")

        val plannedDays = viewModel.getPlannedDaysForMonth(2026, Calendar.MAY)

        // End date 12 is exclusive because time is 00:00
        assertEquals(setOf(10, 11), plannedDays)
    }

    @Test
    fun `filteredPlans filters and sorts correctly`() = runTest {
        val plans = listOf(
            FuturePlan(
                startDate = "2026-05-10",
                endDate = "2026-05-10",
                startTime = "14:00",
                endTime = "15:00",
                note = "Later plan"
            ),
            FuturePlan(
                startDate = "2026-05-10",
                endDate = "2026-05-10",
                startTime = "09:00",
                endTime = "10:00",
                note = "Earlier plan"
            ),
            FuturePlan(
                startDate = "2026-05-11",
                endDate = "2026-05-11",
                startTime = "10:00",
                endTime = "11:00",
                note = "Next day plan"
            )
        )
        plansFlow.value = plans
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")

        // Set selected date to 2026-05-10 in UTC
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.MAY, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        viewModel.selectedDateMillis = calendar.timeInMillis

        val filtered = viewModel.filteredPlans

        assertEquals(2, filtered.size)
        assertEquals("Earlier plan", filtered[0].note)
        assertEquals("Later plan", filtered[1].note)
    }

    @Test
    fun `savePlan calls repository and triggers onSuccess`() = runTest {
        coEvery { planRepository.savePlan(any()) } returns Result.success(Unit)
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        var successCalled = false

        val friendsSelection = listOf("friend1", "friend2")
        viewModel.savePlan(
            startDate = "2026-06-01",
            endDate = "2026-06-01",
            startTime = "10:00",
            endTime = "11:00",
            note = "Test Note",
            visibility = PlanVisibility.ONLY,
            friendsSelection = friendsSelection,
            onValidationError = {},
            onSuccess = { successCalled = true }
        )

        coVerify { 
            planRepository.savePlan(match { 
                it.note == "Test Note" && 
                it.visibility == PlanVisibility.ONLY && 
                it.friendsSelection == friendsSelection
            }) 
        }
        assertTrue(successCalled)
    }

    @Test
    fun `savePlan triggers onValidationError on failure`() = runTest {
        val errorMessage = "Overlap detected"
        coEvery { planRepository.savePlan(any()) } returns Result.failure(Exception(errorMessage))
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        var errorReceived = ""

        viewModel.savePlan(
            startDate = "2026-06-01",
            endDate = "2026-06-01",
            startTime = "10:00",
            endTime = "11:00",
            note = "Test Note",
            visibility = PlanVisibility.EVERYONE,
            friendsSelection = emptyList(),
            onValidationError = { errorReceived = it },
            onSuccess = {}
        )

        assertEquals(errorMessage, errorReceived)
    }

    @Test
    fun `updatePlan calls repository and triggers onSuccess`() = runTest {
        coEvery { planRepository.updatePlan(any()) } returns Result.success(Unit)
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        var successCalled = false

        val friendsSelection = listOf("friend3")
        viewModel.updatePlan(
            planId = "plan123",
            startDate = "2026-06-01",
            endDate = "2026-06-01",
            startTime = "10:00",
            endTime = "11:00",
            note = "Updated Note",
            visibility = PlanVisibility.EXCEPT,
            friendsSelection = friendsSelection,
            onError = {},
            onSuccess = { successCalled = true }
        )

        coVerify { 
            planRepository.updatePlan(match { 
                it.id == "plan123" && 
                it.note == "Updated Note" &&
                it.visibility == PlanVisibility.EXCEPT &&
                it.friendsSelection == friendsSelection
            }) 
        }
        assertTrue(successCalled)
    }

    @Test
    fun `updatePlan triggers onError on failure`() = runTest {
        val errorMessage = "Update failed"
        coEvery { planRepository.updatePlan(any()) } returns Result.failure(Exception(errorMessage))
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        var errorReceived = ""

        viewModel.updatePlan(
            planId = "plan123",
            startDate = "2026-06-01",
            endDate = "2026-06-01",
            startTime = "10:00",
            endTime = "11:00",
            note = "Updated Note",
            visibility = PlanVisibility.EVERYONE,
            friendsSelection = emptyList(),
            onError = { errorReceived = it },
            onSuccess = {}
        )

        assertEquals(errorMessage, errorReceived)
    }

    @Test
    fun `deletePlan calls repository and triggers onSuccess`() = runTest {
        coEvery { planRepository.deletePlan(any()) } returns Result.success(Unit)
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        var successCalled = false

        viewModel.deletePlan(
            planId = "plan123",
            onError = {},
            onSuccess = { successCalled = true }
        )

        coVerify { planRepository.deletePlan("plan123") }
        assertTrue(successCalled)
    }

    @Test
    fun `selectDate updates selectedDateMillis correctly`() = runTest {
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        viewModel.displayedYear = 2026
        viewModel.displayedMonth = Calendar.JUNE

        viewModel.selectDate(15)

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = viewModel.selectedDateMillis!!
        }
        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, calendar.get(Calendar.MONTH))
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `goToToday resets view to current date`() = runTest {
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        // Move to some other date first
        viewModel.displayedYear = 2000
        viewModel.displayedMonth = Calendar.JANUARY
        
        val now = Calendar.getInstance()
        
        viewModel.goToToday()
        
        assertEquals(now.get(Calendar.YEAR), viewModel.displayedYear)
        assertEquals(now.get(Calendar.MONTH), viewModel.displayedMonth)
        
        val selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = viewModel.selectedDateMillis!!
        }
        assertEquals(now.get(Calendar.YEAR), selectedCalendar.get(Calendar.YEAR))
        assertEquals(now.get(Calendar.MONTH), selectedCalendar.get(Calendar.MONTH))
        assertEquals(now.get(Calendar.DAY_OF_MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `filteredPlans returns empty list when no date is selected`() = runTest {
        plansFlow.value = listOf(FuturePlan(startDate = "2026-05-10", endDate = "2026-05-10"))
        viewModel = CalendarViewModel(planRepository, userRepository, socialRepository, currentUserId = "testUser")
        viewModel.selectedDateMillis = null

        assertTrue(viewModel.filteredPlans.isEmpty())
    }
}

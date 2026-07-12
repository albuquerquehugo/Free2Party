package com.free2party.ui.screens.profile

import com.free2party.data.model.FuturePlan
import com.free2party.data.model.Gender
import com.free2party.data.model.Membership
import com.free2party.data.model.User
import com.free2party.data.repository.AuthRepository
import com.free2party.data.repository.PlanRepository
import com.free2party.data.repository.UserRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val planRepository: PlanRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()
    private val userFlow = MutableStateFlow(
        User(
            uid = "me",
            firstName = "John",
            lastName = "Doe",
            gender = Gender.MAN,
            profilePicUrl = "https://example.com/john.jpg",
            isFreeNow = true,
            isStatusFromPlan = false,
            membership = Membership.FREE
        )
    )
    private val plansFlow = MutableStateFlow<List<FuturePlan>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { userRepository.currentUserId } returns "me"
        every { userRepository.observeUser("me") } returns userFlow
        every { planRepository.getOwnPlans() } returns plansFlow
        mockkStatic("com.free2party.util.UtilKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `init observes user data and sets success state`() = runTest {
        viewModel = ProfileViewModel(userRepository, planRepository, authRepository)
        runCurrent()

        assertTrue(viewModel.uiState is ProfileUiState.Success)
        val state = viewModel.uiState as ProfileUiState.Success
        assertEquals("John", state.userName)
        assertEquals("John Doe", state.userFullName)
        assertEquals(Gender.MAN, state.userGender)
        assertEquals("https://example.com/john.jpg", state.profilePicUrl)
        assertEquals(true, state.isUserFree)
        assertEquals(false, state.isStatusFromPlan)
        assertEquals(Membership.FREE, state.membership)
    }

    @Test
    fun `isUserFree is true when user has active plans and isStatusFromPlan is true`() = runTest {
        val plan = FuturePlan(id = "plan1")
        plansFlow.value = listOf(plan)
        userFlow.value = userFlow.value.copy(
            isFreeNow = false,
            isStatusFromPlan = true
        )
        // Mock isPlanActive to return true
        every { com.free2party.util.isPlanActive(any(), any()) } returns true

        viewModel = ProfileViewModel(userRepository, planRepository, authRepository)
        runCurrent()

        assertTrue(viewModel.uiState is ProfileUiState.Success)
        val state = viewModel.uiState as ProfileUiState.Success
        assertTrue(state.isUserFree)
        assertTrue(state.isStatusFromPlan)
    }

    @Test
    fun `isUserFree is false when user has inactive plans and isStatusFromPlan is true`() =
        runTest {
            val plan = FuturePlan(id = "plan1")
            plansFlow.value = listOf(plan)
            userFlow.value = userFlow.value.copy(
                isFreeNow = false,
                isStatusFromPlan = true
            )
            // Mock isPlanActive to return false
            every { com.free2party.util.isPlanActive(any(), any()) } returns false

            viewModel = ProfileViewModel(userRepository, planRepository, authRepository)
            runCurrent()


            assertTrue(viewModel.uiState is ProfileUiState.Success)
            val state = viewModel.uiState as ProfileUiState.Success
            assertFalse(state.isUserFree)
            assertFalse(state.isStatusFromPlan)
        }

    @Test
    fun `logout signs out from auth repository and triggers callback and emits event`() = runTest {
        viewModel = ProfileViewModel(userRepository, planRepository, authRepository)
        runCurrent()

        var callbackInvoked = false
        val events = mutableListOf<ProfileUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.logout {
            callbackInvoked = true
        }
        runCurrent()

        coVerify { authRepository.logout() }
        assertTrue(callbackInvoked)
        assertTrue(events.contains(ProfileUiEvent.Logout))
    }
}

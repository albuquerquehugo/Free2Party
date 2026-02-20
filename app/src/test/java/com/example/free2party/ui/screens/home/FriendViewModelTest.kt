package com.example.free2party.ui.screens.home

import com.example.free2party.data.repository.SocialRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendViewModelTest {

    private lateinit var viewModel: FriendViewModel
    private val socialRepository: SocialRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FriendViewModel(socialRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `inviteFriend with invalid email sets error state`() {
        viewModel.inviteFriend("invalid-email")

        assertTrue(viewModel.uiState is InviteFriendUiState.Error)
        assertEquals(
            "Please enter a valid email address.",
            (viewModel.uiState as InviteFriendUiState.Error).message
        )
    }

    @Test
    fun `inviteFriend success updates state and emits event`() = runTest {
        val email = "friend@example.com"
        coEvery { socialRepository.sendFriendRequest(email) } returns Result.success(Unit)

        val events = mutableListOf<FriendUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.inviteFriend(email)
        runCurrent()

        assertEquals(InviteFriendUiState.Success, viewModel.uiState)
        val event = events.firstOrNull()
        assertTrue(event is FriendUiEvent.InviteSentSuccessfully)
        assertEquals(email, (event as FriendUiEvent.InviteSentSuccessfully).email)
    }

    @Test
    fun `inviteFriend failure updates error state`() = runTest {
        val email = "friend@example.com"
        val errorMessage = "User not found"
        coEvery { socialRepository.sendFriendRequest(email) } returns Result.failure(
            Exception(
                errorMessage
            )
        )

        viewModel.inviteFriend(email)
        runCurrent()

        assertTrue(viewModel.uiState is InviteFriendUiState.Error)
        assertEquals(errorMessage, (viewModel.uiState as InviteFriendUiState.Error).message)
    }

    @Test
    fun `resetState sets state to Idle`() {
        viewModel.inviteFriend("invalid")
        assertTrue(viewModel.uiState is InviteFriendUiState.Error)

        viewModel.resetState()
        assertEquals(InviteFriendUiState.Idle, viewModel.uiState)
    }
}

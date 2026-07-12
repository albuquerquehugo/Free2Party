package com.free2party.ui.screens.home

import com.free2party.data.repository.SocialRepository
import com.free2party.ui.screens.friends.FriendUiEvent
import com.free2party.ui.screens.friends.FriendViewModel
import com.free2party.ui.screens.friends.AddFriendUiState
import com.free2party.util.UiText
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
    fun `addFriend with invalid email sets error state`() {
        viewModel.addFriend("invalid-email")

        assertTrue(viewModel.uiState is AddFriendUiState.Error)
        val state = viewModel.uiState as AddFriendUiState.Error
        assertTrue(state.message is UiText.StringResource)
    }

    @Test
    fun `addFriend success updates state and emits event`() = runTest {
        val email = "friend@example.com"
        coEvery { socialRepository.sendFriendRequest(email) } returns Result.success(Unit)

        val events = mutableListOf<FriendUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.addFriend(email)
        runCurrent()

        assertEquals(AddFriendUiState.Success, viewModel.uiState)
        val event = events.firstOrNull()
        assertTrue(event is FriendUiEvent.FriendRequestSentSuccessfully)
    }

    @Test
    fun `addFriend failure updates error state`() = runTest {
        val email = "friend@example.com"
        coEvery { socialRepository.sendFriendRequest(email) } returns Result.failure(
            Exception("Error")
        )

        viewModel.addFriend(email)
        runCurrent()

        assertTrue(viewModel.uiState is AddFriendUiState.Error)
        val state = viewModel.uiState as AddFriendUiState.Error
        assertTrue(state.message is UiText.StringResource)
    }

    @Test
    fun `resetState sets state to Idle`() {
        viewModel.addFriend("invalid")
        assertTrue(viewModel.uiState is AddFriendUiState.Error)

        viewModel.resetState()
        assertEquals(AddFriendUiState.Idle, viewModel.uiState)
    }
}

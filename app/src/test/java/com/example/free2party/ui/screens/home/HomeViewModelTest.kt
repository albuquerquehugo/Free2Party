package com.example.free2party.ui.screens.home

import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.data.model.User
import com.example.free2party.data.model.UserSettings
import com.example.free2party.data.repository.AuthRepository
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import android.util.Log
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val socialRepository: SocialRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val userFlow = MutableStateFlow(
        User(
            uid = "me",
            firstName = "John",
            lastName = "Doe",
            settings = UserSettings(use24HourFormat = true)
        )
    )
    private val friendsFlow = MutableStateFlow<List<FriendInfo>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { userRepository.currentUserId } returns "me"
        every { userRepository.observeUser("me") } returns userFlow
        every { socialRepository.getFriendsList() } returns friendsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init observes user and friends correctly`() = runTest {
        val friends = listOf(
            FriendInfo(uid = "1", name = "Alice", isFreeNow = true),
            FriendInfo(uid = "2", name = "Bob", isFreeNow = false)
        )
        friendsFlow.value = friends

        viewModel = HomeViewModel(userRepository, socialRepository, authRepository)
        runCurrent()

        assertTrue(viewModel.uiState is HomeUiState.Success)
        val state = viewModel.uiState as HomeUiState.Success
        assertEquals("John Doe", state.userName)
        assertEquals(2, state.friendsList.size)
        assertEquals("Alice", state.friendsList[0].name)
        assertEquals(true, state.use24HourFormat)
    }

    @Test
    fun `friends are sorted correctly (invited last, then by availability, then alphabetical)`() =
        runTest {
            val friends = listOf(
                FriendInfo(uid = "1", name = "Zoe", isFreeNow = false),
                FriendInfo(uid = "2", name = "Alice", isFreeNow = false),
                FriendInfo(
                    uid = "3",
                    name = "Charlie",
                    isFreeNow = true,
                    inviteStatus = InviteStatus.INVITED
                ),
                FriendInfo(uid = "4", name = "Bob", isFreeNow = true)
            )
            friendsFlow.value = friends

            viewModel = HomeViewModel(userRepository, socialRepository, authRepository)
            runCurrent()

            val state = viewModel.uiState as HomeUiState.Success
            // Bob (Free, Accepted) -> Alice (Not Free, Accepted) -> Zoe (Not Free, Accepted) -> Charlie (Invited)
            assertEquals("Bob", state.friendsList[0].name)
            assertEquals("Alice", state.friendsList[1].name)
            assertEquals("Zoe", state.friendsList[2].name)
            assertEquals("Charlie", state.friendsList[3].name)
        }

    @Test
    fun `logout calls authRepository and triggers callback`() {
        viewModel = HomeViewModel(userRepository, socialRepository, authRepository)
        var callbackCalled = false

        viewModel.logout { callbackCalled = true }

        verify { authRepository.logout() }
        assertTrue(callbackCalled)
    }

    @Test
    fun `toggleAvailability success updates action loading`() = runTest {
        coEvery { userRepository.toggleAvailability(any()) } returns Result.success(Unit)
        viewModel = HomeViewModel(userRepository, socialRepository, authRepository)
        runCurrent()

        viewModel.toggleAvailability()
        runCurrent()

        coVerify { userRepository.toggleAvailability(true) }
    }

    @Test
    fun `removeFriend success emits toast event`() = runTest {
        coEvery { socialRepository.removeFriend(any()) } returns Result.success(Unit)
        viewModel = HomeViewModel(userRepository, socialRepository, authRepository)
        runCurrent()

        val events = mutableListOf<HomeUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.removeFriend("friend123")
        runCurrent()

        val event = events.firstOrNull()
        assertTrue("Expected ShowToast but got $event", event is HomeUiEvent.ShowToast)
        assertEquals("Friend removed successfully", (event as HomeUiEvent.ShowToast).message)
    }

    @Test
    fun `cancelFriendInvite success emits toast event`() = runTest {
        coEvery { socialRepository.cancelFriendRequest(any()) } returns Result.success(Unit)
        viewModel = HomeViewModel(userRepository, socialRepository, authRepository)
        runCurrent()

        val events = mutableListOf<HomeUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.cancelFriendInvite("friend123")
        runCurrent()

        val event = events.firstOrNull()
        assertTrue("Expected ShowToast but got $event", event is HomeUiEvent.ShowToast)
        assertEquals("Invite cancelled successfully", (event as HomeUiEvent.ShowToast).message)
    }
}

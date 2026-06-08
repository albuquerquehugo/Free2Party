package com.free2party.ui.screens.events

import com.free2party.data.model.Event
import com.free2party.data.model.Membership
import com.free2party.data.model.User
import com.free2party.data.model.UserSettings
import com.free2party.data.repository.EventRepository
import com.free2party.data.repository.SocialRepository
import com.free2party.data.repository.UserRepository
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
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    private lateinit var viewModel: EventsViewModel
    private val eventRepository: EventRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val socialRepository: SocialRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val userIdFlow = MutableStateFlow("")
    private val eventsFlow = MutableStateFlow<List<Event>>(emptyList())
    private val userObserveFlow = MutableStateFlow(
        User(uid = "testUser", membership = Membership.REGULAR, settings = UserSettings())
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { userRepository.userIdFlow } returns userIdFlow
        every { userRepository.currentUserId } returns "testUser"
        every { userRepository.observeUser(any()) } returns userObserveFlow
        every { eventRepository.getEvents() } returns eventsFlow
        every { socialRepository.getCircles() } returns flowOf(emptyList())
        every { socialRepository.getFriendsList() } returns flowOf(emptyList())

        userIdFlow.value = ""
        eventsFlow.value = emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState is Error when user is not authenticated`() = runTest {
        userIdFlow.value = ""
        viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
        val job = launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        
        assertTrue(viewModel.uiState.value is EventsUiState.Error)
        job.cancel()
    }

    @Test
    fun `uiState switches to Success when user becomes authenticated`() = runTest {
        userIdFlow.value = ""
        viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
        val job = launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        
        assertTrue(viewModel.uiState.value is EventsUiState.Error)

        userIdFlow.value = "testUser"
        eventsFlow.value = listOf(
            Event(id = "1", hostId = "testUser", title = "My Event"),
            Event(id = "2", hostId = "otherUser", title = "Invited Event")
        )

        val successState = viewModel.uiState.value as? EventsUiState.Success
        assertTrue(successState != null)
        assertTrue(successState?.myEvents?.size == 1)
        assertTrue(successState?.invitedEvents?.size == 1)
        
        job.cancel()
    }
}

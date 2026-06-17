package com.free2party.ui.screens.events

import com.free2party.data.model.Event
import com.free2party.data.model.EventType
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
            Event(
                id = "2",
                hostId = "otherUser",
                guestIds = listOf("testUser"),
                title = "Pending Event"
            )
        )

        val successState = viewModel.uiState.value as? EventsUiState.Success
        assertTrue(successState != null)
        assertTrue(successState?.myEvents?.size == 1)
        assertTrue(successState?.pendingEvents?.size == 1)

        job.cancel()
    }

    @Test
    fun `uiState Success filters out public events when user is not invited`() = runTest {
        userIdFlow.value = "testUser"
        viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
        val job = launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        eventsFlow.value = listOf(
            Event(id = "1", hostId = "testUser", title = "My Event"),
            Event(
                id = "2",
                hostId = "otherUser",
                guestIds = listOf("testUser"),
                title = "Invited Event"
            ),
            Event(
                id = "3",
                hostId = "otherUser",
                guestIds = emptyList(),
                title = "Public Event - Uninvited"
            )
        )

        val successState = viewModel.uiState.value as? EventsUiState.Success
        assertTrue(successState != null)
        assertTrue(successState?.myEvents?.size == 1)
        assertTrue(successState?.pendingEvents?.size == 1)
        assertTrue(successState?.pendingEvents?.first()?.id == "2")

        job.cancel()
    }

    @Test
    fun `uiState Success filters out public events when user is not invited even if they responded`() =
        runTest {
            userIdFlow.value = "testUser"
            viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
            val job = launch(testDispatcher) {
                viewModel.uiState.collect {}
            }

            eventsFlow.value = listOf(
                Event(
                    id = "1",
                    hostId = "otherUser",
                    type = EventType.PUBLIC,
                    guestIds = listOf("testUser"),
                    invitedGuestIds = emptyList(),
                    guests = mapOf("testUser" to "DECLINED"),
                    title = "Public Event - Declined & Uninvited"
                ),
                Event(
                    id = "2",
                    hostId = "otherUser",
                    type = EventType.PUBLIC,
                    guestIds = listOf("testUser"),
                    invitedGuestIds = emptyList(),
                    guests = mapOf("testUser" to "ACCEPTED"),
                    title = "Public Event - Accepted & Uninvited"
                )
            )

            val successState = viewModel.uiState.value as? EventsUiState.Success
            assertTrue(successState != null)
            assertTrue(successState?.pendingEvents?.isEmpty() == true)

            job.cancel()
        }

    @Test
    fun `selectedTabIndex defaults to 0 and can be modified`() = runTest {
        viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
        assertTrue(viewModel.selectedTabIndex == 0)
        viewModel.selectedTabIndex = 1
        assertTrue(viewModel.selectedTabIndex == 1)
    }

    @Test
    fun `pendingInvitationsCount correctly counts pending event invitations`() = runTest {
        userIdFlow.value = ""
        viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
        val job = launch(testDispatcher) {
            viewModel.pendingInvitationsCount.collect {}
        }

        assertTrue(viewModel.pendingInvitationsCount.value == 0)

        userIdFlow.value = "testUser"
        eventsFlow.value = listOf(
            Event(
                id = "1",
                hostId = "testUser",
                guestIds = listOf("otherUser"),
                guests = mapOf("otherUser" to "PENDING")
            ),
            Event(
                id = "2",
                hostId = "otherUser",
                guestIds = listOf("testUser"),
                guests = mapOf("testUser" to "PENDING")
            ),
            Event(
                id = "3",
                hostId = "otherUser",
                guestIds = listOf("testUser"),
                guests = mapOf("testUser" to "ACCEPTED")
            ),
            Event(
                id = "4",
                hostId = "otherUser",
                guestIds = listOf("testUser"),
                guests = mapOf("testUser" to "DECLINED")
            ),
            Event(
                id = "5",
                hostId = "otherUser",
                guestIds = listOf("testUser"),
                guests = emptyMap()
            )
        )

        assertTrue(viewModel.pendingInvitationsCount.value == 2)

        job.cancel()
    }

    @Test
    fun `publicEvents are filtered by search query matching title, description or locationName`() =
        runTest {
            userIdFlow.value = "testUser"
            viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
            val job = launch(testDispatcher) {
                viewModel.uiState.collect {}
            }

            eventsFlow.value = listOf(
                Event(
                    id = "1",
                    hostId = "otherUser",
                    type = EventType.PUBLIC,
                    title = "Tech Party",
                    description = "Fun tech party",
                    locationName = "San Francisco"
                ),
                Event(
                    id = "2",
                    hostId = "otherUser",
                    type = EventType.PUBLIC,
                    title = "Dance Night",
                    description = "Dance all night",
                    locationName = "New York"
                ),
                Event(
                    id = "3",
                    hostId = "otherUser",
                    type = EventType.PUBLIC,
                    title = "Study Session",
                    description = "Focus group",
                    locationName = "Library"
                )
            )

            // Initial check: all 3 should show up
            var successState = viewModel.uiState.value as? EventsUiState.Success
            assertTrue(successState?.publicEvents?.size == 3)

            // Filter by title "Tech"
            viewModel.setSearchQuery("tech")
            successState = viewModel.uiState.value as? EventsUiState.Success
            assertTrue(successState?.publicEvents?.size == 1)
            assertTrue(successState?.publicEvents?.first()?.id == "1")

            // Filter by description "night"
            viewModel.setSearchQuery("night")
            successState = viewModel.uiState.value as? EventsUiState.Success
            assertTrue(successState?.publicEvents?.size == 1)
            assertTrue(successState?.publicEvents?.first()?.id == "2")

            // Filter by location "Library"
            viewModel.setSearchQuery("Library")
            successState = viewModel.uiState.value as? EventsUiState.Success
            assertTrue(successState?.publicEvents?.size == 1)
            assertTrue(successState?.publicEvents?.first()?.id == "3")

            job.cancel()
        }

    @Test
    fun `publicEvents are sorted by distance from userLocation when location is available`() =
        runTest {
            userIdFlow.value = "testUser"
            viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
            val job = launch(testDispatcher) {
                viewModel.uiState.collect {}
            }

            // Event locations:
            // E1: 39.0, -8.0
            // E2: 40.0, -8.0
            // E3: no location coordinates
            eventsFlow.value = listOf(
                Event(
                    id = "1",
                    hostId = "otherUser",
                    type = EventType.PUBLIC,
                    latitude = 39.0,
                    longitude = -8.0,
                    title = "Close Event"
                ),
                Event(
                    id = "2",
                    hostId = "otherUser",
                    type = EventType.PUBLIC,
                    latitude = 40.0,
                    longitude = -8.0,
                    title = "Far Event"
                ),
                Event(
                    id = "3",
                    hostId = "otherUser",
                    type = EventType.PUBLIC,
                    latitude = null,
                    longitude = null,
                    title = "Unknown Location Event"
                )
            )

            // User location is set closer to Event 1 (say 38.9, -8.0)
            viewModel.setUserLocation(UserLocation(38.9, -8.0))
            var successState = viewModel.uiState.value as? EventsUiState.Success
            assertTrue(successState?.publicEvents?.size == 3)
            // E1 is closest, then E2, then E3
            assertTrue(successState?.publicEvents?.get(0)?.id == "1")
            assertTrue(successState?.publicEvents?.get(1)?.id == "2")
            assertTrue(successState?.publicEvents?.get(2)?.id == "3")

            // Now change user location closer to Event 2 (say 40.1, -8.0)
            viewModel.setUserLocation(UserLocation(40.1, -8.0))
            successState = viewModel.uiState.value as? EventsUiState.Success
            assertTrue(successState?.publicEvents?.get(0)?.id == "2")
            assertTrue(successState?.publicEvents?.get(1)?.id == "1")
            assertTrue(successState?.publicEvents?.get(2)?.id == "3")

            job.cancel()
        }

    @Test
    fun `events are filtered by showOnlyAttending when toggled`() = runTest {
        userIdFlow.value = "testUser"
        viewModel = EventsViewModel(eventRepository, userRepository, socialRepository)
        val job = launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        eventsFlow.value = listOf(
            Event(
                id = "1",
                hostId = "testUser",
                title = "My Event"
            ),
            Event(
                id = "2",
                hostId = "otherUser",
                guestIds = listOf("testUser"),
                invitedGuestIds = listOf("testUser"),
                guests = mapOf("testUser" to "PENDING"),
                title = "Invited Event - Pending"
            ),
            Event(
                id = "3",
                hostId = "otherUser",
                guestIds = listOf("testUser"),
                invitedGuestIds = listOf("testUser"),
                guests = mapOf("testUser" to "ACCEPTED"),
                title = "Invited Event - Accepted"
            ),
            Event(
                id = "4",
                hostId = "otherUser",
                type = EventType.PUBLIC,
                guestIds = listOf("testUser"),
                invitedGuestIds = emptyList(),
                guests = mapOf("testUser" to "ACCEPTED"),
                title = "Public Event - Attending"
            ),
            Event(
                id = "5",
                hostId = "otherUser",
                type = EventType.PUBLIC,
                guestIds = emptyList(),
                title = "Public Event - Unattended"
            )
        )

        // 1. Initially (filter is off):
        // My Events should have 1 event (id 1)
        // Invited Events should have 2 events (id 2, 3)
        // Public Events should have 2 events (id 4, 5)
        var successState = viewModel.uiState.value as? EventsUiState.Success
        assertTrue(successState != null)
        assertTrue(successState?.myEvents?.size == 1)
        assertTrue(successState?.pendingEvents?.size == 2)
        assertTrue(successState?.publicEvents?.size == 2)

        // 2. Toggle showOnlyAttending to true:
        // My Events should still have 1 event (id 1 - host is always attending)
        // Invited Events should have 1 event (id 3 - accepted only)
        // Public Events should have 1 event (id 4 - accepted/attending only)
        viewModel.toggleShowOnlyAttending()
        successState = viewModel.uiState.value as? EventsUiState.Success
        assertTrue(successState?.myEvents?.size == 1)
        assertTrue(successState?.pendingEvents?.size == 1)
        assertTrue(successState?.pendingEvents?.first()?.id == "3")
        assertTrue(successState?.publicEvents?.size == 1)
        assertTrue(successState?.publicEvents?.first()?.id == "4")

        job.cancel()
    }
}

package com.example.free2party.ui.screens.notifications

import android.util.Log
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.model.Notification
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.UserRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private lateinit var viewModel: NotificationsViewModel
    private val socialRepository: SocialRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val requestsFlow = MutableStateFlow<List<FriendRequest>>(emptyList())
    private val notificationsFlow = MutableStateFlow<List<Notification>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        
        every { socialRepository.getIncomingFriendRequests() } returns requestsFlow
        every { socialRepository.getNotifications() } returns notificationsFlow
        every { userRepository.userIdFlow } returns flowOf("test-uid")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init starts listening to data`() = runTest {
        val requests = listOf(FriendRequest(id = "1", senderName = "Alice"))
        val notifications = listOf(Notification(id = "n1", isRead = false))
        
        requestsFlow.value = requests
        notificationsFlow.value = notifications
        
        viewModel = NotificationsViewModel(socialRepository, userRepository)
        
        // Start collecting flows to activate them (required for WhileSubscribed)
        backgroundScope.launch { viewModel.notificationsUnreadCount.collect {} }
        backgroundScope.launch { viewModel.itemsUnreadCount.collect {} }
        runCurrent()

        assertEquals(1, viewModel.friendRequests.value.size)
        assertEquals(1, viewModel.notificationsUnreadCount.value)
        assertEquals(2, viewModel.itemsUnreadCount.value) // 1 request + 1 unread notif
    }

    @Test
    fun `notificationsUnreadCount only counts notifications`() = runTest {
        requestsFlow.value = listOf(FriendRequest(id = "1"))
        notificationsFlow.value = listOf(
            Notification(id = "n1", isRead = false),
            Notification(id = "n2", isRead = true),
            Notification(id = "n3", isRead = false)
        )
        
        viewModel = NotificationsViewModel(socialRepository, userRepository)
        
        backgroundScope.launch { viewModel.notificationsUnreadCount.collect {} }
        backgroundScope.launch { viewModel.itemsUnreadCount.collect {} }
        runCurrent()

        assertEquals(2, viewModel.notificationsUnreadCount.value)
        assertEquals(3, viewModel.itemsUnreadCount.value) // 1 request + 2 unread notifs
    }

    @Test
    fun `markAllAsRead calls repository with unread IDs only`() = runTest {
        notificationsFlow.value = listOf(
            Notification(id = "n1", isRead = false),
            Notification(id = "n2", isRead = true),
            Notification(id = "n3", isRead = false)
        )
        viewModel = NotificationsViewModel(socialRepository, userRepository)
        runCurrent()

        viewModel.markAllAsRead()
        runCurrent()

        coVerify { socialRepository.markNotificationsAsRead(listOf("n1", "n3")) }
    }

    @Test
    fun `toggleReadStatus calls correct repository method`() = runTest {
        viewModel = NotificationsViewModel(socialRepository, userRepository)
        val unreadNotif = Notification(id = "n1", isRead = false)
        val readNotif = Notification(id = "n2", isRead = true)

        viewModel.toggleReadStatus(unreadNotif)
        runCurrent()
        coVerify { socialRepository.markNotificationAsRead("n1") }

        viewModel.toggleReadStatus(readNotif)
        runCurrent()
        coVerify { socialRepository.markNotificationAsUnread("n2") }
    }

    @Test
    fun `deleteNotification calls repository`() = runTest {
        viewModel = NotificationsViewModel(socialRepository, userRepository)
        viewModel.deleteNotification("n123")
        runCurrent()

        coVerify { socialRepository.deleteNotification("n123") }
    }

    @Test
    fun `acceptFriendRequest calls repository`() = runTest {
        val request = FriendRequest(id = "req123")
        viewModel = NotificationsViewModel(socialRepository, userRepository)
        
        viewModel.acceptFriendRequest(request)
        runCurrent()

        coVerify { socialRepository.updateFriendRequestStatus("req123", FriendRequestStatus.ACCEPTED) }
    }

    @Test
    fun `declineFriendRequest calls repository`() = runTest {
        viewModel = NotificationsViewModel(socialRepository, userRepository)
        
        viewModel.declineFriendRequest("req456")
        runCurrent()

        coVerify { socialRepository.updateFriendRequestStatus("req456", FriendRequestStatus.DECLINED) }
    }
}

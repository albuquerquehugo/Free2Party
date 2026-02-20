package com.example.free2party.ui.screens.notifications

import android.util.Log
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.repository.SocialRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val testDispatcher = UnconfinedTestDispatcher()
    private val requestsFlow = MutableStateFlow<List<FriendRequest>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        
        every { socialRepository.getIncomingFriendRequests() } returns requestsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init starts listening to friend requests`() = runTest {
        val requests = listOf(
            FriendRequest(id = "1", senderName = "Alice"),
            FriendRequest(id = "2", senderName = "Bob")
        )
        requestsFlow.value = requests
        
        viewModel = NotificationsViewModel(socialRepository)
        runCurrent()

        assertEquals(2, viewModel.friendRequests.value.size)
        assertEquals("Alice", viewModel.friendRequests.value[0].senderName)
    }

    @Test
    fun `acceptFriendRequest calls repository with correct parameters`() = runTest {
        val request = FriendRequest(id = "req123")
        viewModel = NotificationsViewModel(socialRepository)
        
        viewModel.acceptFriendRequest(request)
        runCurrent()

        coVerify { socialRepository.updateFriendRequestStatus("req123", FriendRequestStatus.ACCEPTED) }
    }

    @Test
    fun `declineFriendRequest calls repository with correct parameters`() = runTest {
        viewModel = NotificationsViewModel(socialRepository)
        
        viewModel.declineFriendRequest("req456")
        runCurrent()

        coVerify { socialRepository.updateFriendRequestStatus("req456", FriendRequestStatus.DECLINED) }
    }
}

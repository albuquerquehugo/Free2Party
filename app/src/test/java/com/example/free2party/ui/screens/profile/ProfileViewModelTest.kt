package com.example.free2party.ui.screens.profile

import android.net.Uri
import com.example.free2party.data.model.User
import com.example.free2party.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val userFlow = MutableStateFlow(User(uid = "me", firstName = "John", lastName = "Doe"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { userRepository.currentUserId } returns "me"
        every { userRepository.observeUser("me") } returns userFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads user profile`() = runTest {
        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        assertTrue(viewModel.uiState is ProfileUiState.Success)
        assertEquals("John Doe", (viewModel.uiState as ProfileUiState.Success).user.fullName)
    }

    @Test
    fun `updateProfile success updates state and emits toast`() = runTest {
        val updatedUser = User(uid = "me", firstName = "Jane", lastName = "Doe")
        coEvery { userRepository.updateUser(updatedUser) } returns Result.success(Unit)
        
        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        val events = mutableListOf<ProfileUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.updateProfile(updatedUser)
        runCurrent()

        val state = viewModel.uiState as ProfileUiState.Success
        assertEquals(false, state.isSaving)
        val event = events.firstOrNull()
        assertTrue(event is ProfileUiEvent.ShowToast)
        assertEquals("Profile updated successfully!", (event as ProfileUiEvent.ShowToast).message)
    }

    @Test
    fun `uploadProfilePicture success updates user and state`() = runTest {
        val uri = mockk<Uri>()
        val downloadUrl = "https://example.com/pic.jpg"
        coEvery { userRepository.uploadProfilePicture(uri) } returns Result.success(downloadUrl)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)

        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        val events = mutableListOf<ProfileUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.uploadProfilePicture(uri)
        runCurrent()

        coVerify { userRepository.updateUser(match { it.profilePicUrl == downloadUrl }) }
        val state = viewModel.uiState as ProfileUiState.Success
        assertEquals(false, state.isUploadingImage)
        assertTrue(events.any { it is ProfileUiEvent.ShowToast && it.message == "Profile picture updated!" })
    }
}

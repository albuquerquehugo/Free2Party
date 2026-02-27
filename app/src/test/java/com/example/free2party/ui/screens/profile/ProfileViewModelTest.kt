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
import org.junit.Assert.assertFalse
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
    fun `init loads user profile and initializes fields`() = runTest {
        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        assertTrue(viewModel.uiState is ProfileUiState.Success)
        assertEquals("John", viewModel.firstName)
        assertEquals("Doe", viewModel.lastName)
        assertFalse(viewModel.hasChanges)
        assertTrue(viewModel.isFormValid)
    }

    @Test
    fun `field changes update hasChanges and isFormValid`() = runTest {
        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        viewModel.firstName = "Jane"
        assertTrue(viewModel.hasChanges)
        assertTrue(viewModel.isFormValid)

        viewModel.firstName = ""
        assertFalse(viewModel.isFormValid)

        viewModel.firstName = "John"
        viewModel.countryCode = "US"
        viewModel.phoneNumber = "" // Invalid: country selected but phone empty
        assertFalse(viewModel.isFormValid)

        viewModel.phoneNumber = "1234567890" // Valid for US
        assertTrue(viewModel.isFormValid)

        // Invalid birthday: invalid month
        viewModel.birthday = "19901301"
        assertFalse(viewModel.isFormValid)

        // Valid birthday
        viewModel.birthday = "19900101"
        assertTrue(viewModel.isFormValid)
    }

    @Test
    fun `discardChanges resets fields to original values`() = runTest {
        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        viewModel.firstName = "Jane"
        viewModel.phoneNumber = "1234567890"
        assertTrue(viewModel.hasChanges)

        viewModel.discardChanges()

        assertEquals("John", viewModel.firstName)
        assertEquals("", viewModel.phoneNumber)
        assertFalse(viewModel.hasChanges)
    }

    @Test
    fun `updateProfile success updates state and emits toast with navigateBack true`() = runTest {
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)

        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        val events = mutableListOf<ProfileUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.firstName = "Jane"
        viewModel.updateProfile()
        runCurrent()

        val state = viewModel.uiState as ProfileUiState.Success
        assertEquals(false, state.isSaving)
        val event = events.firstOrNull()
        assertTrue(event is ProfileUiEvent.ShowToast)
        val toastEvent = event as ProfileUiEvent.ShowToast
        assertEquals("Profile updated successfully!", toastEvent.message)
        assertEquals(true, toastEvent.navigateBack)

        coVerify { userRepository.updateUser(match { it.firstName == "Jane" }) }
    }

    @Test
    fun `updateProfile failure emits error toast`() = runTest {
        val errorMessage = "Update failed"
        coEvery { userRepository.updateUser(any()) } returns Result.failure(Exception(errorMessage))

        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        val events = mutableListOf<ProfileUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.firstName = "Jane"
        viewModel.updateProfile()
        runCurrent()

        assertTrue(events.any { it is ProfileUiEvent.ShowToast && it.message == "Error: $errorMessage" })
        assertFalse((viewModel.uiState as ProfileUiState.Success).isSaving)
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
        assertTrue(events.any { it is ProfileUiEvent.ShowToast && it.message == "Profile picture updated!" && !it.navigateBack })
    }

    @Test
    fun `uiState transitions during updateProfile`() = runTest {
        viewModel = ProfileViewModel(userRepository)
        runCurrent()

        coEvery { userRepository.updateUser(any()) } coAnswers {
            assertTrue((viewModel.uiState as ProfileUiState.Success).isSaving)
            Result.success(Unit)
        }

        viewModel.firstName = "Jane"
        viewModel.updateProfile()
        runCurrent()

        assertFalse((viewModel.uiState as ProfileUiState.Success).isSaving)
    }
}

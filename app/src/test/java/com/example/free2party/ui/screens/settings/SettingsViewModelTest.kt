package com.example.free2party.ui.screens.settings

import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.model.User
import com.example.free2party.data.model.UserSettings
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val userFlow = MutableStateFlow(User(uid = "me", settings = UserSettings(use24HourFormat = true)))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { userRepository.currentUserId } returns "me"
        every { userRepository.observeUser("me") } returns userFlow
        every { settingsRepository.themeModeFlow } returns flowOf(ThemeMode.AUTOMATIC)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads user settings and observes theme mode`() = runTest {
        viewModel = SettingsViewModel(userRepository, settingsRepository)
        
        // themeMode is observed in init
        runCurrent()

        assertTrue(viewModel.uiState is SettingsUiState.Success)
        assertEquals(true, (viewModel.uiState as SettingsUiState.Success).user.settings.use24HourFormat)
        assertEquals(ThemeMode.AUTOMATIC, viewModel.themeMode)
    }

    @Test
    fun `updateThemeMode calls repository`() = runTest {
        viewModel = SettingsViewModel(userRepository, settingsRepository)
        runCurrent()

        viewModel.updateThemeMode(ThemeMode.DARK)
        runCurrent()

        coVerify { settingsRepository.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `updateSettings success updates state and emits toast`() = runTest {
        val updatedUser = User(uid = "me", settings = UserSettings(use24HourFormat = false))
        coEvery { userRepository.updateUser(updatedUser) } returns Result.success(Unit)
        
        viewModel = SettingsViewModel(userRepository, settingsRepository)
        runCurrent()

        val events = mutableListOf<SettingsUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.updateSettings(updatedUser)
        runCurrent()

        val state = viewModel.uiState as SettingsUiState.Success
        assertEquals(false, state.isSaving)
        val event = events.firstOrNull()
        assertTrue(event is SettingsUiEvent.ShowToast)
        assertEquals("Settings updated successfully!", (event as SettingsUiEvent.ShowToast).message)
        coVerify { userRepository.updateUser(updatedUser) }
    }

    @Test
    fun `updateSettings failure emits error toast`() = runTest {
        val updatedUser = User(uid = "me", settings = UserSettings(use24HourFormat = false))
        val errorMessage = "Update failed"
        coEvery { userRepository.updateUser(updatedUser) } returns Result.failure(Exception(errorMessage))
        
        viewModel = SettingsViewModel(userRepository, settingsRepository)
        runCurrent()

        val events = mutableListOf<SettingsUiEvent>()
        backgroundScope.launch {
            viewModel.uiEvent.collect { events.add(it) }
        }
        runCurrent()

        viewModel.updateSettings(updatedUser)
        runCurrent()

        val state = viewModel.uiState as SettingsUiState.Success
        assertEquals(false, state.isSaving)
        val event = events.firstOrNull()
        assertTrue(event is SettingsUiEvent.ShowToast)
        assertEquals("Error: $errorMessage", (event as SettingsUiEvent.ShowToast).message)
    }
}

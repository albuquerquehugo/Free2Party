package com.free2party.ui.screens.settings

import com.free2party.data.model.ThemeMode
import com.free2party.data.model.User
import com.free2party.data.model.UserSettings
import com.free2party.data.repository.SettingsRepository
import com.free2party.data.repository.UserRepository
import com.free2party.util.UiText
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

import com.free2party.data.repository.SocialRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val socialRepository: SocialRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val userFlow =
        MutableStateFlow(User(uid = "me", settings = UserSettings(use24HourFormat = true)))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { userRepository.currentUserId } returns "me"
        every { userRepository.observeUser("me") } returns userFlow
        every { settingsRepository.themeModeFlow } returns flowOf(ThemeMode.AUTOMATIC)
        every { socialRepository.getCircles() } returns flowOf(emptyList())
        every { socialRepository.getFriendsList() } returns flowOf(emptyList())
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads user settings and observes theme mode`() = runTest {
        viewModel = SettingsViewModel(userRepository, settingsRepository, socialRepository)

        // themeMode is observed in init
        runCurrent()

        assertTrue(viewModel.uiState is SettingsUiState.Success)
        assertEquals(
            true,
            (viewModel.uiState as SettingsUiState.Success).user.settings.use24HourFormat
        )
        assertEquals(ThemeMode.AUTOMATIC, viewModel.themeMode)
    }

    @Test
    fun `updateSettings success updates state and emits toast`() = runTest {
        val updatedUser = User(uid = "me", settings = UserSettings(use24HourFormat = false))
        coEvery { userRepository.updateUser(updatedUser) } returns Result.success(Unit)

        viewModel = SettingsViewModel(userRepository, settingsRepository, socialRepository)
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
        assertTrue((event as SettingsUiEvent.ShowToast).message is UiText.StringResource)
        coVerify { userRepository.updateUser(updatedUser) }
    }

    @Test
    fun `updateSettings failure emits error toast`() = runTest {
        val updatedUser = User(uid = "me", settings = UserSettings(use24HourFormat = false))
        val errorMessage = "Update failed"
        coEvery { userRepository.updateUser(updatedUser) } returns Result.failure(
            Exception(
                errorMessage
            )
        )

        viewModel = SettingsViewModel(userRepository, settingsRepository, socialRepository)
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
        assertTrue((event as SettingsUiEvent.ShowToast).message is UiText.StringResource)
    }

    @Test
    fun `updateSettings with birthday settings success`() = runTest {
        val updatedUser = User(
            uid = "me",
            birthdayVisibility = com.free2party.data.model.BirthdayVisibility.NOBODY,
            birthdayShowType = com.free2party.data.model.BirthdayShowType.DAY_MONTH,
            birthdayFriendsSelection = listOf("friend1")
        )
        coEvery { userRepository.updateUser(updatedUser) } answers {
            userFlow.value = updatedUser
            Result.success(Unit)
        }

        viewModel = SettingsViewModel(userRepository, settingsRepository, socialRepository)
        runCurrent()

        viewModel.updateSettings(updatedUser)
        runCurrent()

        val state = viewModel.uiState as SettingsUiState.Success
        assertEquals(
            com.free2party.data.model.BirthdayVisibility.NOBODY,
            state.user.birthdayVisibility
        )
        assertEquals(
            com.free2party.data.model.BirthdayShowType.DAY_MONTH,
            state.user.birthdayShowType
        )
        assertEquals(listOf("friend1"), state.user.birthdayFriendsSelection)
        coVerify { userRepository.updateUser(updatedUser) }
    }
}

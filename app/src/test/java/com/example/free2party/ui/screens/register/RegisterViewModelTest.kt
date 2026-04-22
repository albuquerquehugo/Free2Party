package com.example.free2party.ui.screens.register

import android.content.Context
import android.net.Uri
import com.example.free2party.R
import com.example.free2party.data.repository.AuthRepository
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.util.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class RegisterViewModelTest {

    private lateinit var viewModel: RegisterViewModel
    private val authRepository: AuthRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val context: Context = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        io.mockk.every { settingsRepository.gradientBackgroundFlow } returns flowOf(true)
        io.mockk.every { context.getString(R.string.date_pattern_yyyy_mm_dd) } returns "yyyyMMdd"
        io.mockk.every { context.getString(R.string.date_pattern_mm_dd_yyyy) } returns "MMddyyyy"
        io.mockk.every { context.getString(R.string.date_pattern_dd_mm_yyyy) } returns "ddMMyyyy"
        viewModel = RegisterViewModel(settingsRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isFormValid returns true only when all required fields and validations pass`() {
        assertFalse(viewModel.isFormValid(context))

        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "john@example.com"
        viewModel.password = "password123"
        viewModel.confirmPassword = "password123"
        // Phone and birthday are empty by default, so they are valid
        assertTrue(viewModel.isFormValid(context))

        // Invalid phone: country selected but no number
        viewModel.countryCode = "US"
        viewModel.phoneNumber = ""
        assertFalse(viewModel.isFormValid(context))

        // Valid phone: US has 10 digits
        viewModel.phoneNumber = "1234567890"
        assertTrue(viewModel.isFormValid(context))

        // Invalid phone: too short for US
        viewModel.phoneNumber = "123"
        assertFalse(viewModel.isFormValid(context))

        // Invalid phone: too long for US
        viewModel.phoneNumber = "12345678901"
        assertFalse(viewModel.isFormValid(context))

        // Valid phone for another country: PT has 9 digits
        viewModel.countryCode = "PT"
        viewModel.phoneNumber = "912345678"
        assertTrue(viewModel.isFormValid(context))

        // Invalid birthday: wrong length
        viewModel.birthday = "199001"
        assertFalse(viewModel.isFormValid(context))

        // Invalid birthday: invalid date (month 13)
        viewModel.birthday = "19901301"
        assertFalse(viewModel.isFormValid(context))

        // Valid birthday
        viewModel.birthday = "19900101"
        assertTrue(viewModel.isFormValid(context))

        viewModel.email = ""
        assertFalse(viewModel.isFormValid(context))
    }

    @Test
    fun `onRegisterClick with empty fields sets error state`() {
        viewModel.firstName = ""
        viewModel.lastName = ""
        viewModel.email = ""
        viewModel.password = ""
        viewModel.confirmPassword = ""

        viewModel.onRegisterClick(context)

        assertTrue(viewModel.uiState is RegisterUiState.Error)
        val state = viewModel.uiState as RegisterUiState.Error
        assertTrue(state.message is UiText.StringResource)
    }

    @Test
    fun `onRegisterClick with invalid email sets error state`() {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "invalid-email"
        viewModel.password = "password123"
        viewModel.confirmPassword = "password123"

        viewModel.onRegisterClick(context)

        assertTrue(viewModel.uiState is RegisterUiState.Error)
        val state = viewModel.uiState as RegisterUiState.Error
        assertTrue(state.message is UiText.StringResource)
    }

    @Test
    fun `onRegisterClick with invalid phone number sets error state`() {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "john@example.com"
        viewModel.password = "password123"
        viewModel.confirmPassword = "password123"
        viewModel.countryCode = "US"
        viewModel.phoneNumber = "123"

        viewModel.onRegisterClick(context)

        assertTrue(viewModel.uiState is RegisterUiState.Error)
        val state = viewModel.uiState as RegisterUiState.Error
        assertTrue(state.message is UiText.StringResource)
    }

    @Test
    fun `onRegisterClick with invalid birthday sets error state`() {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "john@example.com"
        viewModel.password = "password123"
        viewModel.confirmPassword = "password123"
        viewModel.birthday = "19901301"

        viewModel.onRegisterClick(context)

        assertTrue(viewModel.uiState is RegisterUiState.Error)
        val state = viewModel.uiState as RegisterUiState.Error
        assertTrue(state.message is UiText.StringResource)
    }

    @Test
    fun `onRegisterClick success updates state to success and passes all data`() = runTest {
        val firstName = "John"
        val lastName = "Doe"
        val email = "john@example.com"
        val password = "password123"
        val phone = "912345678"
        val country = "PT"
        val bday = "19900101"
        val bio = "My bio"
        val facebook = "fb_user"
        val uri = mockk<Uri>()

        viewModel.firstName = firstName
        viewModel.lastName = lastName
        viewModel.email = email
        viewModel.password = password
        viewModel.confirmPassword = password
        viewModel.profilePicUri = uri
        viewModel.phoneNumber = phone
        viewModel.countryCode = country
        viewModel.birthday = bday
        viewModel.bio = bio
        viewModel.facebookUsername = facebook

        coEvery {
            authRepository.register(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                profilePicUri = uri,
                phoneNumber = phone,
                countryCode = country,
                birthday = bday,
                bio = bio,
                socials = match { it.facebookUsername == facebook }
            )
        } returns Result.success(mockk())

        viewModel.onRegisterClick(context)
        runCurrent()

        assertEquals(RegisterUiState.Success, viewModel.uiState)
        coVerify {
            authRepository.register(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                profilePicUri = uri,
                phoneNumber = phone,
                countryCode = country,
                birthday = bday,
                bio = bio,
                socials = any()
            )
        }
    }

    @Test
    fun `onRegisterClick normalizes email before calling repository`() = runTest {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "  JOHN@Example.COM  "
        viewModel.password = "password123"
        viewModel.confirmPassword = "password123"

        coEvery {
            authRepository.register(
                firstName = any(),
                lastName = any(),
                email = any(),
                password = any(),
                profilePicUri = any(),
                phoneNumber = any(),
                countryCode = any(),
                birthday = any(),
                bio = any(),
                socials = any()
            )
        } returns Result.success(mockk())

        viewModel.onRegisterClick(context)
        runCurrent()

        coVerify {
            authRepository.register(
                firstName = "John",
                lastName = "Doe",
                email = "john@example.com",
                password = "password123",
                profilePicUri = null,
                phoneNumber = "",
                countryCode = "",
                birthday = "",
                bio = "",
                socials = any()
            )
        }
    }

    @Test
    fun `resetFields clears all data and state`() = runTest {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "john@example.com"
        viewModel.password = "password"
        viewModel.phoneNumber = "1234567890"
        viewModel.countryCode = "US"
        viewModel.birthday = "19900101"
        viewModel.bio = "Bio"
        viewModel.facebookUsername = "fb"

        viewModel.resetFields()

        assertEquals("", viewModel.firstName)
        assertEquals("", viewModel.lastName)
        assertEquals("", viewModel.email)
        assertEquals("", viewModel.password)
        assertEquals("", viewModel.phoneNumber)
        assertEquals("", viewModel.countryCode)
        assertEquals("", viewModel.birthday)
        assertEquals("", viewModel.bio)
        assertEquals("", viewModel.facebookUsername)
        assertEquals(RegisterUiState.Idle, viewModel.uiState)
    }

    @Test
    fun `uiState transitions to loading during registration`() = runTest {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "john@example.com"
        viewModel.password = "password123"
        viewModel.confirmPassword = "password123"

        coEvery {
            authRepository.register(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } coAnswers {
            assertEquals(RegisterUiState.Loading, viewModel.uiState)
            Result.success(mockk())
        }

        viewModel.onRegisterClick(context)
        runCurrent()
        assertEquals(RegisterUiState.Success, viewModel.uiState)
    }
}

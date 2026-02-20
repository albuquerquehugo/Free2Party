package com.example.free2party.ui.screens.register

import android.net.Uri
import com.example.free2party.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isFormValid returns true only when all required fields are not blank`() {
        assertFalse(viewModel.isFormValid)

        viewModel.firstName = "John"
        assertFalse(viewModel.isFormValid)

        viewModel.lastName = "Doe"
        assertFalse(viewModel.isFormValid)

        viewModel.email = "john@example.com"
        assertFalse(viewModel.isFormValid)

        viewModel.password = "password123"
        assertTrue(viewModel.isFormValid)

        viewModel.email = ""
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun `onRegisterClick with empty fields sets error state`() {
        viewModel.firstName = ""
        viewModel.lastName = ""
        viewModel.email = ""
        viewModel.password = ""

        viewModel.onRegisterClick()

        assertTrue(viewModel.uiState is RegisterUiState.Error)
        assertEquals(
            "All fields are required",
            (viewModel.uiState as RegisterUiState.Error).message
        )
    }

    @Test
    fun `onRegisterClick with invalid email sets error state`() {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "invalid-email"
        viewModel.password = "password123"

        viewModel.onRegisterClick()

        assertTrue(viewModel.uiState is RegisterUiState.Error)
        assertEquals(
            "Please enter a valid email address",
            (viewModel.uiState as RegisterUiState.Error).message
        )
    }

    @Test
    fun `onRegisterClick success updates state to success`() = runTest {
        val firstName = "John"
        val lastName = "Doe"
        val email = "john@example.com"
        val password = "password123"
        val uri = mockk<Uri>()

        viewModel.firstName = firstName
        viewModel.lastName = lastName
        viewModel.email = email
        viewModel.password = password
        viewModel.profilePicUri = uri

        coEvery {
            authRepository.register(
                firstName,
                lastName,
                email,
                password,
                uri
            )
        } returns Result.success(mockk())

        viewModel.onRegisterClick()
        runCurrent()

        assertEquals(RegisterUiState.Success, viewModel.uiState)
    }

    @Test
    fun `onRegisterClick normalizes email before calling repository`() = runTest {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "  JOHN@Example.COM  "
        viewModel.password = "password123"

        coEvery {
            authRepository.register(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Result.success(mockk())

        viewModel.onRegisterClick()
        runCurrent()

        coVerify { authRepository.register("John", "Doe", "john@example.com", "password123", null) }
    }

    @Test
    fun `onRegisterClick failure updates error state`() = runTest {
        val firstName = "John"
        val lastName = "Doe"
        val email = "john@example.com"
        val password = "password123"
        val errorMessage = "Email already in use"

        viewModel.firstName = firstName
        viewModel.lastName = lastName
        viewModel.email = email
        viewModel.password = password

        coEvery {
            authRepository.register(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Result.failure(Exception(errorMessage))

        viewModel.onRegisterClick()
        runCurrent()

        assertTrue(viewModel.uiState is RegisterUiState.Error)
        assertEquals(errorMessage, (viewModel.uiState as RegisterUiState.Error).message)
    }

    @Test
    fun `resetFields clears all data and state`() = runTest {
        viewModel.firstName = "John"
        viewModel.lastName = "Doe"
        viewModel.email = "john@example.com"
        viewModel.password = "password"
        viewModel.profilePicUri = mockk()

        coEvery {
            authRepository.register(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Result.success(mockk())
        viewModel.onRegisterClick()
        runCurrent()

        viewModel.resetFields()

        assertEquals("", viewModel.firstName)
        assertEquals("", viewModel.lastName)
        assertEquals("", viewModel.email)
        assertEquals("", viewModel.password)
        assertEquals(null, viewModel.profilePicUri)
        assertEquals(RegisterUiState.Idle, viewModel.uiState)
    }
}

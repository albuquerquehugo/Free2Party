package com.example.free2party.ui.screens.login

import com.example.free2party.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var viewModel: LoginViewModel
    private val authRepository: AuthRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isFormValid returns true only when both fields are not blank`() {
        assertFalse(viewModel.isFormValid)

        viewModel.email = "test@example.com"
        assertFalse(viewModel.isFormValid)

        viewModel.password = "password"
        assertTrue(viewModel.isFormValid)

        viewModel.email = ""
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun `onLoginClick with empty fields sets error state`() {
        viewModel.email = ""
        viewModel.password = ""

        viewModel.onLoginClick {}

        assertTrue(viewModel.uiState is LoginUiState.Error)
        assertEquals(
            "Email and password cannot be empty",
            (viewModel.uiState as LoginUiState.Error).message
        )
    }

    @Test
    fun `onLoginClick success updates state and calls onSuccess`() = runTest(testDispatcher) {
        val email = "test@example.com"
        val password = "password123"
        viewModel.email = email
        viewModel.password = password

        coEvery { authRepository.login(email, password) } returns Result.success(mockk())

        var onSuccessCalled = false
        viewModel.onLoginClick { onSuccessCalled = true }

        assertTrue(onSuccessCalled)
        assertEquals(LoginUiState.Success, viewModel.uiState)
    }

    @Test
    fun `onLoginClick normalizes email before calling repository`() = runTest(testDispatcher) {
        viewModel.email = "  USER@Example.COM  "
        viewModel.password = "password"
        coEvery { authRepository.login(any(), any()) } returns Result.success(mockk())

        viewModel.onLoginClick {}

        coVerify { authRepository.login("user@example.com", "password") }
    }

    @Test
    fun `onLoginClick failure updates error state`() = runTest(testDispatcher) {
        val email = "test@example.com"
        val password = "wrong_password"
        viewModel.email = email
        viewModel.password = password
        val errorMessage = "Invalid credentials"

        coEvery { authRepository.login(email, password) } returns Result.failure(
            Exception(
                errorMessage
            )
        )

        viewModel.onLoginClick {}

        assertTrue(viewModel.uiState is LoginUiState.Error)
        assertEquals(errorMessage, (viewModel.uiState as LoginUiState.Error).message)
    }

    @Test
    fun `onForgotPasswordConfirm with invalid email sets error state`() {
        viewModel.onForgotPasswordConfirm("invalid-email")

        assertTrue(viewModel.uiState is LoginUiState.Error)
        assertEquals(
            "Please enter a valid email address.",
            (viewModel.uiState as LoginUiState.Error).message
        )
    }

    @Test
    fun `onForgotPasswordConfirm success resets to idle and emits toast event`() =
        runTest(testDispatcher) {
            val email = "test@example.com"
            coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.success(Unit)

            val events = mutableListOf<LoginUiEvent>()
            val collectJob = launch {
                viewModel.uiEvent.collect { events.add(it) }
            }

            viewModel.onForgotPasswordConfirm(email)

            assertEquals(LoginUiState.Idle, viewModel.uiState)
            val event = events.firstOrNull()
            assertTrue("Expected ShowToast but was $event", event is LoginUiEvent.ShowToast)
            assertEquals(
                "Password reset email sent! Please check your inbox.",
                (event as LoginUiEvent.ShowToast).message
            )

            collectJob.cancel()
        }

    @Test
    fun `onForgotPasswordConfirm normalizes email`() = runTest(testDispatcher) {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.success(Unit)

        viewModel.onForgotPasswordConfirm("  RESET@Example.COM  ")

        coVerify { authRepository.sendPasswordResetEmail("reset@example.com") }
    }

    @Test
    fun `resetState sets state to Idle`() {
        viewModel.onForgotPasswordConfirm("invalid")
        assertTrue(viewModel.uiState is LoginUiState.Error)

        viewModel.resetState()
        assertEquals(LoginUiState.Idle, viewModel.uiState)
    }

    @Test
    fun `resetFields clears data and state`() {
        viewModel.email = "test@example.com"
        viewModel.password = "password"
        viewModel.resetFields()

        assertEquals("", viewModel.email)
        assertEquals("", viewModel.password)
        assertEquals(LoginUiState.Idle, viewModel.uiState)
    }
}

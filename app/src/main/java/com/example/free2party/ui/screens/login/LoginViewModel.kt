package com.example.free2party.ui.screens.login

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.R
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.repository.AuthRepository
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.exception.AuthException
import com.example.free2party.exception.EmailNotVerifiedException
import com.example.free2party.util.UiText
import com.google.firebase.auth.AuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Error(val message: UiText, val isEmailNotVerified: Boolean = false) : LoginUiState
    object Success : LoginUiState
}

sealed class LoginUiEvent {
    data class ShowToast(val message: UiText) : LoginUiEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")

    val isFormValid by derivedStateOf {
        email.isNotBlank() && password.isNotBlank()
    }

    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    var themeMode by mutableStateOf(ThemeMode.AUTOMATIC)
        private set

    var gradientBackground by mutableStateOf(true)
        private set

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeThemeMode()
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            settingsRepository.themeModeFlow.collectLatest { mode ->
                themeMode = mode
            }
        }
        viewModelScope.launch {
            settingsRepository.gradientBackgroundFlow.collectLatest { enabled ->
                gradientBackground = enabled
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun updateGradientBackground(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGradientBackground(enabled)
        }
    }

    fun onLoginClick(onSuccess: () -> Unit) {
        if (uiState is LoginUiState.Loading) return

        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            uiState = LoginUiState.Error(UiText.StringResource(R.string.error_login_empty_fields))
            return
        }

        uiState = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.login(normalizedEmail, password)
                .onSuccess {
                    uiState = LoginUiState.Success
                    onSuccess()
                }
                .onFailure { e ->
                    val isEmailNotVerified = e is EmailNotVerifiedException
                    val errorText = if (e is AuthException && e.messageRes != null) {
                        UiText.StringResource(e.messageRes)
                    } else {
                        UiText.StringResource(R.string.error_login_failed)
                    }
                    uiState = LoginUiState.Error(errorText, isEmailNotVerified)
                }
        }
    }

    fun onResendVerificationClick() {
        if (uiState is LoginUiState.Loading) return

        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            uiState =
                LoginUiState.Error(UiText.StringResource(R.string.error_resend_verification_empty_fields))
            return
        }

        uiState = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.resendVerificationEmail(normalizedEmail, password)
                .onSuccess {
                    uiState = LoginUiState.Idle
                    _uiEvent.emit(LoginUiEvent.ShowToast(UiText.StringResource(R.string.message_resend_verification_success)))
                }
                .onFailure { e ->
                    val errorText = if (e is AuthException && e.messageRes != null) {
                        UiText.StringResource(e.messageRes)
                    } else {
                        UiText.StringResource(R.string.error_resend_verification_failed)
                    }
                    uiState = LoginUiState.Error(errorText)
                }
        }
    }

    fun onGoogleSignIn(credential: AuthCredential, onSuccess: () -> Unit) {
        if (uiState is LoginUiState.Loading) return
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.signInWithGoogle(credential)
                .onSuccess {
                    uiState = LoginUiState.Success
                    onSuccess()
                }
                .onFailure { e ->
                    val errorText = if (e is AuthException && e.messageRes != null) {
                        UiText.StringResource(e.messageRes)
                    } else {
                        UiText.StringResource(R.string.error_google_failed)
                    }
                    uiState = LoginUiState.Error(errorText)
                }
        }
    }

    fun onForgotPasswordConfirm(email: String) {
        if (uiState is LoginUiState.Loading) return

        val normalizedEmail = email.trim().lowercase()
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(normalizedEmail)) {
            uiState = LoginUiState.Error(UiText.StringResource(R.string.error_invalid_email))
            return
        }

        uiState = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(normalizedEmail)
                .onSuccess {
                    uiState = LoginUiState.Idle
                    _uiEvent.emit(LoginUiEvent.ShowToast(UiText.StringResource(R.string.message_reset_password_sent)))
                }
                .onFailure { e ->
                    val errorText = if (e is AuthException && e.messageRes != null) {
                        UiText.StringResource(e.messageRes)
                    } else {
                        UiText.StringResource(R.string.error_failed_reset_password)
                    }
                    uiState = LoginUiState.Error(errorText)
                }
        }
    }

    fun resetState() {
        uiState = LoginUiState.Idle
    }

    fun resetFields() {
        email = ""
        password = ""
        uiState = LoginUiState.Idle
    }
}

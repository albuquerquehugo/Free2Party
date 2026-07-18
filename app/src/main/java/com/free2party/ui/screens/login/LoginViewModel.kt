package com.free2party.ui.screens.login

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.R
import com.free2party.data.repository.AuthRepository
import com.free2party.data.repository.SettingsRepository
import com.free2party.exception.AuthException
import com.free2party.exception.EmailNotVerifiedException
import com.free2party.util.UiText
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
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


    var useLegacyGoogleSignIn by mutableStateOf(false)
        private set

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            settingsRepository.useLegacyGoogleSignInFlow.collectLatest { enabled ->
                useLegacyGoogleSignIn = enabled
            }
        }
    }


    fun saveUseLegacyGoogleSignInPreference(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseLegacyGoogleSignIn(enabled)
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
                    _uiEvent.emit(LoginUiEvent.ShowToast(UiText.StringResource(R.string.text_resend_verification_success)))
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

    fun onGoogleSignIn(
        credential: AuthCredential,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit = {}
    ) {
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.signInWithGoogle(credential)
                .onSuccess {
                    uiState = LoginUiState.Success
                    onSuccess()
                }
                .onFailure { e ->
                    Log.e("LoginViewModel", "Google Sign-In failed", e)
                    val errorText = if (e is AuthException && e.messageRes != null) {
                        UiText.StringResource(e.messageRes)
                    } else {
                        UiText.StringResource(R.string.error_google_failed_unknown)
                    }
                    uiState = LoginUiState.Error(errorText)
                    _uiEvent.emit(LoginUiEvent.ShowToast(errorText))
                    onFailure(e)
                }
        }
    }

    fun onGoogleSignInError(e: Throwable) {
        when (e) {
            is GoogleIdTokenParsingException -> {
                Log.e("LoginViewModel", "Received an invalid google id token response", e)
                val localizedMsg = e.localizedMessage
                val errorText = if (!localizedMsg.isNullOrBlank()) {
                    UiText.Composite(
                        parts = listOf(
                            UiText.StringResource(R.string.error_google_failed_invalid_token),
                            UiText.DynamicString(localizedMsg)
                        ),
                        separator = ": "
                    )
                } else {
                    UiText.StringResource(R.string.error_google_failed_invalid_token)
                }
                uiState = LoginUiState.Error(errorText)
                viewModelScope.launch {
                    _uiEvent.emit(LoginUiEvent.ShowToast(errorText))
                }
            }

            is NoCredentialException -> {
                Log.e(
                    "LoginViewModel",
                    "No Google accounts available. Falling back to legacy Google Sign-In and saving preference.",
                    e
                )
                saveUseLegacyGoogleSignInPreference(true)
            }

            is GetCredentialException -> {
                Log.e(
                    "LoginViewModel",
                    "Google Sign-In failed. Falling back to legacy Google Sign-In and saving preference.",
                    e
                )
                saveUseLegacyGoogleSignInPreference(true)
            }

            else -> {
                Log.e(
                    "LoginViewModel",
                    "An unexpected error occurred during Google Sign-In. Falling back to legacy Google Sign-In and saving preference.",
                    e
                )
                saveUseLegacyGoogleSignInPreference(true)
            }
        }
    }

    fun onGoogleSignInUnexpectedCredentialType(type: String) {
        Log.e("LoginViewModel", "Unexpected credential type: $type")
        val errorText = UiText.Composite(
            parts = listOf(
                UiText.StringResource(R.string.error_google_failed_unexpected_response),
                UiText.DynamicString(type)
            ),
            separator = ": "
        )
        uiState = LoginUiState.Error(errorText)
        viewModelScope.launch {
            _uiEvent.emit(LoginUiEvent.ShowToast(errorText))
        }
    }

    fun onLegacyGoogleSignInNullToken() {
        Log.e("LoginViewModel", "Legacy Google Sign-In: ID Token is null")
        val errorText = UiText.StringResource(R.string.error_google_failed_null_token)
        uiState = LoginUiState.Error(errorText)
        viewModelScope.launch {
            _uiEvent.emit(LoginUiEvent.ShowToast(errorText))
        }
    }

    fun onLegacyGoogleSignInApiException(e: ApiException, isResultOk: Boolean) {
        if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
            Log.i("LoginViewModel", "Legacy Google Sign-In cancelled by user")
            resetState()
            return
        }

        val statusString = when (e.statusCode) {
            GoogleSignInStatusCodes.DEVELOPER_ERROR -> "DEVELOPER_ERROR (Check SHA-1 signature)"
            GoogleSignInStatusCodes.NETWORK_ERROR -> "NETWORK_ERROR (Check connection)"
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "SIGN_IN_FAILED"
            else -> "Status code ${e.statusCode}"
        }
        Log.e("LoginViewModel", "Legacy Google Sign-In failed: $statusString", e)

        val baseError =
            if (isResultOk) R.string.error_google_failed_api_exception else R.string.error_google_failed
        val errorText = UiText.Composite(
            parts = listOf(
                UiText.StringResource(baseError),
                UiText.DynamicString(statusString)
            ),
            separator = ": "
        )
        uiState = LoginUiState.Error(errorText)
        viewModelScope.launch {
            _uiEvent.emit(LoginUiEvent.ShowToast(errorText))
        }
    }

    fun onLegacyGoogleSignInUnexpectedException(e: Exception) {
        Log.e("LoginViewModel", "Legacy Google Sign-In failed to parse task", e)
        resetState()
    }

    fun onLegacyGoogleSignInNullData(resultCode: Int) {
        Log.i(
            "LoginViewModel",
            "Legacy Google Sign-In returned result code $resultCode with null data"
        )
        resetState()
    }

    fun onLegacyGoogleSignOutFailed(e: Exception) {
        Log.w("LoginViewModel", "launchLegacyGoogleSignIn: Sign-out failed (non-fatal)", e)
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
                    _uiEvent.emit(LoginUiEvent.ShowToast(UiText.StringResource(R.string.text_reset_password_sent)))
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

    fun setGoogleSignInLoading() {
        uiState = LoginUiState.Loading
    }

    fun resetFields() {
        email = ""
        password = ""
        uiState = LoginUiState.Idle
    }
}

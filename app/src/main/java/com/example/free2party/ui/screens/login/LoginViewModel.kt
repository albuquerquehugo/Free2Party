package com.example.free2party.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.repository.AuthRepository
import com.example.free2party.data.repository.AuthRepositoryImpl
import com.example.free2party.data.repository.UserRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
    object Success : LoginUiState
}

sealed class LoginUiEvent {
    data class ShowToast(val message: String) : LoginUiEvent()
}

class LoginViewModel : ViewModel() {
    private val userRepository = UserRepositoryImpl(
        currentUserId = Firebase.auth.currentUser?.uid ?: "",
        db = Firebase.firestore,
        storage = Firebase.storage
    )
    private val authRepository: AuthRepository = AuthRepositoryImpl(
        auth = Firebase.auth,
        userRepository = userRepository
    )

    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onLoginClick(onSuccess: () -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            uiState = LoginUiState.Error("Email and password cannot be empty")
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
                    uiState = LoginUiState.Error(e.localizedMessage ?: "Login failed")
                }
        }
    }

    fun onForgotPasswordConfirm(email: String) {
        val normalizedEmail = email.trim().lowercase()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            uiState = LoginUiState.Error("Please enter a valid email address.")
            return
        }

        uiState = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(normalizedEmail)
                .onSuccess {
                    uiState = LoginUiState.Idle
                    _uiEvent.emit(LoginUiEvent.ShowToast("Password reset email sent! Please check your inbox."))
                }
                .onFailure { e ->
                    uiState = LoginUiState.Error(e.localizedMessage ?: "Failed to send reset email")
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

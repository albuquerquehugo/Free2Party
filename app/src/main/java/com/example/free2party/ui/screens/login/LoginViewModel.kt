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
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
    object Success : LoginUiState
}

class LoginViewModel : ViewModel() {
    private val userRepository = UserRepositoryImpl(
        currentUserId = Firebase.auth.currentUser?.uid ?: "",
        db = Firebase.firestore
    )
    private val authRepository: AuthRepository = AuthRepositoryImpl(
        auth = Firebase.auth,
        userRepository = userRepository
    )

    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    fun onLoginClick(onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            uiState = LoginUiState.Error("Email and password cannot be empty")
            return
        }

        uiState = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.login(email, password)
                .onSuccess {
                    uiState = LoginUiState.Success
                    onSuccess()
                }
                .onFailure { e ->
                    uiState = LoginUiState.Error(e.localizedMessage ?: "Login failed")
                }
        }
    }

    fun resetFields() {
        email = ""
        password = ""
        uiState = LoginUiState.Idle
    }
}

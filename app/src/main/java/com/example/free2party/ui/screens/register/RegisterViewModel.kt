package com.example.free2party.ui.screens.register

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
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
import kotlinx.coroutines.launch

sealed interface RegisterUiState {
    object Idle : RegisterUiState
    object Loading : RegisterUiState
    data class Error(val message: String) : RegisterUiState
    object Success : RegisterUiState
}

class RegisterViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(
        auth = Firebase.auth,
        userRepository = UserRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore,
            storage = Firebase.storage
        )
    )
) : ViewModel() {

    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var profilePicUri by mutableStateOf<Uri?>(null)

    val isFormValid by derivedStateOf {
        firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && password.isNotBlank()
    }

    var uiState by mutableStateOf<RegisterUiState>(RegisterUiState.Idle)
        private set

    fun onRegisterClick() {
        val normalizedEmail = email.trim().lowercase()
        if (firstName.isBlank() || lastName.isBlank() || normalizedEmail.isBlank() || password.isBlank()) {
            uiState = RegisterUiState.Error("All fields are required")
            return
        }

        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(normalizedEmail)) {
            uiState = RegisterUiState.Error("Please enter a valid email address")
            return
        }

        uiState = RegisterUiState.Loading
        viewModelScope.launch {
            authRepository.register(firstName, lastName, normalizedEmail, password, profilePicUri)
                .onSuccess { uiState = RegisterUiState.Success }
                .onFailure { e ->
                    uiState = RegisterUiState.Error(
                        e.localizedMessage ?: "Registration failed"
                    )
                }
        }
    }

    fun resetFields() {
        firstName = ""
        lastName = ""
        email = ""
        password = ""
        profilePicUri = null
        uiState = RegisterUiState.Idle
    }
}

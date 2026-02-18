package com.example.free2party.ui.screens.register

import android.net.Uri
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

class RegisterViewModel : ViewModel() {
    private val userRepository = UserRepositoryImpl(
        auth = Firebase.auth,
        db = Firebase.firestore,
        storage = Firebase.storage
    )
    private val authRepository: AuthRepository = AuthRepositoryImpl(
        auth = Firebase.auth,
        userRepository = userRepository
    )

    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var profilePicUri by mutableStateOf<Uri?>(null)

    var uiState by mutableStateOf<RegisterUiState>(RegisterUiState.Idle)
        private set

    fun onRegisterClick() {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            uiState = RegisterUiState.Error("All fields are required")
            return
        }

        uiState = RegisterUiState.Loading
        viewModelScope.launch {
            authRepository.register(firstName, lastName, email, password, profilePicUri)
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

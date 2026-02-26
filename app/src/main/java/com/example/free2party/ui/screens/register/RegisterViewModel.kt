package com.example.free2party.ui.screens.register

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.Countries
import com.example.free2party.data.model.DatePattern
import com.example.free2party.data.model.UserSocials
import com.example.free2party.data.repository.AuthRepository
import com.example.free2party.data.repository.AuthRepositoryImpl
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.util.isValidDateDigits
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
    var phoneNumber by mutableStateOf("")
    var countryCode by mutableStateOf("")
    var birthday by mutableStateOf("")
    var bio by mutableStateOf("")
    var facebookUsername by mutableStateOf("")
    var instagramUsername by mutableStateOf("")
    var tiktokUsername by mutableStateOf("")
    var xUsername by mutableStateOf("")

    private val isPhoneValid by derivedStateOf {
        if (phoneNumber.isEmpty()) return@derivedStateOf countryCode.isEmpty()
        val country = Countries.find { it.code == countryCode }
        country == null || phoneNumber.length == country.digitsCount
    }

    private val isBirthdayValid by derivedStateOf {
        birthday.isEmpty() || isValidDateDigits(birthday, DatePattern.YYYY_MM_DD)
    }

    val isFormValid by derivedStateOf {
        firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                isPhoneValid &&
                isBirthdayValid
    }

    var uiState by mutableStateOf<RegisterUiState>(RegisterUiState.Idle)
        private set

    fun onRegisterClick() {
        val normalizedEmail = email.trim().lowercase()
        if (firstName.isBlank() || lastName.isBlank() || normalizedEmail.isBlank() || password.isBlank()) {
            uiState = RegisterUiState.Error("Required fields (*) must be filled")
            return
        }

        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(normalizedEmail)) {
            uiState = RegisterUiState.Error("Please enter a valid email address")
            return
        }

        if (!isPhoneValid) {
            uiState = RegisterUiState.Error("Please enter a valid phone number")
            return
        }

        if (!isBirthdayValid) {
            uiState = RegisterUiState.Error("Please enter a valid date")
            return
        }

        uiState = RegisterUiState.Loading
        viewModelScope.launch {
            authRepository.register(
                firstName = firstName,
                lastName = lastName,
                email = normalizedEmail,
                password = password,
                profilePicUri = profilePicUri,
                phoneNumber = phoneNumber,
                countryCode = countryCode,
                birthday = birthday,
                bio = bio,
                socials = UserSocials(
                    facebookUsername = facebookUsername,
                    instagramUsername = instagramUsername,
                    tiktokUsername = tiktokUsername,
                    xUsername = xUsername
                )
            ).onSuccess { uiState = RegisterUiState.Success }
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
        phoneNumber = ""
        countryCode = ""
        birthday = ""
        bio = ""
        facebookUsername = ""
        instagramUsername = ""
        tiktokUsername = ""
        xUsername = ""
        uiState = RegisterUiState.Idle
    }
}

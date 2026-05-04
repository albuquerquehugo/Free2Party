package com.example.free2party.ui.screens.register

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.R
import com.example.free2party.data.model.Countries
import com.example.free2party.data.model.DatePattern
import com.example.free2party.data.model.Gender
import com.example.free2party.data.model.UserSocials
import com.example.free2party.data.repository.AuthRepository
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.exception.AuthException
import com.example.free2party.exception.InfrastructureException
import com.example.free2party.util.UiText
import com.example.free2party.util.isBirthdayFieldValid
import com.example.free2party.util.isPhoneValid
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface RegisterUiState {
    object Idle : RegisterUiState
    object Loading : RegisterUiState
    data class Error(val message: UiText) : RegisterUiState
    object Success : RegisterUiState
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var profilePicUri by mutableStateOf<Uri?>(null)
    var countryCode by mutableStateOf("")
    private var _phoneNumber by mutableStateOf("")
    var phoneNumber: String
        get() = _phoneNumber
        set(value) {
            _phoneNumber = value
            if (value.length >= 3) {
                val isNanp = Countries.find { it.code == countryCode }?.phoneCode == "+1"
                if (countryCode.isEmpty() || isNanp) {
                    com.example.free2party.util.getCountryFromNanpAreaCode(value)?.let {
                        countryCode = it
                    }
                }
            }
        }
    var birthday by mutableStateOf("")
    var bio by mutableStateOf("")
    var gender by mutableStateOf(Gender.OTHER)
    var whatsappCountryCode by mutableStateOf("")
    private var _whatsappNumber by mutableStateOf("")
    var whatsappNumber: String
        get() = _whatsappNumber
        set(value) {
            _whatsappNumber = value
            if (value.length >= 3) {
                val isNanp = Countries.find { it.code == whatsappCountryCode }?.phoneCode == "+1"
                if (whatsappCountryCode.isEmpty() || isNanp) {
                    com.example.free2party.util.getCountryFromNanpAreaCode(value)?.let {
                        whatsappCountryCode = it
                    }
                }
            }
        }
    var isWhatsappSameAsPhone by mutableStateOf(false)
    var telegramUsername by mutableStateOf("")
    var facebookUsername by mutableStateOf("")
    var instagramUsername by mutableStateOf("")
    var tiktokUsername by mutableStateOf("")
    var xUsername by mutableStateOf("")

    var gradientBackground by mutableStateOf(true)
        private set

    init {
        observeGradientBackground()
    }

    private fun observeGradientBackground() {
        viewModelScope.launch {
            settingsRepository.gradientBackgroundFlow.collectLatest { enabled ->
                gradientBackground = enabled
            }
        }
    }

    private val isEmailValid by derivedStateOf {
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        emailPattern.matches(email)
    }

    private val isPhoneNumberValid by derivedStateOf {
        isPhoneValid(phoneNumber, countryCode)
    }

    private fun isBirthdayValid(context: Context): Boolean {
        return isBirthdayFieldValid(birthday, context, DatePattern.YYYY_MM_DD.patternResId)
    }

    private val isWhatsappNumberValid by derivedStateOf {
        isPhoneValid(whatsappNumber, whatsappCountryCode)
    }

    private val isPasswordConfirmed by derivedStateOf {
        password == confirmPassword
    }

    fun isFormValid(context: Context): Boolean {
        return firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                isEmailValid &&
                password.isNotBlank() &&
                isPhoneNumberValid &&
                isBirthdayValid(context) &&
                isWhatsappNumberValid &&
                isPasswordConfirmed
    }

    var uiState by mutableStateOf<RegisterUiState>(RegisterUiState.Idle)
        private set

    fun onRegisterClick(context: Context) {
        val normalizedEmail = email.trim().lowercase()
        firstName = firstName.trim()
        lastName = lastName.trim()
        bio = bio.trim()
        telegramUsername = telegramUsername.trim()
        facebookUsername = facebookUsername.trim()
        instagramUsername = instagramUsername.trim()
        tiktokUsername = tiktokUsername.trim()
        xUsername = xUsername.trim()

        if (firstName.isBlank() || lastName.isBlank() || normalizedEmail.isBlank() ||
            password.isBlank() || confirmPassword.isBlank()
        ) {
            uiState =
                RegisterUiState.Error(UiText.StringResource(R.string.error_required_fields))
            return
        }

        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(normalizedEmail)) {
            uiState =
                RegisterUiState.Error(UiText.StringResource(R.string.error_invalid_email))
            return
        }

        if (password != confirmPassword) {
            uiState =
                RegisterUiState.Error(UiText.StringResource(R.string.error_passwords_not_match))
            return
        }

        if (!isPhoneNumberValid) {
            uiState =
                RegisterUiState.Error(UiText.StringResource(R.string.error_invalid_phone))
            return
        }

        if (!isBirthdayValid(context)) {
            uiState = RegisterUiState.Error(UiText.StringResource(R.string.error_invalid_date))
            return
        }

        if (!isWhatsappNumberValid) {
            uiState =
                RegisterUiState.Error(UiText.StringResource(R.string.error_invalid_whatsapp))
            return
        }

        val whatsappFullNumber = if (whatsappNumber.isNotBlank()) {
            val country = Countries.find { it.code == whatsappCountryCode }
            val code = country?.phoneCode?.filter { it.isDigit() } ?: ""
            code + whatsappNumber
        } else ""

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
                gender = gender,
                socials = UserSocials(
                    whatsappNumber = whatsappNumber,
                    whatsappCountryCode = whatsappCountryCode,
                    whatsappFullNumber = whatsappFullNumber,
                    telegramUsername = telegramUsername,
                    facebookUsername = facebookUsername,
                    instagramUsername = instagramUsername,
                    tiktokUsername = tiktokUsername,
                    xUsername = xUsername
                )
            ).onSuccess {
                uiState = RegisterUiState.Success
            }.onFailure { e ->
                val errorText = when (e) {
                    is AuthException -> if (e.messageRes != null) UiText.StringResource(e.messageRes) else UiText.StringResource(
                        R.string.error_registration_failed
                    )

                    is InfrastructureException -> if (e.messageRes != null) UiText.StringResource(e.messageRes) else UiText.StringResource(
                        R.string.error_infrastructure
                    )

                    else -> UiText.StringResource(R.string.error_registration_failed)
                }
                uiState = RegisterUiState.Error(errorText)
            }
        }
    }

    fun resetFields() {
        firstName = ""
        lastName = ""
        email = ""
        password = ""
        confirmPassword = ""
        profilePicUri = null
        phoneNumber = ""
        countryCode = ""
        birthday = ""
        bio = ""
        gender = Gender.OTHER
        whatsappNumber = ""
        whatsappCountryCode = ""
        isWhatsappSameAsPhone = false
        telegramUsername = ""
        facebookUsername = ""
        instagramUsername = ""
        tiktokUsername = ""
        xUsername = ""
        uiState = RegisterUiState.Idle
    }
}

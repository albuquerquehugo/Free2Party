package com.example.free2party.ui.screens.profile

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.Countries
import com.example.free2party.data.model.User
import com.example.free2party.data.model.UserSocials
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.util.isValidDateDigits
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(
        val user: User,
        val isSaving: Boolean = false,
        val isUploadingImage: Boolean = false
    ) : ProfileUiState

    data class Error(val message: String) : ProfileUiState
}

sealed class ProfileUiEvent {
    data class ShowToast(val message: String, val navigateBack: Boolean = false) : ProfileUiEvent()
}

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState by mutableStateOf<ProfileUiState>(ProfileUiState.Loading)
        internal set

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Edited fields
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var countryCode by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var birthday by mutableStateOf("")
    var bio by mutableStateOf("")
    var whatsappCountryCode by mutableStateOf("")
    var whatsappNumber by mutableStateOf("")
    var telegramUsername by mutableStateOf("")
    var facebookUsername by mutableStateOf("")
    var instagramUsername by mutableStateOf("")
    var tiktokUsername by mutableStateOf("")
    var xUsername by mutableStateOf("")

    val isPhoneValid by derivedStateOf {
        if (phoneNumber.isEmpty()) return@derivedStateOf countryCode.isEmpty()
        val country = Countries.find { it.code == countryCode }
        country == null || phoneNumber.length == country.digitsCount
    }

    val isBirthdayValid by derivedStateOf {
        val pattern = (uiState as? ProfileUiState.Success)?.user?.settings?.datePattern
        birthday.isEmpty() || (pattern != null && isValidDateDigits(birthday, pattern))
    }

    val isWhatsappValid by derivedStateOf {
        if (whatsappNumber.isEmpty()) return@derivedStateOf whatsappCountryCode.isEmpty()
        val country = Countries.find { it.code == whatsappCountryCode }
        country == null || whatsappNumber.length == country.digitsCount
    }

    val isFormValid by derivedStateOf {
        firstName.isNotBlank() && lastName.isNotBlank() && isPhoneValid && isBirthdayValid && isWhatsappValid
    }

    val hasChanges by derivedStateOf {
        val user = (uiState as? ProfileUiState.Success)?.user ?: return@derivedStateOf false
        firstName != user.firstName ||
                lastName != user.lastName ||
                countryCode != user.countryCode ||
                phoneNumber != user.phoneNumber ||
                birthday != user.birthday ||
                bio != user.bio ||
                whatsappCountryCode != user.socials.whatsappCountryCode ||
                whatsappNumber != user.socials.whatsappNumber ||
                telegramUsername != user.socials.telegramUsername ||
                facebookUsername != user.socials.facebookUsername ||
                instagramUsername != user.socials.instagramUsername ||
                tiktokUsername != user.socials.tiktokUsername ||
                xUsername != user.socials.xUsername
    }

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userRepository.observeUser(userRepository.currentUserId)
                .catch { e ->
                    uiState = ProfileUiState.Error(
                        e.localizedMessage
                            ?: "User profile not found. Please try logging out and in again."
                    )
                }
                .collect { user ->
                    val currentState = uiState
                    if (currentState !is ProfileUiState.Success) {
                        // Initial load, populate edited fields
                        initializeFields(user)
                    }
                    uiState = if (currentState is ProfileUiState.Success) {
                        currentState.copy(user = user)
                    } else {
                        ProfileUiState.Success(user = user)
                    }
                }
        }
    }

    private fun initializeFields(user: User) {
        firstName = user.firstName
        lastName = user.lastName
        countryCode = user.countryCode
        phoneNumber = user.phoneNumber
        birthday = user.birthday
        bio = user.bio
        whatsappCountryCode = user.socials.whatsappCountryCode
        whatsappNumber = user.socials.whatsappNumber
        telegramUsername = user.socials.telegramUsername
        facebookUsername = user.socials.facebookUsername
        instagramUsername = user.socials.instagramUsername
        tiktokUsername = user.socials.tiktokUsername
        xUsername = user.socials.xUsername
    }

    fun discardChanges() {
        val user = (uiState as? ProfileUiState.Success)?.user ?: return
        initializeFields(user)
    }

    fun updateProfile() {
        val currentState = uiState as? ProfileUiState.Success ?: return
        if (!isPhoneValid) {
            uiState = ProfileUiState.Error("Please enter a valid phone number")
            return
        }

        if (!isBirthdayValid) {
            uiState = ProfileUiState.Error("Please enter a valid date")
            return
        }

        if (!isWhatsappValid) {
            uiState = ProfileUiState.Error("Please enter a valid WhatsApp number")
            return
        }

        if (!isFormValid) return

        val whatsappFullNumber = if (whatsappNumber.isNotBlank()) {
            val country = Countries.find { it.code == whatsappCountryCode }
            val code = country?.phoneCode?.filter { it.isDigit() } ?: ""
            code + whatsappNumber
        } else ""

        val updatedUser = currentState.user.copy(
            firstName = firstName,
            lastName = lastName,
            countryCode = countryCode,
            phoneNumber = phoneNumber,
            birthday = birthday,
            bio = bio,
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
        )

        uiState = currentState.copy(isSaving = true)
        viewModelScope.launch {
            userRepository.updateUser(updatedUser)
                .onSuccess {
                    uiState =
                        (uiState as? ProfileUiState.Success)?.copy(isSaving = false) ?: uiState
                    _uiEvent.emit(
                        ProfileUiEvent.ShowToast(
                            "Profile updated successfully!",
                            navigateBack = true
                        )
                    )
                }
                .onFailure { e ->
                    uiState =
                        (uiState as? ProfileUiState.Success)?.copy(isSaving = false) ?: uiState
                    _uiEvent.emit(ProfileUiEvent.ShowToast("Error: ${e.localizedMessage}"))
                }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        val currentState = uiState as? ProfileUiState.Success ?: return
        uiState = currentState.copy(isUploadingImage = true)

        viewModelScope.launch {
            userRepository.uploadProfilePicture(uri)
                .onSuccess { downloadUrl ->
                    val updatedUser = currentState.user.copy(profilePicUrl = downloadUrl)
                    userRepository.updateUser(updatedUser)
                        .onSuccess {
                            uiState =
                                (uiState as? ProfileUiState.Success)?.copy(isUploadingImage = false)
                                    ?: uiState
                            _uiEvent.emit(ProfileUiEvent.ShowToast("Profile picture updated!"))
                        }
                        .onFailure { e ->
                            uiState =
                                (uiState as? ProfileUiState.Success)?.copy(isUploadingImage = false)
                                    ?: uiState
                            _uiEvent.emit(
                                ProfileUiEvent.ShowToast(
                                    "Error updating profile with new image: ${e.localizedMessage}"
                                )
                            )
                        }
                }
                .onFailure { e ->
                    uiState = (uiState as? ProfileUiState.Success)?.copy(isUploadingImage = false)
                        ?: uiState
                    _uiEvent.emit(
                        ProfileUiEvent.ShowToast(
                            "Error uploading image: ${e.localizedMessage}"
                        )
                    )
                }
        }
    }

    companion object {
        fun provideFactory(
            userRepository: UserRepository = UserRepositoryImpl(
                auth = Firebase.auth,
                db = Firebase.firestore,
                storage = Firebase.storage
            )
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(userRepository) as T
            }
        }
    }
}

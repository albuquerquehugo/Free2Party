package com.example.free2party.ui.screens.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.User
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
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
    data class ShowToast(val message: String) : ProfileUiEvent()
}

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState by mutableStateOf<ProfileUiState>(ProfileUiState.Loading)
        internal set

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

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
                    uiState = if (currentState is ProfileUiState.Success) {
                        currentState.copy(user = user)
                    } else {
                        ProfileUiState.Success(user = user)
                    }
                }
        }
    }

    fun updateProfile(updatedUser: User) {
        val currentState = uiState as? ProfileUiState.Success ?: return
        uiState = currentState.copy(isSaving = true)

        viewModelScope.launch {
            userRepository.updateUser(updatedUser)
                .onSuccess {
                    uiState =
                        (uiState as? ProfileUiState.Success)?.copy(isSaving = false) ?: uiState
                    _uiEvent.emit(ProfileUiEvent.ShowToast("Profile updated successfully!"))
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

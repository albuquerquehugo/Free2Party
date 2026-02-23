package com.example.free2party.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val user: User, val isSaving: Boolean = false) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

sealed class SettingsUiEvent {
    data class ShowToast(val message: String) : SettingsUiEvent()
}

class SettingsViewModel(
    private val userRepository: UserRepository = UserRepositoryImpl(
        auth = Firebase.auth,
        db = Firebase.firestore,
        storage = Firebase.storage
    )
) : ViewModel() {

    var uiState by mutableStateOf<SettingsUiState>(SettingsUiState.Loading)
        private set

    private val _uiEvent = MutableSharedFlow<SettingsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userRepository.observeUser(userRepository.currentUserId)
                .catch { e ->
                    uiState = SettingsUiState.Error(
                        e.localizedMessage
                            ?: "User settings not found. Please try logging out and in again."
                    )
                }
                .collect { user ->
                    val currentState = uiState
                    uiState = if (currentState is SettingsUiState.Success) {
                        currentState.copy(user = user)
                    } else {
                        SettingsUiState.Success(user = user)
                    }
                }
        }
    }

    fun updateSettings(updatedUser: User) {
        val currentState = uiState as? SettingsUiState.Success ?: return
        uiState = currentState.copy(isSaving = true)

        viewModelScope.launch {
            userRepository.updateUser(updatedUser)
                .onSuccess {
                    uiState =
                        (uiState as? SettingsUiState.Success)?.copy(isSaving = false) ?: uiState
                    _uiEvent.emit(SettingsUiEvent.ShowToast("Settings updated successfully!"))
                }
                .onFailure { e ->
                    uiState =
                        (uiState as? SettingsUiState.Success)?.copy(isSaving = false) ?: uiState
                    _uiEvent.emit(SettingsUiEvent.ShowToast("Error: ${e.localizedMessage}"))
                }
        }
    }
}

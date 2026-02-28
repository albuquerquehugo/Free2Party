package com.example.free2party.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.model.User
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
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
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var uiState by mutableStateOf<SettingsUiState>(SettingsUiState.Loading)
        private set

    var themeMode by mutableStateOf(ThemeMode.AUTOMATIC)
        private set

    private val _uiEvent = MutableSharedFlow<SettingsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadSettings()
        observeThemeMode()
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

    private fun observeThemeMode() {
        viewModelScope.launch {
            settingsRepository.themeModeFlow.collectLatest { mode ->
                themeMode = mode
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
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

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            userRepository: UserRepository = UserRepositoryImpl(
                auth = Firebase.auth,
                db = Firebase.firestore,
                storage = Firebase.storage
            )
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(userRepository, settingsRepository) as T
            }
        }
    }
}

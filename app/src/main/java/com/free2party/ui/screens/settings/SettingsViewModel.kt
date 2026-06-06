package com.free2party.ui.screens.settings

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.Circle
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.ThemeMode
import com.free2party.data.model.User
import com.free2party.data.repository.SettingsRepository
import com.free2party.data.repository.SocialRepository
import com.free2party.data.repository.UserRepository
import com.free2party.R
import com.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val user: User, val isSaving: Boolean = false) : SettingsUiState
    data class Error(val message: UiText) : SettingsUiState
}

sealed class SettingsUiEvent {
    data class ShowToast(val message: UiText) : SettingsUiEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    socialRepository: SocialRepository
) : ViewModel() {

    var uiState by mutableStateOf<SettingsUiState>(SettingsUiState.Loading)
        private set

    val circles: StateFlow<List<Circle>> = socialRepository.getCircles()
        .catch { e -> Log.e("SettingsViewModel", "Error in circles flow", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsList: StateFlow<List<FriendInfo>> = socialRepository.getFriendsList()
        .catch { e -> Log.e("SettingsViewModel", "Error in friendsList flow", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var themeMode by mutableStateOf(ThemeMode.AUTOMATIC)
        private set

    var gradientBackground by mutableStateOf(true)
        private set

    private val _uiEvent = MutableSharedFlow<SettingsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadSettings()
        observeThemeMode()
        observeGradientBackground()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userRepository.observeUser(userRepository.currentUserId)
                .catch { _ ->
                    uiState = SettingsUiState.Error(
                        UiText.StringResource(R.string.error_settings_not_found)
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

    private fun observeGradientBackground() {
        viewModelScope.launch {
            settingsRepository.gradientBackgroundFlow.collectLatest { enabled ->
                gradientBackground = enabled
            }
        }
    }

    fun updateSettings(updatedUser: User, showToast: Boolean = true) {
        val currentState = uiState as? SettingsUiState.Success ?: return
        if (showToast) {
            uiState = currentState.copy(isSaving = true)
        }

        viewModelScope.launch {
            userRepository.updateUser(updatedUser)
                .onSuccess {
                    if (showToast) {
                        uiState =
                            (uiState as? SettingsUiState.Success)?.copy(isSaving = false) ?: uiState
                        _uiEvent.emit(SettingsUiEvent.ShowToast(UiText.StringResource(R.string.toast_settings_updated)))
                    }
                }
                .onFailure { _ ->
                    if (showToast) {
                        uiState =
                            (uiState as? SettingsUiState.Success)?.copy(isSaving = false) ?: uiState
                        _uiEvent.emit(
                            SettingsUiEvent.ShowToast(
                                UiText.StringResource(R.string.error_updating_settings)
                            )
                        )
                    }
                }
        }
    }
}

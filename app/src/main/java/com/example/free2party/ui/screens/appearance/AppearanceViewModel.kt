package com.example.free2party.ui.screens.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var themeMode by mutableStateOf(ThemeMode.AUTOMATIC)
        private set

    var gradientBackground by mutableStateOf(true)
        private set

    init {
        observeThemeMode()
        observeGradientBackground()
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

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            
            // Also update the Firestore user settings
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                val userResult = userRepository.getUserById(uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrThrow()
                    val updatedUser = user.copy(
                        settings = user.settings.copy(themeMode = mode)
                    )
                    userRepository.updateUser(updatedUser)
                }
            }
        }
    }

    fun updateGradientBackground(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGradientBackground(enabled)
            
            // Also update the Firestore user settings
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                val userResult = userRepository.getUserById(uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrThrow()
                    val updatedUser = user.copy(
                        settings = user.settings.copy(gradientBackground = enabled)
                    )
                    userRepository.updateUser(updatedUser)
                }
            }
        }
    }
}

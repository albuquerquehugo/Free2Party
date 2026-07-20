package com.free2party.ui.screens.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.ThemeMode
import com.free2party.data.model.Membership
import com.free2party.data.repository.SettingsRepository
import com.free2party.data.repository.UserRepository
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

    var isGradientBackground by mutableStateOf(true)
        private set

    var gradientTheme by mutableStateOf("DEFAULT")
        private set

    var membership by mutableStateOf(Membership.FREE)
        private set

    var statusEmoji by mutableStateOf("")
        private set

    var statusColor by mutableStateOf("")
        private set

    var profilePicUrl by mutableStateOf("")
        private set

    init {
        observeThemeMode()
        observeGradientBackground()
        observeGradientTheme()
        observeUser()
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
                isGradientBackground = enabled
            }
        }
    }

    private fun observeGradientTheme() {
        viewModelScope.launch {
            settingsRepository.gradientThemeFlow.collectLatest { theme ->
                gradientTheme = theme
            }
        }
    }

    private fun observeUser() {
        viewModelScope.launch {
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                userRepository.observeUser(uid).collectLatest { user ->
                    membership = user.membership
                    statusEmoji = user.statusEmoji
                    statusColor = user.settings.statusColor
                    profilePicUrl = user.profilePicUrl
                }
            }
        }
    }

    fun updateStatusEmoji(emoji: String) {
        if (membership == Membership.FREE && emoji.isNotBlank()) return
        viewModelScope.launch {
            statusEmoji = emoji
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                val userResult = userRepository.getUserById(uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrThrow()
                    val updatedUser = user.copy(statusEmoji = emoji)
                    userRepository.updateUser(updatedUser)
                }
            }
        }
    }

    fun updateStatusColor(color: String) {
        if (membership == Membership.FREE && color.isNotBlank()) return
        viewModelScope.launch {
            statusColor = color
            settingsRepository.setStatusColor(color)
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                val userResult = userRepository.getUserById(uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrThrow()
                    val updatedUser = user.copy(
                        settings = user.settings.copy(statusColor = color)
                    )
                    userRepository.updateUser(updatedUser)
                }
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

    fun updateGradientTheme(theme: String) {
        if (membership == Membership.FREE && theme != "DEFAULT") return
        viewModelScope.launch {
            settingsRepository.setGradientTheme(theme)
            
            // Also update the Firestore user settings
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                val userResult = userRepository.getUserById(uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrThrow()
                    val updatedUser = user.copy(
                        settings = user.settings.copy(gradientTheme = theme)
                    )
                    userRepository.updateUser(updatedUser)
                }
            }
        }
    }
}

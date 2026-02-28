package com.example.free2party

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.repository.SettingsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var themeMode by mutableStateOf(ThemeMode.AUTOMATIC)
        private set

    init {
        observeThemeMode()
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            settingsRepository.themeModeFlow.collectLatest { mode ->
                themeMode = mode
            }
        }
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(settingsRepository) as T
            }
        }
    }
}

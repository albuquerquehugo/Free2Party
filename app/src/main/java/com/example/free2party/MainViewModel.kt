package com.example.free2party

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    var themeMode by mutableStateOf(ThemeMode.AUTOMATIC)
        private set

    init {
        observeUserSettings()
    }

    private fun observeUserSettings() {
        viewModelScope.launch {
            val currentUserId = userRepository.currentUserId
            if (currentUserId.isNotEmpty()) {
                userRepository.observeUser(currentUserId).collectLatest { user ->
                    themeMode = user.settings.themeMode
                }
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
                return MainViewModel(userRepository) as T
            }
        }
    }
}

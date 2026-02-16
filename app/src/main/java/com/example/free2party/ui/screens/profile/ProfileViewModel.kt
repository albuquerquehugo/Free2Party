package com.example.free2party.ui.screens.profile

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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

sealed class ProfileUiEvent {
    data class ShowToast(val message: String) : ProfileUiEvent()
}

class ProfileViewModel : ViewModel() {
    private val userRepository: UserRepository = UserRepositoryImpl(
        currentUserId = Firebase.auth.currentUser?.uid ?: "",
        db = Firebase.firestore
    )

    var uiState by mutableStateOf<ProfileUiState>(ProfileUiState.Loading)
        private set

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userRepository.observeUser(userRepository.currentUserId)
                .catch { e ->
                    uiState = ProfileUiState.Error(e.localizedMessage ?: "Unknown error")
                }
                .collect { user ->
                    uiState = ProfileUiState.Success(user)
                }
        }
    }

    fun updateName(newName: String) {
        if (uiState !is ProfileUiState.Success) return
        
        viewModelScope.launch {
            userRepository.updateUserName(newName)
                .onSuccess {
                    _uiEvent.emit(ProfileUiEvent.ShowToast("Name updated successfully!"))
                }
                .onFailure { e ->
                    _uiEvent.emit(ProfileUiEvent.ShowToast("Error: ${e.localizedMessage}"))
                }
        }
    }
}

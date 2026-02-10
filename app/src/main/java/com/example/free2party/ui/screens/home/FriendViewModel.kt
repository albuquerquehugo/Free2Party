package com.example.free2party.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.SocialRepositoryImpl
import com.example.free2party.data.repository.UserRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface InviteFriendUiState {
    object Idle : InviteFriendUiState
    object Searching : InviteFriendUiState
    data class Error(val message: String) : InviteFriendUiState
    object Success : InviteFriendUiState
}

sealed class FriendUiEvent {
    data class InviteSentSuccessfully(val email: String) : FriendUiEvent()
}

class FriendViewModel : ViewModel() {
    private val userRepository = UserRepositoryImpl(
        currentUserId = Firebase.auth.currentUser?.uid ?: "",
        db = Firebase.firestore
    )
    private val socialRepository: SocialRepository = SocialRepositoryImpl(
        db = Firebase.firestore,
        userRepository = userRepository
    )

    var searchQuery by mutableStateOf("")
    var uiState by mutableStateOf<InviteFriendUiState>(InviteFriendUiState.Idle)
        private set

    private val _uiEvent = MutableSharedFlow<FriendUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun inviteFriend() {
        val inputEmail = searchQuery.trim().lowercase()

        if (inputEmail.isBlank()) {
            uiState = InviteFriendUiState.Error("Please enter a valid email.")
            return
        }

        uiState = InviteFriendUiState.Searching
        viewModelScope.launch {
            socialRepository.sendFriendRequest(inputEmail)
                .onSuccess {
                    searchQuery = ""
                    uiState = InviteFriendUiState.Success
                    _uiEvent.emit(FriendUiEvent.InviteSentSuccessfully(inputEmail))
                }
                .onFailure { e ->
                    uiState =
                        InviteFriendUiState.Error(e.localizedMessage ?: "Error sending invite.")
                }
        }
    }

    fun resetState() {
        searchQuery = ""
        uiState = InviteFriendUiState.Idle
    }
}

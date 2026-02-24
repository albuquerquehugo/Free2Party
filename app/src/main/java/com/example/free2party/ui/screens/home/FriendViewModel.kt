package com.example.free2party.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.repository.PlanRepositoryImpl
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.SocialRepositoryImpl
import com.example.free2party.data.repository.UserRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
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

class FriendViewModel(
    private val socialRepository: SocialRepository = SocialRepositoryImpl(
        db = Firebase.firestore,
        userRepository = UserRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore,
            storage = Firebase.storage
        ),
        planRepository = PlanRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore
        )
    )
) : ViewModel() {

    var uiState by mutableStateOf<InviteFriendUiState>(InviteFriendUiState.Idle)
        private set

    private val _uiEvent = MutableSharedFlow<FriendUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun inviteFriend(email: String) {
        val normalizedEmail = email.trim().lowercase()

        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(normalizedEmail)) {
            uiState = InviteFriendUiState.Error("Please enter a valid email address.")
            return
        }

        uiState = InviteFriendUiState.Searching
        viewModelScope.launch {
            socialRepository.sendFriendRequest(normalizedEmail)
                .onSuccess {
                    uiState = InviteFriendUiState.Success
                    _uiEvent.emit(FriendUiEvent.InviteSentSuccessfully(normalizedEmail))
                }
                .onFailure { e ->
                    uiState =
                        InviteFriendUiState.Error(e.localizedMessage ?: "Error sending invite.")
                }
        }
    }

    fun resetState() {
        uiState = InviteFriendUiState.Idle
    }
}

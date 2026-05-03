package com.example.free2party.ui.screens.friends

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.free2party.R
import com.example.free2party.data.model.UserSearchResult
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.SocialRepositoryImpl
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.exception.InfrastructureException
import com.example.free2party.exception.SocialException
import com.example.free2party.util.UiText
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface InviteFriendUiState {
    object Idle : InviteFriendUiState
    object Searching : InviteFriendUiState
    data class Error(val message: UiText) : InviteFriendUiState
    object Success : InviteFriendUiState
}

sealed class FriendUiEvent {
    object InviteSentSuccessfully : FriendUiEvent()
    data class ShowToast(val message: UiText) : FriendUiEvent()
}

class FriendViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    var uiState by mutableStateOf<InviteFriendUiState>(InviteFriendUiState.Idle)
        private set

    var searchResults by mutableStateOf<List<UserSearchResult>>(emptyList())
        private set

    var isSearchingUsers by mutableStateOf(value = false)
        private set

    private val _uiEvent = MutableSharedFlow<FriendUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var searchJob: Job? = null

    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearchingUsers = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            isSearchingUsers = true
            socialRepository.searchUsers(query)
                .onSuccess { results ->
                    searchResults = results
                }
                .onFailure { e ->
                    Log.e("FriendViewModel", "Search users failed", e)
                    searchResults = emptyList()
                }
            isSearchingUsers = false
        }
    }

    fun inviteFriend(email: String) {
        val normalizedEmail = email.trim().lowercase()

        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(normalizedEmail)) {
            uiState =
                InviteFriendUiState.Error(UiText.StringResource(R.string.error_invalid_email))
            return
        }

        uiState = InviteFriendUiState.Searching
        viewModelScope.launch {
            socialRepository.sendFriendRequest(normalizedEmail)
                .onSuccess {
                    uiState = InviteFriendUiState.Success
                    _uiEvent.emit(FriendUiEvent.InviteSentSuccessfully)
                }
                .onFailure { e ->
                    val errorText = when (e) {
                        is InfrastructureException ->
                            if (e.messageRes != null) UiText.StringResource(e.messageRes)
                            else UiText.StringResource(R.string.error_infrastructure)

                        is SocialException ->
                            if (e.messageRes != null) UiText.StringResource(e.messageRes)
                            else UiText.StringResource(R.string.error_social)

                        else -> UiText.StringResource(R.string.error_sending_invite)
                    }
                    uiState = InviteFriendUiState.Error(errorText)
                    _uiEvent.emit(FriendUiEvent.ShowToast(errorText))
                }
        }
    }

    fun resetState() {
        uiState = InviteFriendUiState.Idle
        searchResults = emptyList()
        isSearchingUsers = false
        searchJob?.cancel()
    }

    companion object {
        fun provideFactory(
            context: android.content.Context,
            socialRepository: SocialRepository = SocialRepositoryImpl(
                db = Firebase.firestore,
                userRepository = UserRepositoryImpl(
                    auth = Firebase.auth,
                    db = Firebase.firestore,
                    storage = Firebase.storage
                ),
                context = context
            )
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FriendViewModel(socialRepository) as T
            }
        }
    }
}

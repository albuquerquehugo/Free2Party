package com.free2party.ui.screens.friends

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.FriendRequestStatus
import com.free2party.data.model.UserRelationship
import com.free2party.data.model.UserSearchResult
import com.free2party.data.repository.SocialRepository
import com.free2party.exception.InfrastructureException
import com.free2party.exception.SocialException
import com.free2party.R
import com.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface AddFriendUiState {
    object Idle : AddFriendUiState
    object Searching : AddFriendUiState
    data class Error(val message: UiText) : AddFriendUiState
    object Success : AddFriendUiState
}

sealed class FriendUiEvent {
    object FriendRequestSentSuccessfully : FriendUiEvent()
    data class ShowToast(val message: UiText) : FriendUiEvent()
}

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    var uiState by mutableStateOf<AddFriendUiState>(AddFriendUiState.Idle)
        private set

    var searchResults by mutableStateOf<List<UserSearchResult>>(emptyList())
        private set

    var isSearchingUsers by mutableStateOf(value = false)
        private set

    var isSendingRequest by mutableStateOf(value = false)
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
            delay(300.milliseconds) // Debounce
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

    fun addFriend(email: String) {
        val normalizedEmail = email.trim().lowercase()

        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(normalizedEmail)) {
            uiState =
                AddFriendUiState.Error(UiText.StringResource(R.string.error_invalid_email))
            return
        }

        uiState = AddFriendUiState.Searching
        isSendingRequest = true
        viewModelScope.launch {
            try {
                socialRepository.sendFriendRequest(normalizedEmail)
                    .onSuccess {
                        uiState = AddFriendUiState.Success
                        searchResults = sortSearchResults(searchResults.map { user ->
                            if (user.email.equals(normalizedEmail, ignoreCase = true)) {
                                user.copy(relationship = UserRelationship.PENDING)
                            } else {
                                user
                            }
                        })
                        _uiEvent.emit(FriendUiEvent.FriendRequestSentSuccessfully)
                    }
                    .onFailure { e ->
                        val errorText = when (e) {
                            is InfrastructureException ->
                                if (e.messageRes != null) UiText.StringResource(e.messageRes)
                                else UiText.StringResource(R.string.error_infrastructure)

                            is SocialException ->
                                if (e.messageRes != null) UiText.StringResource(e.messageRes)
                                else UiText.StringResource(R.string.error_social)

                            else -> UiText.StringResource(R.string.error_sending_friend_request)
                        }
                        uiState = AddFriendUiState.Error(errorText)
                        _uiEvent.emit(FriendUiEvent.ShowToast(errorText))
                    }
            } finally {
                isSendingRequest = false
            }
        }
    }

    fun acceptFriendRequest(requestId: String, friendEmail: String? = null) {
        isSendingRequest = true
        viewModelScope.launch {
            try {
                socialRepository.updateFriendRequestStatus(requestId, FriendRequestStatus.ACCEPTED)
                    .onSuccess {
                        searchResults = sortSearchResults(searchResults.map { user ->
                            if (user.requestId == requestId || (friendEmail != null && user.email.equals(friendEmail, ignoreCase = true))) {
                                user.copy(relationship = UserRelationship.FRIEND)
                            } else {
                                user
                            }
                        })
                        _uiEvent.emit(FriendUiEvent.ShowToast(UiText.StringResource(R.string.toast_friend_request_accepted)))
                    }
                    .onFailure {
                        _uiEvent.emit(FriendUiEvent.ShowToast(UiText.StringResource(R.string.error_database_operation)))
                    }
            } finally {
                isSendingRequest = false
            }
        }
    }

    fun declineFriendRequest(requestId: String, friendEmail: String? = null) {
        isSendingRequest = true
        viewModelScope.launch {
            try {
                socialRepository.updateFriendRequestStatus(requestId, FriendRequestStatus.DECLINED)
                    .onSuccess {
                        searchResults = sortSearchResults(searchResults.map { user ->
                            if (user.requestId == requestId || (friendEmail != null && user.email.equals(friendEmail, ignoreCase = true))) {
                                user.copy(relationship = UserRelationship.NONE)
                            } else {
                                user
                            }
                        })
                        _uiEvent.emit(FriendUiEvent.ShowToast(UiText.StringResource(R.string.toast_friend_request_declined)))
                    }
                    .onFailure {
                        _uiEvent.emit(FriendUiEvent.ShowToast(UiText.StringResource(R.string.error_database_operation)))
                    }
            } finally {
                isSendingRequest = false
            }
        }
    }

    fun resetState() {
        uiState = AddFriendUiState.Idle
        searchResults = emptyList()
        isSearchingUsers = false
        searchJob?.cancel()
    }

    private fun sortSearchResults(list: List<UserSearchResult>): List<UserSearchResult> {
        return list.sortedWith(
            compareByDescending { user ->
                when (user.relationship) {
                    UserRelationship.PENDING_INCOMING -> 4
                    UserRelationship.PENDING -> 3
                    UserRelationship.FRIEND -> 2
                    UserRelationship.NONE -> 1
                    UserRelationship.BLOCKED -> 0
                }
            }
        )
    }
}

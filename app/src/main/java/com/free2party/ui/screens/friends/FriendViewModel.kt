package com.free2party.ui.screens.friends

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.UserSearchResult
import com.free2party.data.repository.SocialRepository
import com.free2party.exception.InfrastructureException
import com.free2party.exception.SocialException
import com.free2party.R
import com.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
        viewModelScope.launch {
            socialRepository.sendFriendRequest(normalizedEmail)
                .onSuccess {
                    uiState = AddFriendUiState.Success
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
        }
    }

    fun resetState() {
        uiState = AddFriendUiState.Idle
        searchResults = emptyList()
        isSearchingUsers = false
        searchJob?.cancel()
    }
}

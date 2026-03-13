package com.example.free2party.ui.screens.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.repository.AuthRepository
import com.example.free2party.data.repository.AuthRepositoryImpl
import com.example.free2party.data.repository.PlanRepositoryImpl
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.SocialRepositoryImpl
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.exception.InfrastructureException
import com.example.free2party.exception.SocialException
import com.example.free2party.exception.UserNotFoundException
import com.example.free2party.util.UiText
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val userName: String = "",
        val profilePicUrl: String = "",
        val isUserFree: Boolean = false,
        val use24HourFormat: Boolean = true,
        val friendsList: List<FriendInfo> = emptyList(),
        val isActionLoading: Boolean = false
    ) : HomeUiState

    data class Error(val message: UiText) : HomeUiState
}

sealed class HomeUiEvent {
    data class ShowToast(val message: UiText) : HomeUiEvent()
    object Logout : HomeUiEvent()
}

class HomeViewModel(
    private val userRepository: UserRepository = UserRepositoryImpl(
        auth = Firebase.auth,
        db = Firebase.firestore,
        storage = Firebase.storage
    ),
    private val socialRepository: SocialRepository = SocialRepositoryImpl(
        db = Firebase.firestore,
        userRepository = userRepository,
        planRepository = PlanRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore
        )
    ),
    private val authRepository: AuthRepository = AuthRepositoryImpl(
        auth = Firebase.auth,
        userRepository = userRepository
    )
) : ViewModel() {

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Loading)
        private set

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeData()
    }

    private fun observeData() {
        combine(
            userRepository.observeUser(userRepository.currentUserId),
            socialRepository.getFriendsList()
        ) { user, friends ->
            // Sort: Invited at the bottom, then by availability, then alphabetically
            val sortedFriends = friends.sortedWith(
                compareBy<FriendInfo> { it.inviteStatus == InviteStatus.INVITED }
                    .thenByDescending { it.isFreeNow }
                    .thenBy { it.name }
            )
            HomeUiState.Success(
                userName = user.firstName,
                profilePicUrl = user.profilePicUrl,
                isUserFree = user.isFreeNow,
                use24HourFormat = user.settings.use24HourFormat,
                friendsList = sortedFriends
            )
        }.onEach { newState ->
            uiState = newState
        }.catch { e ->
            Log.e("HomeViewModel", "Error observing data", e)
            if (e is UserNotFoundException) {
                authRepository.logout()
                _uiEvent.emit(HomeUiEvent.Logout)
            } else {
                val errorText = when (e) {
                    is InfrastructureException if e.messageRes != null -> UiText.StringResource(e.messageRes)
                    is SocialException if e.messageRes != null -> UiText.StringResource(e.messageRes)
                    else -> UiText.DynamicString(e.localizedMessage ?: "An error occurred")
                }

                if (errorText is UiText.DynamicString && errorText.value.contains("permission", ignoreCase = true)) {
                    Log.w("HomeViewModel", "Permission error handled: ${errorText.value}")
                } else {
                    uiState = HomeUiState.Error(errorText)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        authRepository.logout()
        onLogoutSuccess()
    }

    fun toggleAvailability() {
        val currentState = uiState as? HomeUiState.Success ?: return

        uiState = currentState.copy(isActionLoading = true)
        viewModelScope.launch {
            userRepository.toggleAvailability(!currentState.isUserFree)
                .onSuccess {
                    Log.d("HomeViewModel", "Availability updated successfully")
                }
                .onFailure { e ->
                    Log.e("HomeViewModel", "Error updating availability", e)
                    val errorText = when {
                        e is InfrastructureException && e.messageRes != null -> UiText.StringResource(e.messageRes)
                        else -> UiText.DynamicString(e.localizedMessage ?: "Error updating availability")
                    }
                    _uiEvent.emit(HomeUiEvent.ShowToast(errorText))
                }
        }
    }

    fun removeFriend(friendUid: String) {
        viewModelScope.launch {
            socialRepository.removeFriend(friendUid)
                .onSuccess {
                    Log.d("HomeViewModel", "Friend removed successfully")
                    _uiEvent.emit(HomeUiEvent.ShowToast(UiText.DynamicString("Friend removed successfully")))
                }
                .onFailure { e ->
                    Log.e("HomeViewModel", "Error removing friend", e)
                    val errorText = when (e) {
                        is InfrastructureException if e.messageRes != null -> UiText.StringResource(e.messageRes)
                        is SocialException if e.messageRes != null -> UiText.StringResource(e.messageRes)
                        else -> UiText.DynamicString(e.localizedMessage ?: "Error removing friend")
                    }
                    _uiEvent.emit(HomeUiEvent.ShowToast(errorText))
                }
        }
    }

    fun cancelFriendInvite(friendUid: String) {
        viewModelScope.launch {
            socialRepository.cancelFriendRequest(friendUid)
                .onSuccess {
                    Log.d("HomeViewModel", "Invite cancelled successfully")
                    _uiEvent.emit(HomeUiEvent.ShowToast(UiText.DynamicString("Invite cancelled successfully")))
                }
                .onFailure { e ->
                    Log.e("HomeViewModel", "Error cancelling invite", e)
                    val errorText = when (e) {
                        is InfrastructureException if e.messageRes != null -> UiText.StringResource(e.messageRes)
                        is SocialException if e.messageRes != null -> UiText.StringResource(e.messageRes)
                        else -> UiText.DynamicString(e.localizedMessage ?: "Error cancelling invite")
                    }
                    _uiEvent.emit(HomeUiEvent.ShowToast(errorText))
                }
        }
    }
}

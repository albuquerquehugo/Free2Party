package com.example.free2party.ui.screens.blocked

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.free2party.R
import com.example.free2party.data.model.BlockedUser
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.SocialRepositoryImpl
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.exception.InfrastructureException
import com.example.free2party.exception.SocialException
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.exception.UserNotFoundException
import com.example.free2party.util.UiText
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface BlockedUsersUiState {
    object Loading : BlockedUsersUiState
    data class Success(val blockedUsers: List<BlockedUser>) : BlockedUsersUiState
    data class Error(val message: UiText) : BlockedUsersUiState
}

sealed class BlockedUsersUiEvent {
    data class ShowToast(val message: UiText) : BlockedUsersUiEvent()
}

class BlockedUsersViewModel(
    private val userRepository: UserRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {
    var uiState by mutableStateOf<BlockedUsersUiState>(BlockedUsersUiState.Loading)
        private set

    private val _uiEvent = MutableSharedFlow<BlockedUsersUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    var gradientBackground by mutableStateOf(true)
        private set

    init {
        loadGradientBackground()
        loadBlockedUsers()
    }

    private fun loadGradientBackground() {
        viewModelScope.launch {
            userRepository.observeUser(userRepository.currentUserId)
                .catch { }
                .collect { user ->
                    gradientBackground = user.settings.gradientBackground
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadBlockedUsers() {
        viewModelScope.launch {
            socialRepository.getBlockedUsers()
                .flatMapLatest { blockedStubs ->
                    if (blockedStubs.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val flows = blockedStubs.map { stub ->
                            userRepository.observeUser(stub.uid).map { user ->
                                stub.copy(
                                    name = user.fullName,
                                    email = user.email,
                                    profilePicUrl = user.profilePicUrl
                                )
                            }.catch { emit(stub) }
                        }
                        combine(flows) { it.toList() }
                    }
                }
                .catch { e ->
                    if (e is UserNotFoundException || e is UnauthorizedException) {
                        return@catch
                    }
                    uiState = BlockedUsersUiState.Error(
                        mapToUiText(e, R.string.error_database_operation)
                    )
                }
                .collect { blockedUsers ->
                    uiState = BlockedUsersUiState.Success(blockedUsers)
                }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            socialRepository.unblockUser(userId)
                .onSuccess {
                    _uiEvent.emit(
                        BlockedUsersUiEvent.ShowToast(
                            UiText.StringResource(R.string.toast_user_unblocked)
                        )
                    )
                }
                .onFailure { e ->
                    _uiEvent.emit(
                        BlockedUsersUiEvent.ShowToast(
                            mapToUiText(e, R.string.error_unblocking_user)
                        )
                    )
                }
        }
    }

    private fun mapToUiText(e: Throwable, defaultRes: Int): UiText {
        return when (e) {
            is InfrastructureException -> e.messageRes?.let { UiText.StringResource(it) }
                ?: UiText.StringResource(R.string.error_infrastructure)

            is SocialException -> e.messageRes?.let { UiText.StringResource(it) }
                ?: UiText.StringResource(R.string.error_social)

            else -> UiText.StringResource(defaultRes)
        }
    }

    companion object {
        fun provideFactory(
            context: Context,
            userRepository: UserRepository = UserRepositoryImpl(
                auth = Firebase.auth,
                db = Firebase.firestore,
                storage = Firebase.storage
            ),
            socialRepository: SocialRepository = SocialRepositoryImpl(
                db = Firebase.firestore,
                userRepository = userRepository,
                context = context
            )
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BlockedUsersViewModel(userRepository, socialRepository) as T
            }
        }
    }
}

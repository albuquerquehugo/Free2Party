package com.free2party.ui.screens.profile

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.R
import com.free2party.data.model.User
import com.free2party.data.repository.UserRepository
import com.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface InterestsUiState {
    object Loading : InterestsUiState
    data class Success(
        val user: User,
        val isSaving: Boolean = false
    ) : InterestsUiState

    data class Error(val message: UiText) : InterestsUiState
}

sealed class InterestsUiEvent {
    data class ShowToast(
        val message: UiText,
        val navigateBack: Boolean = false
    ) : InterestsUiEvent()
}

@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState by mutableStateOf<InterestsUiState>(InterestsUiState.Loading)
        private set

    private val _uiEvent = MutableSharedFlow<InterestsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var currentUser: User? = null

    var selectedInterests by mutableStateOf<Set<String>>(emptySet())
    var initialInterests by mutableStateOf<Set<String>>(emptySet())
        private set

    val hasChanges: Boolean
        get() = selectedInterests != initialInterests

    val gradientBackground: Boolean
        get() = currentUser?.settings?.gradientBackground ?: true

    init {
        observeUserData()
    }

    private fun observeUserData() {
        viewModelScope.launch {
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                userRepository.observeUser(uid)
                    .catch { e ->
                        Log.e("InterestsViewModel", "Error observing user data", e)
                        uiState =
                            InterestsUiState.Error(UiText.StringResource(R.string.error_unknown))
                    }
                    .collectLatest { user ->
                        currentUser = user
                        val state = uiState
                        if (state !is InterestsUiState.Success || !state.isSaving) {
                            initialInterests = user.interests.toSet()
                            selectedInterests = user.interests.toSet()
                            uiState = InterestsUiState.Success(user = user)
                        }
                    }
            } else {
                uiState = InterestsUiState.Error(UiText.StringResource(R.string.error_unauthorized))
            }
        }
    }

    fun toggleInterest(interestId: String) {
        selectedInterests = if (selectedInterests.contains(interestId)) {
            selectedInterests - interestId
        } else {
            selectedInterests + interestId
        }
    }

    fun discardChanges() {
        selectedInterests = initialInterests
    }

    fun saveInterests() {
        val user = currentUser ?: return
        val successState = uiState as? InterestsUiState.Success ?: return

        viewModelScope.launch {
            uiState = successState.copy(isSaving = true)
            val updatedUser = user.copy(interests = selectedInterests.toList())

            userRepository.updateUser(updatedUser)
                .onSuccess {
                    uiState = successState.copy(isSaving = false)
                    initialInterests = selectedInterests
                    _uiEvent.emit(
                        InterestsUiEvent.ShowToast(
                            message = UiText.StringResource(R.string.toast_interests_updated),
                            navigateBack = true
                        )
                    )
                }
                .onFailure { error ->
                    Log.e("InterestsViewModel", "Error saving interests", error)
                    uiState = successState.copy(isSaving = false)
                    _uiEvent.emit(
                        InterestsUiEvent.ShowToast(
                            message = UiText.StringResource(R.string.error_updating_profile)
                        )
                    )
                }
        }
    }
}

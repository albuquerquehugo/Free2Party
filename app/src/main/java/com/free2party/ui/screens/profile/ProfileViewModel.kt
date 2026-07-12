package com.free2party.ui.screens.profile

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.Gender
import com.free2party.data.model.Membership
import com.free2party.data.repository.AuthRepository
import com.free2party.data.repository.PlanRepository
import com.free2party.data.repository.UserRepository
import com.free2party.R
import com.free2party.util.UiText
import com.free2party.util.isPlanActive
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(
        val userName: String = "",
        val userFullName: String = "",
        val userGender: Gender = Gender.OTHER,
        val profilePicUrl: String = "",
        val isUserFree: Boolean = false,
        val isStatusFromPlan: Boolean = false,
        val gradientBackground: Boolean = true,
        val membership: Membership = Membership.FREE
    ) : ProfileUiState

    data class Error(val message: UiText) : ProfileUiState
}

sealed class ProfileUiEvent {
    object Logout : ProfileUiEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf<ProfileUiState>(ProfileUiState.Loading)
        private set

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeUserData()
    }

    private fun observeUserData() {
        viewModelScope.launch {
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                combine(
                    userRepository.observeUser(uid),
                    planRepository.getOwnPlans()
                ) { user, plans ->
                    val isAnyPlanActiveNow = plans.any { isPlanActive(it) }
                    val effectiveIsFree =
                        if (user.isStatusFromPlan) isAnyPlanActiveNow else user.isFreeNow
                    val effectiveFromPlan = user.isStatusFromPlan && isAnyPlanActiveNow

                    ProfileUiState.Success(
                        userName = user.firstName,
                        userFullName = user.fullName,
                        userGender = user.gender,
                        profilePicUrl = user.profilePicUrl,
                        isUserFree = effectiveIsFree,
                        isStatusFromPlan = effectiveFromPlan,
                        gradientBackground = user.settings.gradientBackground,
                        membership = user.membership
                    )
                }
                    .catch { e ->
                        Log.e("ProfileViewModel", "Error observing user data", e)
                        uiState =
                            ProfileUiState.Error(UiText.StringResource(R.string.error_unknown))
                    }
                    .collectLatest { newState ->
                        uiState = newState
                    }
            } else {
                uiState = ProfileUiState.Error(UiText.StringResource(R.string.error_unknown))
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutSuccess()
            _uiEvent.emit(ProfileUiEvent.Logout)
        }
    }
}

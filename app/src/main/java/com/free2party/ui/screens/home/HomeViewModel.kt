package com.free2party.ui.screens.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.Gender
import com.free2party.data.model.Membership
import com.free2party.data.model.InviteStatus
import com.free2party.data.model.FriendInfo
import com.free2party.data.repository.PlanRepository
import com.free2party.data.repository.SocialRepository
import com.free2party.data.repository.UserRepository
import com.free2party.exception.InfrastructureException
import com.free2party.exception.SocialException
import com.free2party.exception.UnauthorizedException
import com.free2party.R
import com.free2party.util.UiText
import com.free2party.util.isPlanActive
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val userName: String = "",
        val userGender: Gender = Gender.OTHER,
        val profilePicUrl: String = "",
        val isUserFree: Boolean = false,
        val isStatusFromPlan: Boolean = false,
        val isWithinPlanPeriod: Boolean = false,
        val use24HourFormat: Boolean = true,
        val gradientBackground: Boolean = true,
        val friendsList: List<FriendInfo> = emptyList(),
        val isActionLoading: Boolean = false,
        val membership: Membership = Membership.FREE
    ) : HomeUiState

    data class Error(val message: UiText) : HomeUiState
}

sealed class HomeUiEvent {
    data class ShowToast(val message: UiText) : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val socialRepository: SocialRepository,
    private val planRepository: PlanRepository
) : ViewModel() {

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Loading)
        private set

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeData() {
        userRepository.userIdFlow
            .filter { it.isNotBlank() }
            .flatMapLatest { uid ->
                combine(
                    userRepository.observeUser(uid),
                    socialRepository.getFriendsList(),
                    socialRepository.getOutgoingFriendRequests(),
                    planRepository.getOwnPlans()
                ) { user, friends, outgoingRequests, plans ->
                    val isAnyPlanActiveNow = plans.any { isPlanActive(it) }
                    val effectiveIsFree =
                        if (user.isStatusFromPlan) isAnyPlanActiveNow else user.isFreeNow
                    val effectiveFromPlan = user.isStatusFromPlan && isAnyPlanActiveNow

                    // Map outgoing requests to FriendInfo with PENDING status
                    val pendingFriends = outgoingRequests
                        .filter { it.receiverName.isNotBlank() }
                        .map { request ->
                            FriendInfo(
                                uid = request.receiverId,
                                name = request.receiverName,
                                profilePicUrl = request.receiverProfilePicUrl,
                                inviteStatus = InviteStatus.PENDING
                            )
                        }

                    // Combine confirmed friends and pending ones, filtering out the current user
                    val allFriends = (friends + pendingFriends)
                        .filter { it.uid != uid }
                        .distinctBy { it.uid }

                    // Sort: Pending at the bottom, then by availability, then alphabetically
                    val sortedFriends = allFriends.sortedWith(
                        compareBy<FriendInfo> { it.inviteStatus == InviteStatus.PENDING }
                            .thenByDescending { it.isFreeNow }
                            .thenBy { it.name }
                    )
                    HomeUiState.Success(
                        userName = user.firstName,
                        userGender = user.gender,
                        profilePicUrl = user.profilePicUrl,
                        isUserFree = effectiveIsFree,
                        isStatusFromPlan = effectiveFromPlan,
                        isWithinPlanPeriod = isAnyPlanActiveNow,
                        use24HourFormat = user.settings.use24HourFormat,
                        gradientBackground = user.settings.gradientBackground,
                        friendsList = sortedFriends,
                        membership = user.membership
                    )
                }
            }
            .onEach { newState ->
                uiState = newState
            }
            .catch { e ->
                Log.e("HomeViewModel", "Error observing data", e)

                val errorText = when (e) {
                    is UnauthorizedException -> UiText.StringResource(R.string.error_unauthorized)
                    is InfrastructureException ->
                        if (e.messageRes != null) UiText.StringResource(e.messageRes)
                        else UiText.StringResource(R.string.error_infrastructure)

                    is SocialException ->
                        if (e.messageRes != null) UiText.StringResource(e.messageRes)
                        else UiText.StringResource(R.string.error_social)

                    else -> UiText.StringResource(R.string.error_unknown)
                }
                uiState = HomeUiState.Error(errorText)
            }
            .launchIn(viewModelScope)
    }

    fun toggleAvailability() {
        val currentState = uiState as? HomeUiState.Success ?: return
        val isCurrentlyFree = currentState.isUserFree
        val isAnyPlanActiveNow = currentState.isWithinPlanPeriod

        val (nextIsFree, fromPlan) = if (isCurrentlyFree) {
            // Transitions to Busy
            if (isAnyPlanActiveNow) {
                // Manually overriding an active plan to be busy
                false to false
            } else {
                // Was manually free, now returning to automatic (which is busy since no plan active)
                false to true
            }
        } else {
            // Transitions to Free
            if (isAnyPlanActiveNow) {
                // Returning to automatic (which is free since a plan is active)
                true to true
            } else {
                // Manually setting to free
                true to false
            }
        }

        uiState = currentState.copy(isActionLoading = true)
        viewModelScope.launch {
            userRepository.toggleAvailability(nextIsFree, fromPlan = fromPlan)
                .onSuccess {
                    Log.d(
                        "HomeViewModel",
                        "Availability updated successfully: isFree=$nextIsFree, fromPlan=$fromPlan"
                    )
                }
                .onFailure { e ->
                    Log.e("HomeViewModel", "Error updating availability", e)
                    val errorText = when {
                        e is InfrastructureException &&
                                e.messageRes != null -> UiText.StringResource(e.messageRes)

                        else -> UiText.StringResource(R.string.error_updating_availability)
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
                    _uiEvent.emit(HomeUiEvent.ShowToast(UiText.StringResource(R.string.toast_friend_removed)))
                }
                .onFailure { e ->
                    Log.e("HomeViewModel", "Error removing friend", e)
                    val errorText = when (e) {
                        is InfrastructureException ->
                            if (e.messageRes != null) UiText.StringResource(e.messageRes)
                            else UiText.StringResource(R.string.error_infrastructure)

                        is SocialException ->
                            if (e.messageRes != null) UiText.StringResource(e.messageRes)
                            else UiText.StringResource(R.string.error_social)

                        else -> UiText.StringResource(R.string.error_removing_friend)
                    }
                    _uiEvent.emit(HomeUiEvent.ShowToast(errorText))
                }
        }
    }

    fun removeAndBlockFriend(friendUid: String) {
        viewModelScope.launch {
            socialRepository.removeAndBlockFriend(friendUid)
                .onSuccess {
                    Log.d("HomeViewModel", "Friend removed and blocked successfully")
                    _uiEvent.emit(HomeUiEvent.ShowToast(UiText.StringResource(R.string.toast_friend_removed)))
                }
                .onFailure { e ->
                    Log.e("HomeViewModel", "Error removing and blocking friend", e)
                    val errorText = when (e) {
                        is InfrastructureException ->
                            if (e.messageRes != null) UiText.StringResource(e.messageRes)
                            else UiText.StringResource(R.string.error_infrastructure)

                        is SocialException ->
                            if (e.messageRes != null) UiText.StringResource(e.messageRes)
                            else UiText.StringResource(R.string.error_social)

                        else -> UiText.StringResource(R.string.error_removing_friend)
                    }
                    _uiEvent.emit(HomeUiEvent.ShowToast(errorText))
                }
        }
    }

    fun reportUser(userId: String, reason: String) {
        viewModelScope.launch {
            socialRepository.reportUser(userId, reason)
                .onSuccess {
                    socialRepository.removeAndBlockFriend(userId)
                        .onSuccess {
                            _uiEvent.emit(
                                HomeUiEvent.ShowToast(
                                    UiText.StringResource(R.string.toast_user_reported)
                                )
                            )
                        }
                        .onFailure { e ->
                            _uiEvent.emit(
                                HomeUiEvent.ShowToast(
                                    mapToUiText(
                                        e,
                                        R.string.error_reporting_user
                                    )
                                )
                            )
                        }
                }
                .onFailure { e ->
                    _uiEvent.emit(
                        HomeUiEvent.ShowToast(
                            mapToUiText(
                                e,
                                R.string.error_reporting_user
                            )
                        )
                    )
                }
        }
    }

    private fun mapToUiText(e: Throwable, defaultResId: Int): UiText {
        return when (e) {
            is InfrastructureException ->
                if (e.messageRes != null) UiText.StringResource(e.messageRes)
                else UiText.StringResource(R.string.error_infrastructure)

            is SocialException ->
                if (e.messageRes != null) UiText.StringResource(e.messageRes)
                else UiText.StringResource(R.string.error_social)

            else -> UiText.StringResource(defaultResId)
        }
    }

    fun cancelFriendInvite(friendUid: String) {
        viewModelScope.launch {
            socialRepository.cancelFriendRequest(friendUid)
                .onSuccess {
                    Log.d("HomeViewModel", "Invite cancelled successfully")
                    _uiEvent.emit(HomeUiEvent.ShowToast(UiText.StringResource(R.string.toast_invite_cancelled)))
                }
                .onFailure { e ->
                    Log.e("HomeViewModel", "Error cancelling invite", e)
                    val errorText = when (e) {
                        is InfrastructureException ->
                            if (e.messageRes != null) UiText.StringResource(e.messageRes)
                            else UiText.StringResource(R.string.error_infrastructure)

                        is SocialException ->
                            if (e.messageRes != null) UiText.StringResource(e.messageRes)
                            else UiText.StringResource(R.string.error_social)

                        else -> UiText.StringResource(R.string.error_cancelling_invite)
                    }
                    _uiEvent.emit(HomeUiEvent.ShowToast(errorText))
                }
        }
    }

}

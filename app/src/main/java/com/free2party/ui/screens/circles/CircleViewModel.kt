package com.free2party.ui.screens.circles

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.Circle
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.Membership
import com.free2party.data.repository.SettingsRepository
import com.free2party.data.repository.SocialRepository
import com.free2party.data.repository.UserRepository
import com.free2party.R
import com.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CircleUiEvent {
    data class ShowToast(val message: UiText) : CircleUiEvent()
    data class CircleActionSuccess(val circleId: String? = null) : CircleUiEvent()
}

@HiltViewModel
class CircleViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val membership: StateFlow<Membership> = userRepository.userIdFlow
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(Membership.FREE)
            else userRepository.observeUser(uid).map { it.membership }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Membership.FREE)

    var circles by mutableStateOf<List<Circle>>(emptyList())
        private set

    val friendsList: StateFlow<List<FriendInfo>> = socialRepository.getFriendsList()
        .catch { e -> Log.e("CircleViewModel", "Error in friendsList flow", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedCircleId by mutableStateOf<String?>(null)
        private set

    var isActionLoading by mutableStateOf(false)
        private set

    var gradientBackground by mutableStateOf(true)
        private set

    private val _uiEvent = MutableSharedFlow<CircleUiEvent>(extraBufferCapacity = 10)
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeCircles()
        observeLastUsedFilter()
        observeUserSettings()
    }

    private fun observeUserSettings() {
        viewModelScope.launch {
            settingsRepository.gradientBackgroundFlow.collect {
                gradientBackground = it
            }
        }
    }

    private fun observeCircles() {
        socialRepository.getCircles()
            .onEach { circles = it }
            .catch { /* Handle error */ }
            .launchIn(viewModelScope)
    }

    private fun observeLastUsedFilter() {
        viewModelScope.launch {
            settingsRepository.lastUsedCircleIdFlow.collect { id ->
                selectedCircleId = id
            }
        }
    }

    fun updateSelectedCircleId(id: String?) {
        viewModelScope.launch {
            settingsRepository.setLastUsedCircleId(id)
        }
    }

    fun createCircle(name: String, selectedFriends: List<String>) {
        if (name.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_required_fields)))
            }
            return
        }

        if (membership.value == Membership.FREE && circles.size >= MAX_FREE_CIRCLES) {
            viewModelScope.launch {
                _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_circle_creation_limit)))
            }
            return
        }

        viewModelScope.launch {
            isActionLoading = true
            try {
                socialRepository.createCircle(name, selectedFriends)
                    .onSuccess { circleId ->
                        isActionLoading = false
                        _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.toast_circle_created)))
                        _uiEvent.emit(CircleUiEvent.CircleActionSuccess(circleId))
                    }
                    .onFailure { _ ->
                        isActionLoading = false
                        _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_database_operation)))
                    }
            } catch (_: Exception) {
                isActionLoading = false
                _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_database_operation)))
            }
        }
    }

    fun updateCircle(circleId: String, name: String, selectedFriends: List<String>) {
        if (name.isBlank()) return

        val allowedCircleIds = circles.sortedWith(
            compareBy<Circle> { it.createdAt ?: java.util.Date() }
                .thenBy { it.name }
        ).take(MAX_FREE_CIRCLES).map { it.id }.toSet()

        if (membership.value == Membership.FREE && circleId !in allowedCircleIds) {
            viewModelScope.launch {
                _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_circle_creation_limit)))
            }
            return
        }

        viewModelScope.launch {
            isActionLoading = true
            try {
                socialRepository.updateCircle(circleId, name, selectedFriends)
                    .onSuccess {
                        isActionLoading = false
                        _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.toast_circle_updated)))
                        _uiEvent.emit(CircleUiEvent.CircleActionSuccess(circleId))
                    }
                    .onFailure { _ ->
                        isActionLoading = false
                        _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_database_operation)))
                    }
            } catch (_: Exception) {
                isActionLoading = false
                _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_database_operation)))
            }
        }
    }

    fun deleteCircle(circleId: String) {
        viewModelScope.launch {
            isActionLoading = true
            try {
                socialRepository.deleteCircle(circleId)
                    .onSuccess {
                        isActionLoading = false
                        _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.toast_circle_deleted)))
                        _uiEvent.emit(CircleUiEvent.CircleActionSuccess(null))
                    }
                    .onFailure { _ ->
                        isActionLoading = false
                        _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_database_operation)))
                    }
            } catch (_: Exception) {
                isActionLoading = false
                _uiEvent.emit(CircleUiEvent.ShowToast(UiText.StringResource(R.string.error_database_operation)))
            }
        }
    }

    companion object {
        const val MAX_FREE_CIRCLES = 3
    }
}

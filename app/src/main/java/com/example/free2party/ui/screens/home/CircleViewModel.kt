package com.example.free2party.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.R
import com.example.free2party.data.model.Circle
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed class CircleUiEvent {
    data class ShowToast(val message: UiText) : CircleUiEvent()
    data class CircleActionSuccess(val circleId: String? = null) : CircleUiEvent()
}

@HiltViewModel
class CircleViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val settingsRepository: com.example.free2party.data.repository.SettingsRepository
) : ViewModel() {

    var circles by mutableStateOf<List<Circle>>(emptyList())
        private set

    var selectedCircleId by mutableStateOf<String?>(null)
        private set

    var isActionLoading by mutableStateOf(false)
        private set

    private val _uiEvent = MutableSharedFlow<CircleUiEvent>(extraBufferCapacity = 10)
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeCircles()
        observeLastUsedFilter()
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
}

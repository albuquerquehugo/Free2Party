package com.example.free2party.ui.screens.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.R
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.model.Notification
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class NotificationItem {
    data class Request(val friendRequest: FriendRequest) : NotificationItem()
    data class Info(val notification: Notification) : NotificationItem()

    val timestamp: java.util.Date?
        get() = when (this) {
            is Request -> friendRequest.timestamp
            is Info -> notification.timestamp
        }

    val id: String
        get() = when (this) {
            is Request -> friendRequest.id
            is Info -> notification.id
        }
}

sealed class NotificationsUiEvent {
    data class ShowToast(val message: UiText) : NotificationsUiEvent()
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private var observationJob: Job? = null

    val gradientBackground: StateFlow<Boolean> = settingsRepository.gradientBackgroundFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())

    private val _uiEvent = MutableSharedFlow<NotificationsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val itemsUnreadCount: StateFlow<Int> = _notifications
        .onEach { notificationList ->
            Log.d(
                "NotificationsViewModel",
                "Unread count update: ${notificationList.count { !it.isRead }}"
            )
        }
        .combine(_friendRequests) { notifications, requests ->
            notifications.count { !it.isRead } + requests.size
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationsUnreadCount: StateFlow<Int> = _notifications
        .map { notifications -> notifications.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationItems: StateFlow<List<NotificationItem>> = combine(
        _notifications,
        _friendRequests
    ) { notifications, requests ->
        val items = notifications.map { NotificationItem.Info(it) } +
                requests.map { NotificationItem.Request(it) }
        items.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            userRepository.userIdFlow.collectLatest { uid ->
                if (uid.isNotBlank()) {
                    listenToData()
                } else {
                    observationJob?.cancel()
                    _friendRequests.value = emptyList()
                    _notifications.value = emptyList()
                }
            }
        }
    }

    private fun listenToData() {
        observationJob?.cancel()
        observationJob = combine(
            socialRepository.getIncomingFriendRequests(),
            socialRepository.getNotifications()
        ) { requests, notifications ->
            _friendRequests.value = requests
            _notifications.value = notifications
        }.catch { e ->
            Log.e("NotificationsViewModel", "Error observing notifications", e)
        }.launchIn(viewModelScope)
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.updateFriendRequestStatus(requestId, FriendRequestStatus.ACCEPTED)
                .onSuccess {
                    _uiEvent.emit(NotificationsUiEvent.ShowToast(UiText.StringResource(R.string.toast_friend_request_accepted)))
                }
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.updateFriendRequestStatus(requestId, FriendRequestStatus.DECLINED)
                .onSuccess {
                    _uiEvent.emit(NotificationsUiEvent.ShowToast(UiText.StringResource(R.string.toast_friend_request_declined)))
                }
        }
    }

    fun declineAndBlockFriendRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.declineAndBlockFriendRequest(requestId)
                .onSuccess {
                    _uiEvent.emit(NotificationsUiEvent.ShowToast(UiText.StringResource(R.string.toast_friend_request_declined_and_user_blocked)))
                }
        }
    }

    fun toggleReadStatus(notification: Notification) {
        viewModelScope.launch {
            if (notification.isRead) {
                socialRepository.markNotificationAsUnread(notification.id)
            } else {
                socialRepository.markNotificationAsRead(notification.id)
            }
        }
    }

    fun markAllAsRead() {
        val unreadIds = _notifications.value
            .filter { !it.isRead }
            .map { it.id }

        if (unreadIds.isNotEmpty()) {
            viewModelScope.launch {
                socialRepository.markNotificationsAsRead(unreadIds)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            socialRepository.deleteNotification(notificationId)
        }
    }
}

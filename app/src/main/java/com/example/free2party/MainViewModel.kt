package com.example.free2party

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.model.Notification
import com.example.free2party.data.model.NotificationType
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.SocialRepositoryImpl
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.ui.navigation.Screen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    initialHandledNotificationId: String? = null
) : ViewModel() {

    var themeMode by mutableStateOf(ThemeMode.AUTOMATIC)
        private set

    var gradientBackground by mutableStateOf(true)
        private set

    val gradientBackgroundFlow = settingsRepository.gradientBackgroundFlow

    private val _navigateToRoute = MutableSharedFlow<String>()
    val navigateToRoute = _navigateToRoute.asSharedFlow()

    private val _systemNotificationToDisplay = Channel<Notification>(Channel.BUFFERED)
    val systemNotificationToDisplay = _systemNotificationToDisplay.receiveAsFlow()

    private val _systemNotificationToDismiss = Channel<String>(Channel.BUFFERED)
    val systemNotificationToDismiss = _systemNotificationToDismiss.receiveAsFlow()

    private val activeSystemNotifications = MutableStateFlow(
        initialHandledNotificationId?.let { setOf(it) } ?: emptySet()
    )

    init {
        observeThemeMode()
        observeGradientBackground()

        // Immediate suppression for startup
        initialHandledNotificationId?.let { id ->
            Log.d("MainViewModel", "Suppressing initial notification: $id")
            viewModelScope.launch {
                settingsRepository.markNotificationAsShown(id)
            }
        }

        observeUserSettings()
        observeFriendRequests()
        observeNotifications()
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            settingsRepository.themeModeFlow.collectLatest { mode ->
                themeMode = mode
            }
        }
    }

    private fun observeGradientBackground() {
        viewModelScope.launch {
            settingsRepository.gradientBackgroundFlow.collectLatest { enabled ->
                gradientBackground = enabled
            }
        }
    }

    private fun observeUserSettings() {
        viewModelScope.launch {
            userRepository.userIdFlow.collectLatest { uid ->
                if (uid.isNotBlank()) {
                    // Initial Sync: Push local choices to Cloud (especially important after login)
                    val localTheme = settingsRepository.themeModeFlow.first()
                    val localBackground = settingsRepository.gradientBackgroundFlow.first()

                    val initialUser = userRepository.observeUser(uid).first()
                    if (initialUser.settings.themeMode != localTheme || initialUser.settings.gradientBackground != localBackground) {
                        Log.d("MainViewModel", "Pushing local settings to Cloud at login/startup")
                        val updatedUser = initialUser.copy(
                            settings = initialUser.settings.copy(
                                themeMode = localTheme,
                                gradientBackground = localBackground
                            )
                        )
                        userRepository.updateUser(updatedUser)
                    }

                    // Continuous Pull Sync: Only from Cloud to Local
                    userRepository.observeUser(uid).collectLatest { user ->
                        val currentTheme = settingsRepository.themeModeFlow.first()
                        val currentBackground = settingsRepository.gradientBackgroundFlow.first()

                        if (user.settings.themeMode != currentTheme) {
                            Log.d(
                                "MainViewModel",
                                "Syncing themeMode from Cloud: ${user.settings.themeMode}"
                            )
                            settingsRepository.setThemeMode(user.settings.themeMode)
                        }
                        if (user.settings.gradientBackground != currentBackground) {
                            Log.d(
                                "MainViewModel",
                                "Syncing gradientBackground from Cloud: ${user.settings.gradientBackground}"
                            )
                            settingsRepository.setGradientBackground(user.settings.gradientBackground)
                        }
                    }
                }
            }
        }
    }

    private fun observeFriendRequests() {
        viewModelScope.launch {
            userRepository.userIdFlow.collectLatest { uid ->
                if (uid.isNotBlank()) {
                    combine(
                        socialRepository.getIncomingFriendRequests(),
                        settingsRepository.shownNotificationIdsFlow,
                        activeSystemNotifications
                    ) { requests, shownIds, suppressedIds ->
                        Triple(requests, shownIds, suppressedIds)
                    }.collect { (requests, shownIds, suppressedIds) ->
                        val currentRequestIds = requests.map { it.id }.toSet()

                        val removedRequestIds = shownIds.filter { id ->
                            !currentRequestIds.contains(id) && id.contains("_")
                        }
                        removedRequestIds.forEach { id ->
                            settingsRepository.clearShownNotification(id)
                        }

                        val toDismiss = suppressedIds.filter { id ->
                            !currentRequestIds.contains(id) && id.contains("_")
                        }
                        toDismiss.forEach { id ->
                            _systemNotificationToDismiss.send(id)
                            activeSystemNotifications.update { it - id }
                        }

                        requests.forEach { request ->
                            if (request.friendRequestStatus == FriendRequestStatus.PENDING &&
                                !shownIds.contains(request.id) &&
                                !suppressedIds.contains(request.id)
                            ) {
                                Log.d("MainViewModel", "Triggering banner: ${request.id}")
                                activeSystemNotifications.update { it + request.id }
                                settingsRepository.markNotificationAsShown(request.id)
                                _systemNotificationToDisplay.send(
                                    Notification(
                                        id = request.id,
                                        title = "New Friend Request",
                                        message = "${request.senderName} (${request.senderEmail}) sent you a friend request!",
                                        type = NotificationType.FRIEND_REQUEST_RECEIVED
                                    )
                                )
                            }
                        }
                    }
                } else {
                    activeSystemNotifications.value = emptySet()
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            userRepository.userIdFlow.collectLatest { uid ->
                if (uid.isNotBlank()) {
                    combine(
                        socialRepository.getNotifications(),
                        settingsRepository.shownNotificationIdsFlow,
                        activeSystemNotifications
                    ) { notifications, shownIds, suppressedIds ->
                        Triple(notifications, shownIds, suppressedIds)
                    }.collect { (notifications, shownIds, suppressedIds) ->
                        val currentIds = notifications.map { it.id }.toSet()

                        val removedIds = shownIds.filter { id ->
                            !currentIds.contains(id) && !id.contains("_")
                        }
                        removedIds.forEach { id ->
                            settingsRepository.clearShownNotification(id)
                        }

                        // Dismiss if read or deleted
                        val toDismiss = suppressedIds.filter { id ->
                            !id.contains("_") &&
                                    (!currentIds.contains(id) || notifications.find { it.id == id }?.isRead == true)
                        }

                        toDismiss.forEach { id ->
                            Log.d("MainViewModel", "Dismissing system notification: $id")
                            _systemNotificationToDismiss.send(id)
                            activeSystemNotifications.update { it - id }
                        }

                        notifications.forEach { notification ->
                            if (notification.type == NotificationType.FRIEND_ADDED &&
                                !notification.isSilent &&
                                !notification.isRead &&
                                !shownIds.contains(notification.id) &&
                                !suppressedIds.contains(notification.id)
                            ) {
                                Log.d("MainViewModel", "Triggering banner: ${notification.id}")
                                activeSystemNotifications.update { it + notification.id }
                                settingsRepository.markNotificationAsShown(notification.id)
                                _systemNotificationToDisplay.send(notification)
                            }
                        }
                    }
                } else {
                    activeSystemNotifications.value = emptySet()
                }
            }
        }
    }

    fun onNotificationClicked(notificationId: String) {
        activeSystemNotifications.update { it + notificationId }

        viewModelScope.launch {
            settingsRepository.markNotificationAsShown(notificationId)

            Log.d("MainViewModel", "Notification clicked: $notificationId. Suppression updated.")

            val notifications = socialRepository.getNotifications().first()
            val notification = notifications.find { it.id == notificationId }
            if (notification != null) {
                if (!notification.isRead) {
                    socialRepository.markNotificationAsRead(notificationId)
                }
                _navigateToRoute.emit(Screen.Notifications.route)
                return@launch
            }

            val requests = socialRepository.getIncomingFriendRequests().first()
            val request = requests.find { it.id == notificationId }
            if (request != null) {
                _navigateToRoute.emit(Screen.Notifications.route)
            } else {
                _navigateToRoute.emit(Screen.Home.route)
            }
        }
    }

    companion object {
        fun provideFactory(
            context: Context,
            settingsRepository: SettingsRepository,
            initialHandledNotificationId: String? = null,
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
                return MainViewModel(
                    settingsRepository,
                    socialRepository,
                    userRepository,
                    initialHandledNotificationId
                ) as T
            }
        }
    }
}

package com.free2party

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.FriendRequestStatus
import com.free2party.data.model.Notification
import com.free2party.data.model.NotificationType
import com.free2party.data.model.ThemeMode
import com.free2party.data.model.Membership
import com.free2party.data.repository.PlanRepository
import com.free2party.data.repository.SettingsRepository
import com.free2party.data.repository.SocialRepository
import com.free2party.data.repository.UserRepository
import com.free2party.ui.navigation.Screen
import com.free2party.util.isPlanActive
import com.free2party.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class UserNavState(
    val profilePicUrl: String = "",
    val isUserFree: Boolean = false,
    val isStatusFromPlan: Boolean = false,
    val statusColor: String = "",
    val statusEmoji: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {
    private var initialHandledNotificationId: String? = null

    private val _userNavState = MutableStateFlow<UserNavState?>(null)
    val userNavState = _userNavState.asStateFlow()

    val isOnline = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setInitialNotificationId(id: String?) {
        initialHandledNotificationId = id
        if (id != null) {
            activeSystemNotifications.value = setOf(id)
        }
    }

    var themeMode by mutableStateOf(ThemeMode.AUTOMATIC)
        private set

    var gradientBackground by mutableStateOf(true)
        private set

    var gradientTheme by mutableStateOf("DEFAULT")
        private set

    val gradientBackgroundFlow = settingsRepository.gradientBackgroundFlow
    val gradientThemeFlow = settingsRepository.gradientThemeFlow
    val onboardingCompletedFlow = settingsRepository.onboardingCompletedFlow

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
        observeGradientTheme()

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
        observeUserNavState()
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

    private fun observeGradientTheme() {
        viewModelScope.launch {
            settingsRepository.gradientThemeFlow.collectLatest { theme ->
                gradientTheme = theme
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
                    val localThemeName = settingsRepository.gradientThemeFlow.first()
                    val localStatusColor = settingsRepository.statusColorFlow.first()

                    // Wait for user document to be created (max 10 seconds)
                    val initialUser = withTimeoutOrNull(10000.milliseconds) {
                        userRepository.observeUser(uid).first()
                    }

                    if (initialUser != null) {
                        var targetThemeName = localThemeName
                        var targetStatusColor = localStatusColor
                        if (initialUser.membership == Membership.FREE) {
                            if (localThemeName != "DEFAULT") {
                                targetThemeName = "DEFAULT"
                                settingsRepository.setGradientTheme("DEFAULT")
                            }
                            if (localStatusColor.isNotBlank()) {
                                targetStatusColor = ""
                                settingsRepository.setStatusColor("")
                            }
                        }

                        val targetEmoji = if (initialUser.membership == Membership.FREE) "" else initialUser.statusEmoji

                        if ((initialUser.settings.themeMode != localTheme) || 
                            (initialUser.settings.gradientBackground != localBackground) ||
                            (initialUser.settings.gradientTheme != targetThemeName) ||
                            (initialUser.settings.statusColor != targetStatusColor) ||
                            (initialUser.statusEmoji != targetEmoji)) {
                            Log.d(
                                "MainViewModel",
                                "Pushing local settings to Cloud at login/startup"
                            )
                            val updatedUser = initialUser.copy(
                                statusEmoji = targetEmoji,
                                settings = initialUser.settings.copy(
                                    themeMode = localTheme,
                                    gradientBackground = localBackground,
                                    gradientTheme = targetThemeName,
                                    statusColor = targetStatusColor
                                )
                            )
                            userRepository.updateUser(updatedUser)
                        }
                    } else {
                        Log.e("MainViewModel", "Timeout waiting for user $uid document")
                    }

                    // Continuous Pull Sync: Only from Cloud to Local
                    userRepository.observeUser(uid).collectLatest { user ->
                        // Reset premium customization settings (gradient theme, status color, status emoji) to default if user is FREE
                        if (user.membership == Membership.FREE &&
                            (user.settings.gradientTheme != "DEFAULT" || user.settings.statusColor.isNotBlank() || user.statusEmoji.isNotBlank())
                        ) {
                            Log.d(
                                "MainViewModel",
                                "User membership is FREE, but has premium customization settings. Resetting to defaults."
                            )
                            if (user.settings.gradientTheme != "DEFAULT") {
                                settingsRepository.setGradientTheme("DEFAULT")
                            }
                            if (user.settings.statusColor.isNotBlank()) {
                                settingsRepository.setStatusColor("")
                            }
                            val updatedUser = user.copy(
                                statusEmoji = "",
                                settings = user.settings.copy(
                                    gradientTheme = "DEFAULT",
                                    statusColor = ""
                                )
                            )
                            userRepository.updateUser(updatedUser)
                            return@collectLatest
                        }

                        val currentTheme = settingsRepository.themeModeFlow.first()
                        val currentBackground = settingsRepository.gradientBackgroundFlow.first()
                        val currentThemeName = settingsRepository.gradientThemeFlow.first()
                        val currentStatusColor = settingsRepository.statusColorFlow.first()

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
                        if (user.settings.gradientTheme != currentThemeName) {
                            Log.d(
                                "MainViewModel",
                                "Syncing gradientTheme from Cloud: ${user.settings.gradientTheme}"
                            )
                            settingsRepository.setGradientTheme(user.settings.gradientTheme)
                        }
                        if (user.settings.statusColor != currentStatusColor) {
                            Log.d(
                                "MainViewModel",
                                "Syncing statusColor from Cloud: ${user.settings.statusColor}"
                            )
                            settingsRepository.setStatusColor(user.settings.statusColor)
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
                                !request.isViewed &&
                                !shownIds.contains(request.id) &&
                                !suppressedIds.contains(request.id)
                            ) {
                                Log.d("MainViewModel", "Triggering banner: ${request.id}")
                                activeSystemNotifications.update { it + request.id }
                                settingsRepository.markNotificationAsShown(request.id)
                                _systemNotificationToDisplay.send(
                                    Notification(
                                        id = request.id,
                                        title = "", // Handled by View (MainActivity) using type
                                        message = "${request.senderName} (${request.senderEmail})", // Raw data for parsing
                                        type = NotificationType.FRIEND_REQUEST_RECEIVED
                                    )
                                )
                                viewModelScope.launch {
                                    socialRepository.markFriendRequestAsViewed(request.id)
                                }
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
                            if ((notification.type == NotificationType.FRIEND_ACCEPTED ||
                                        notification.type == NotificationType.EVENT_COMMENT ||
                                        notification.type == NotificationType.EVENT_INVITE) &&
                                !notification.isSilent &&
                                !notification.isRead &&
                                !notification.isViewed &&
                                !shownIds.contains(notification.id) &&
                                !suppressedIds.contains(notification.id)
                            ) {
                                Log.d("MainViewModel", "Triggering banner: ${notification.id}")
                                activeSystemNotifications.update { it + notification.id }
                                settingsRepository.markNotificationAsShown(notification.id)
                                _systemNotificationToDisplay.send(notification)
                                viewModelScope.launch {
                                    socialRepository.markNotificationAsViewed(notification.id)
                                }
                            }
                        }
                    }
                } else {
                    activeSystemNotifications.value = emptySet()
                }
            }
        }
    }

    private fun observeUserNavState() {
        viewModelScope.launch {
            userRepository.userIdFlow.collectLatest { uid ->
                if (uid.isNotBlank()) {
                    combine(
                        userRepository.observeUser(uid),
                        planRepository.getOwnPlans(),
                        settingsRepository.statusColorFlow
                    ) { user, plans, localColor ->
                        val isAnyPlanActiveNow = plans.any { isPlanActive(it) }
                        val effectiveIsFree =
                            if (user.isStatusFromPlan) isAnyPlanActiveNow else user.isFreeNow
                        val effectiveFromPlan = user.isStatusFromPlan && isAnyPlanActiveNow
                        UserNavState(
                            profilePicUrl = user.profilePicUrl,
                            isUserFree = effectiveIsFree,
                            isStatusFromPlan = effectiveFromPlan,
                            statusColor = localColor,
                            statusEmoji = user.statusEmoji
                        )
                    }.collect { state ->
                        _userNavState.value = state
                    }
                } else {
                    _userNavState.value = null
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
                if (notification.eventId.isNotBlank()) {
                    val scrollToComments = notification.type == NotificationType.EVENT_COMMENT
                    _navigateToRoute.emit(
                        Screen.EventDetails.createRoute(
                            notification.eventId,
                            scrollToComments
                        )
                    )
                } else {
                    _navigateToRoute.emit(Screen.Notifications.route)
                }
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

}

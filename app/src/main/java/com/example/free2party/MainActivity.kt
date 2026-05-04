package com.example.free2party

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.free2party.data.model.Notification
import com.example.free2party.data.model.NotificationType
import com.example.free2party.ui.navigation.AppNavigation
import com.example.free2party.ui.navigation.Screen
import com.example.free2party.ui.theme.Free2PartyTheme
import com.example.free2party.util.NotificationHelper
import com.example.free2party.util.matchNameAndEmail
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationIdState = mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askNotificationPermission()

        // Capture initial intent data
        notificationIdState.value =
            intent.getStringExtra("NOTIFICATION_ID") ?: intent.getStringExtra("notificationId")

        setContent {
            var currentNotificationId by notificationIdState

            val mainViewModel: MainViewModel = hiltViewModel()

            // Pass initial notification ID to ViewModel
            LaunchedEffect(Unit) {
                if (currentNotificationId != null) {
                    mainViewModel.setInitialNotificationId(currentNotificationId)
                }
            }

            val initialStartDestination = remember {
                if ((currentNotificationId != null) && (Firebase.auth.currentUser != null)) {
                    Screen.Notifications.route
                } else {
                    null
                }
            }

            // Handle notification clicks (from both startup and onNewIntent)
            LaunchedEffect(currentNotificationId) {
                currentNotificationId?.let { id ->
                    mainViewModel.onNotificationClicked(id)
                    intent.removeExtra("NOTIFICATION_ID")
                    intent.removeExtra("notificationId")
                    currentNotificationId = null
                }
            }

            LaunchedEffect(mainViewModel) {
                mainViewModel.systemNotificationToDisplay.collectLatest { notification ->
                    val (title, body) = resolveNotificationStrings(notification)
                    NotificationHelper.showNotification(
                        context = this@MainActivity,
                        notificationId = notification.id,
                        title = title,
                        message = body
                    )
                }
            }

            LaunchedEffect(mainViewModel) {
                mainViewModel.systemNotificationToDismiss.collectLatest { notificationId ->
                    NotificationHelper.dismissNotification(
                        context = this@MainActivity,
                        notificationId = notificationId
                    )
                }
            }

            Free2PartyTheme(themeMode = mainViewModel.themeMode) {
                // Lock orientation to portrait on phones (width < 600dp), allow rotation on tablets
                val windowSize = currentWindowSize()
                val density = LocalDensity.current
                val widthDp = with(density) { windowSize.toSize().width.toDp() }

                LaunchedEffect(widthDp) {
                    requestedOrientation = if (widthDp < 600.dp) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                    }
                }

                AppNavigation(
                    mainViewModel = mainViewModel,
                    startDestination = initialStartDestination
                )
            }
        }
    }

    private fun resolveNotificationStrings(notification: Notification): Pair<String, String> {
        return when (notification.type) {
            NotificationType.FRIEND_REQUEST_RECEIVED -> {
                val match = notification.message.matchNameAndEmail(this)
                val title = getString(R.string.notification_friend_request_received_title)
                val body = if (match != null) {
                    getString(
                        R.string.notification_friend_request_received_body,
                        match.first,
                        match.second
                    )
                } else notification.message
                title to body
            }

            NotificationType.FRIEND_ADDED -> {
                val match = notification.message.matchNameAndEmail(this)
                val title = getString(R.string.notification_friend_request_accepted_title)
                val body = if (match != null) {
                    getString(
                        R.string.notification_friend_request_accepted_body,
                        match.first,
                        match.second
                    )
                } else notification.message
                title to body
            }

            NotificationType.FRIEND_DECLINED -> {
                val match = notification.message.matchNameAndEmail(this)
                val title = getString(R.string.notification_friend_request_declined_title)
                val body = if (match != null) {
                    getString(
                        R.string.notification_friend_request_declined_body,
                        match.first,
                        match.second
                    )
                } else notification.message
                title to body
            }

            NotificationType.FRIEND_REMOVED -> {
                val match = notification.message.matchNameAndEmail(this)
                val title = getString(R.string.notification_friend_removed_title)
                val body = if (match != null) {
                    getString(
                        R.string.notification_friend_removed_body,
                        match.first,
                        match.second
                    )
                } else notification.message
                title to body
            }

            else -> notification.title to notification.message
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationIdState.value =
            intent.getStringExtra("NOTIFICATION_ID") ?: intent.getStringExtra("notificationId")
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

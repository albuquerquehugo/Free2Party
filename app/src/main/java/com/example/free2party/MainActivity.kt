package com.example.free2party

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.ui.navigation.AppNavigation
import com.example.free2party.ui.navigation.Screen
import com.example.free2party.ui.theme.Free2PartyTheme
import com.example.free2party.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.collectLatest

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
        notificationIdState.value = intent.getStringExtra("NOTIFICATION_ID") ?: intent.getStringExtra("notificationId")

        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            var currentNotificationId by notificationIdState

            val initialStartDestination = remember {
                if (currentNotificationId != null && Firebase.auth.currentUser != null) {
                    Screen.Notifications.route
                } else {
                    null
                }
            }

            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(
                    context = applicationContext,
                    settingsRepository = settingsRepository,
                    initialHandledNotificationId = currentNotificationId
                )
            )

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
                    NotificationHelper.showNotification(
                        context = this@MainActivity,
                        notificationId = notification.id,
                        title = notification.title,
                        message = notification.message
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
                AppNavigation(
                    settingsRepository = settingsRepository,
                    mainViewModel = mainViewModel,
                    startDestination = initialStartDestination
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationIdState.value = intent.getStringExtra("NOTIFICATION_ID") ?: intent.getStringExtra("notificationId")
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

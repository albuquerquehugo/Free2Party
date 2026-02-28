package com.example.free2party

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.ui.navigation.AppNavigation
import com.example.free2party.ui.theme.Free2PartyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(settingsRepository)
            )
            Free2PartyTheme(themeMode = viewModel.themeMode) {
                AppNavigation(settingsRepository)
            }
        }
    }
}

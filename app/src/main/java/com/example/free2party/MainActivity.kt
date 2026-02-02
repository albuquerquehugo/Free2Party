package com.example.free2party

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.free2party.ui.navigation.AppNavigation
import com.example.free2party.ui.theme.Free2PartyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Free2PartyTheme {
                AppNavigation()
            }
        }
    }
}

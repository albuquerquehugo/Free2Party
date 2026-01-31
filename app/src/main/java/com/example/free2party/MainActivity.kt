package com.example.free2party

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.free2party.ui.navigation.Routes
import com.example.free2party.ui.screens.addfriend.AddFriendScreen
import com.example.free2party.ui.screens.home.HomeScreen
import com.example.free2party.ui.screens.login.LoginScreen
import com.example.free2party.ui.screens.register.RegisterScreen
import com.example.free2party.ui.theme.Free2PartyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Free2PartyTheme {
                Free2PartyNavGraph()
            }
        }
    }
}

@Composable
fun Free2PartyNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        // --- Login Screen ---
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        // --- Register Screen ---
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // --- Home Screen ---
        composable(Routes.HOME) {
            HomeScreen(
                onAddFriendClick = {
                    navController.navigate(Routes.ADD_FRIEND)
                }
            )
        }

        // -- Add Friend Screen ---
        composable(Routes.ADD_FRIEND) {
            AddFriendScreen()
        }
    }
}
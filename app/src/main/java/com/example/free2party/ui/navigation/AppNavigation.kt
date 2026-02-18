package com.example.free2party.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.free2party.ui.screens.calendar.CalendarRoute
import com.example.free2party.ui.screens.home.HomeRoute
import com.example.free2party.ui.screens.login.LoginRoute
import com.example.free2party.ui.screens.notifications.NotificationsRoute
import com.example.free2party.ui.screens.notifications.NotificationsViewModel
import com.example.free2party.ui.screens.profile.ProfileRoute
import com.example.free2party.ui.screens.register.RegisterRoute
import com.example.free2party.util.provideCalendarViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login", Icons.AutoMirrored.Filled.Login)
    object Register : Screen("register", "Register", Icons.Default.HowToReg)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Notifications : Screen("notifications", "Notifications", Icons.Default.Notifications)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Calendar,
    Screen.Notifications
)

@Composable
fun AppNavigation(notificationsViewModel: NotificationsViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = BottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController, notificationsViewModel)
            }
        }
    ) { innerPadding ->
        Free2PartyNavGraph(
            navController = navController,
            notificationsViewModel = notificationsViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, notificationsViewModel: NotificationsViewModel) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val friendRequests by notificationsViewModel.friendRequests.collectAsState()
    val pendingCount = friendRequests.size

    NavigationBar {
        BottomNavItems.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    if (screen is Screen.Notifications && pendingCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(pendingCount.toString())
                                }
                            }
                        ) {
                            Icon(screen.icon!!, contentDescription = screen.label)
                        }
                    } else {
                        screen.icon?.let { Icon(it, contentDescription = screen.label) }
                    }
                },
                label = { screen.label?.let { Text(it) } },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun Free2PartyNavGraph(
    navController: NavHostController,
    notificationsViewModel: NotificationsViewModel,
    modifier: Modifier = Modifier
) {
    val startDest = if (Firebase.auth.currentUser != null) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterRoute(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeRoute(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.Calendar.route) {
            val uid = Firebase.auth.currentUser?.uid ?: ""
            CalendarRoute(
                viewModel = viewModel(
                    key = "calendar_$uid",
                    factory = provideCalendarViewModelFactory(null) // null uses the current user correctly
                )
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsRoute(viewModel = notificationsViewModel)
        }

        composable(Screen.Profile.route) {
            ProfileRoute(onBack = { navController.popBackStack() })
        }
    }
}

package com.example.free2party.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.ui.screens.calendar.CalendarRoute
import com.example.free2party.ui.screens.calendar.CalendarViewModel
import com.example.free2party.ui.screens.home.HomeRoute
import com.example.free2party.ui.screens.login.LoginRoute
import com.example.free2party.ui.screens.login.LoginViewModel
import com.example.free2party.ui.screens.notifications.NotificationsRoute
import com.example.free2party.ui.screens.notifications.NotificationsViewModel
import com.example.free2party.ui.screens.profile.ProfileRoute
import com.example.free2party.ui.screens.register.RegisterRoute
import com.example.free2party.ui.screens.settings.SettingsRoute
import com.example.free2party.ui.screens.settings.SettingsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

sealed class Screen(
    val route: String,
    val label: String? = null,
    val icon: ImageVector? = null,
    val iconSelected: ImageVector? = null
) {
    object Login : Screen(route = "login", label = "Login", icon = Icons.AutoMirrored.Filled.Login)
    object Register : Screen(route = "register", label = "Register", icon = Icons.Default.HowToReg)
    object Profile : Screen(route = "profile", label = "Profile", icon = Icons.Default.Person)
    object Settings : Screen(route = "settings", label = "Settings", icon = Icons.Default.Settings)
    object Home : Screen(
        route = "home",
        label = "Home",
        icon = Icons.Outlined.Home,
        iconSelected = Icons.Filled.Home
    )

    object Calendar :
        Screen(
            route = "calendar",
            label = "Calendar",
            icon = Icons.Outlined.CalendarMonth,
            iconSelected = Icons.Filled.CalendarMonth
        )

    object Notifications : Screen(
        route = "notifications",
        label = "Notifications",
        icon = Icons.Outlined.Notifications,
        iconSelected = Icons.Filled.Notifications
    )
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Calendar,
    Screen.Notifications
)

@Composable
fun AppNavigation(
    settingsRepository: SettingsRepository,
    notificationsViewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModel.provideFactory()
    )
) {
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
            settingsRepository = settingsRepository,
            notificationsViewModel = notificationsViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    notificationsViewModel: NotificationsViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val unreadCountState = notificationsViewModel.itemsUnreadCount.collectAsState(initial = 0)
    val totalUnread = unreadCountState.value

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        BottomNavItems.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    if (screen is Screen.Notifications && totalUnread > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(totalUnread.toString())
                                }
                            }
                        ) {
                            Icon(
                                if (isSelected) screen.iconSelected!! else screen.icon!!,
                                contentDescription = screen.label
                            )

                        }
                    } else {
                        if (isSelected && screen.iconSelected != null) {
                            Icon(screen.iconSelected, contentDescription = screen.label)
                        } else {
                            screen.icon?.let {
                                Icon(it, contentDescription = screen.label)
                            }
                        }
                    }
                },
                label = {
                    screen.label?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                        )
                    }
                },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun Free2PartyNavGraph(
    navController: NavHostController,
    settingsRepository: SettingsRepository,
    notificationsViewModel: NotificationsViewModel,
    modifier: Modifier = Modifier
) {
    val startDest = remember {
        if (Firebase.auth.currentUser != null) Screen.Home.route else Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginRoute(
                viewModel = viewModel(
                    factory = LoginViewModel.provideFactory(settingsRepository)
                ),
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
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Calendar.route) {
            val uid = Firebase.auth.currentUser?.uid ?: ""
            CalendarRoute(
                viewModel = viewModel(
                    key = "calendar_$uid",
                    factory = CalendarViewModel.provideFactory(null)
                )
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsRoute(viewModel = notificationsViewModel)
        }

        composable(Screen.Profile.route) {
            ProfileRoute(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsRoute(
                viewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(settingsRepository)
                ),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

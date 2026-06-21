package com.free2party.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.free2party.util.TextFieldRegistry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.free2party.MainViewModel
import com.free2party.R
import com.free2party.ui.components.AppBackground
import com.free2party.ui.screens.appearance.AppearanceRoute
import com.free2party.ui.screens.circles.CirclesRoute
import com.free2party.ui.screens.calendar.CalendarRoute
import com.free2party.ui.screens.home.HomeRoute
import com.free2party.ui.screens.friends.InviteFriendRoute
import com.free2party.ui.screens.login.LoginRoute
import com.free2party.ui.screens.notifications.NotificationsRoute
import com.free2party.ui.screens.notifications.NotificationsViewModel
import com.free2party.ui.screens.profile.ProfileRoute
import com.free2party.ui.screens.blocked.BlockedUsersRoute
import com.free2party.ui.screens.register.RegisterRoute
import com.free2party.ui.screens.settings.SettingsRoute
import com.free2party.ui.screens.events.EventsRoute
import com.free2party.ui.screens.events.EventsViewModel
import com.free2party.ui.screens.events.CreateEventScreen
import com.free2party.ui.screens.events.EventDetailsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(
    val route: String,
    @get:StringRes val labelResId: Int? = null,
    val icon: ImageVector? = null,
    val iconSelected: ImageVector? = null
) {
    object Login : Screen(
        route = "login",
        labelResId = R.string.title_login,
        icon = Icons.AutoMirrored.Filled.Login
    )

    object Register : Screen(
        route = "register",
        labelResId = R.string.title_register,
        icon = Icons.Default.HowToReg
    )

    object Profile :
        Screen(route = "profile", labelResId = R.string.title_profile, icon = Icons.Default.Person)

    object BlockedUsers : Screen(
        route = "blocked_users",
        labelResId = R.string.title_blocked_users,
        icon = Icons.Default.Block
    )

    object Settings : Screen(
        route = "settings",
        labelResId = R.string.title_settings,
        icon = Icons.Default.Settings
    )

    object Home : Screen(
        route = "home",
        labelResId = R.string.title_home,
        icon = Icons.Outlined.Home,
        iconSelected = Icons.Filled.Home
    )

    object Calendar :
        Screen(
            route = "calendar",
            labelResId = R.string.title_calendar,
            icon = Icons.Outlined.CalendarMonth,
            iconSelected = Icons.Filled.CalendarMonth
        )

    object Notifications : Screen(
        route = "notifications",
        labelResId = R.string.title_notifications,
        icon = Icons.Outlined.Notifications,
        iconSelected = Icons.Filled.Notifications
    )

    object Events : Screen(
        route = "events",
        labelResId = R.string.title_events,
        icon = Icons.Outlined.Celebration,
        iconSelected = Icons.Filled.Celebration
    )

    object CreateEvent : Screen(
        route = "create_event?eventId={eventId}",
        labelResId = R.string.title_create_event
    ) {
        fun createRoute(eventId: String? = null): String {
            return if (eventId != null) "create_event?eventId=$eventId" else "create_event"
        }
    }

    object EventDetails : Screen(
        route = "event_details/{eventId}?scrollToComments={scrollToComments}",
        labelResId = R.string.title_event_details
    ) {
        fun createRoute(eventId: String, scrollToComments: Boolean = false): String {
            return "event_details/$eventId?scrollToComments=$scrollToComments"
        }
    }

    object InviteFriend : Screen(
        route = "invite_friend",
        labelResId = R.string.title_invite_friend
    )

    object Circles : Screen(
        route = "circles",
        labelResId = R.string.title_circles
    )

    object Appearance : Screen(
        route = "appearance",
        labelResId = R.string.title_appearance
    )
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Calendar,
    Screen.Events,
    Screen.Notifications
)

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel = hiltViewModel(),
    notificationsViewModel: NotificationsViewModel = hiltViewModel(),
    eventsViewModel: EventsViewModel = hiltViewModel(),
    startDestination: String? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = BottomNavItems.any { it.route == currentDestination?.route }
    val gradientBackground by mainViewModel.gradientBackgroundFlow.collectAsState(initial = true)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(mainViewModel) {
        mainViewModel.navigateToRoute.collectLatest { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    AppBackground(enabled = gradientBackground) {
        Scaffold(
            containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
            bottomBar = {
                if (showBottomBar) {
                    BottomNavigationBar(navController, notificationsViewModel, eventsViewModel)
                }
            }
        ) { innerPadding ->
            var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .onGloballyPositioned { rootCoordinates = it }
                    .pointerInput(rootCoordinates) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                            val isInsideTextField = TextFieldRegistry.isPointInsideAnyTextField(
                                down.position,
                                rootCoordinates
                            )
                            if (!isInsideTextField) {
                                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                if (up != null) {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                }
                            }
                        }
                    }
                    .focusable()
            ) {
                Free2PartyNavGraph(
                    navController = navController,
                    notificationsViewModel = notificationsViewModel,
                    startDestinationOverride = startDestination
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    notificationsViewModel: NotificationsViewModel,
    eventsViewModel: EventsViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val unreadCountState = notificationsViewModel.itemsUnreadCount.collectAsState(initial = 0)
    val totalUnread = unreadCountState.value

    val pendingInvitationsCountState =
        eventsViewModel.pendingInvitationsCount.collectAsState(initial = 0)
    val pendingInvitationsCount = pendingInvitationsCountState.value

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) {
        BottomNavItems.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val label = screen.labelResId?.let { stringResource(it) }

            NavigationBarItem(
                icon = {
                    if ((screen is Screen.Notifications) && (totalUnread > 0)) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(totalUnread.toString())
                                }
                            }
                        ) {
                            Icon(
                                if (isSelected) screen.iconSelected!! else screen.icon!!,
                                contentDescription = label
                            )

                        }
                    } else if ((screen is Screen.Events) && (pendingInvitationsCount > 0)) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(pendingInvitationsCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                if (isSelected) screen.iconSelected!! else screen.icon!!,
                                contentDescription = label
                            )

                        }
                    } else {
                        if (isSelected && screen.iconSelected != null) {
                            Icon(screen.iconSelected, contentDescription = label)
                        } else {
                            screen.icon?.let {
                                Icon(it, contentDescription = label)
                            }
                        }
                    }
                },
                label = {
                    label?.let {
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
    notificationsViewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
    startDestinationOverride: String? = null
) {
    val startDest = remember(startDestinationOverride) {
        startDestinationOverride
            ?: if (Firebase.auth.currentUser != null) Screen.Home.route else Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginRoute(
                viewModel = hiltViewModel(),
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
                viewModel = hiltViewModel(),
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
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
                homeViewModel = hiltViewModel(),
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToBlockedUsers = {
                    navController.navigate(Screen.BlockedUsers.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToInviteFriend = {
                    navController.navigate(Screen.InviteFriend.route)
                },
                onNavigateToCircles = {
                    navController.navigate(Screen.Circles.route)
                },
                onNavigateToAppearance = {
                    navController.navigate(Screen.Appearance.route)
                }
            )
        }

        composable(Screen.InviteFriend.route) {
            val mainViewModel: MainViewModel = hiltViewModel()
            val gradientBackground by mainViewModel.gradientBackgroundFlow.collectAsState(initial = true)

            InviteFriendRoute(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                gradientBackground = gradientBackground
            )
        }

        composable(Screen.Circles.route) {
            CirclesRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Appearance.route) {
            AppearanceRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarRoute()
        }

        composable(Screen.Notifications.route) {
            NotificationsRoute(
                viewModel = notificationsViewModel,
                onNavigateToEventDetails = { eventId, scrollToComments ->
                    navController.navigate(
                        Screen.EventDetails.createRoute(
                            eventId,
                            scrollToComments
                        )
                    )
                }
            )
        }

        composable(Screen.Events.route) {
            EventsRoute(
                viewModel = hiltViewModel(),
                onNavigateToCreateEvent = {
                    navController.navigate(Screen.CreateEvent.createRoute())
                },
                onNavigateToEventDetails = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                }
            )
        }

        composable(
            route = Screen.CreateEvent.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            CreateEventScreen(
                viewModel = hiltViewModel(),
                editingEventId = eventId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EventDetails.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                },
                navArgument("scrollToComments") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val scrollToComments = backStackEntry.arguments?.getBoolean("scrollToComments") ?: false
            EventDetailsScreen(
                viewModel = hiltViewModel(),
                eventId = eventId,
                scrollToComments = scrollToComments,
                onNavigateToEditEvent = { id ->
                    navController.navigate(Screen.CreateEvent.createRoute(id))
                },
                onNavigateToEvents = {
                    navController.navigate(Screen.Events.route) {
                        popUpTo(Screen.EventDetails.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.BlockedUsers.route) {
            BlockedUsersRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsRoute(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

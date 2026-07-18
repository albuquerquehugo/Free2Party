package com.free2party.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.navArgument
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.free2party.MainViewModel
import com.free2party.R
import com.free2party.ui.components.AppBackground
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.screens.appearance.AppearanceRoute
import com.free2party.ui.screens.blocked.BlockedUsersRoute
import com.free2party.ui.screens.circles.CirclesRoute
import com.free2party.ui.screens.calendar.CalendarRoute
import com.free2party.ui.screens.events.CreateEventScreen
import com.free2party.ui.screens.events.EventsRoute
import com.free2party.ui.screens.events.EventsViewModel
import com.free2party.ui.screens.events.EventDetailsScreen
import com.free2party.ui.screens.home.HomeRoute
import com.free2party.ui.screens.friends.AddFriendRoute
import com.free2party.ui.screens.login.LoginRoute
import com.free2party.ui.screens.notifications.NotificationsRoute
import com.free2party.ui.screens.notifications.NotificationsViewModel
import com.free2party.ui.screens.onboarding.OnboardingRoute
import com.free2party.ui.screens.premium.PremiumRoute
import com.free2party.ui.screens.profile.EditProfileRoute
import com.free2party.ui.screens.profile.InterestsRoute
import com.free2party.ui.screens.profile.ProfileRoute
import com.free2party.ui.screens.register.RegisterRoute
import com.free2party.ui.screens.settings.SettingsRoute
import com.free2party.ui.theme.available
import com.free2party.ui.theme.availableContainer
import com.free2party.ui.theme.busy
import com.free2party.ui.theme.onAvailableContainer
import com.free2party.data.model.ThemeMode
import com.free2party.ui.theme.Free2PartyTheme
import com.free2party.util.TextFieldRegistry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    @get:StringRes val labelResId: Int? = null,
    val icon: ImageVector? = null,
    val iconSelected: ImageVector? = null
) {
    object Login : Screen(
        route = "login",
        labelResId = R.string.label_login,
        icon = Icons.AutoMirrored.Filled.Login
    )

    object Register : Screen(
        route = "register",
        labelResId = R.string.label_register,
        icon = Icons.Default.HowToReg
    )

    object Profile :
        Screen(route = "profile", labelResId = R.string.label_profile, icon = Icons.Default.Person)

    object EditProfile : Screen(
        route = "edit_profile",
        labelResId = R.string.label_edit_profile,
        icon = Icons.Default.Person
    )

    object Interests : Screen(
        route = "interests",
        labelResId = R.string.label_interests,
        icon = Icons.Default.Favorite
    )

    object BlockedUsers : Screen(
        route = "blocked_users",
        labelResId = R.string.label_blocked_users,
        icon = Icons.Default.Block
    )

    object Settings : Screen(
        route = "settings",
        labelResId = R.string.label_settings,
        icon = Icons.Default.Settings
    )

    object Home : Screen(
        route = "home",
        labelResId = R.string.label_home,
        icon = Icons.Outlined.Home,
        iconSelected = Icons.Filled.Home
    )

    object Calendar :
        Screen(
            route = "calendar",
            labelResId = R.string.label_calendar,
            icon = Icons.Outlined.CalendarMonth,
            iconSelected = Icons.Filled.CalendarMonth
        )

    object Notifications : Screen(
        route = "notifications",
        labelResId = R.string.label_notifications,
        icon = Icons.Outlined.Notifications,
        iconSelected = Icons.Filled.Notifications
    )

    object Events : Screen(
        route = "events",
        labelResId = R.string.label_events,
        icon = Icons.Outlined.Celebration,
        iconSelected = Icons.Filled.Celebration
    )

    object CreateEvent : Screen(
        route = "create_event?eventId={eventId}",
        labelResId = R.string.label_create_event
    ) {
        fun createRoute(eventId: String? = null): String {
            return if (eventId != null) "create_event?eventId=$eventId" else "create_event"
        }
    }

    object EventDetails : Screen(
        route = "event_details/{eventId}?scrollToComments={scrollToComments}",
        labelResId = R.string.label_event_details
    ) {
        fun createRoute(eventId: String, scrollToComments: Boolean = false): String {
            return "event_details/$eventId?scrollToComments=$scrollToComments"
        }
    }

    object AddFriend : Screen(
        route = "add_friend",
        labelResId = R.string.label_add_friend
    )

    object Circles : Screen(
        route = "circles",
        labelResId = R.string.label_circles
    )

    object Appearance : Screen(
        route = "appearance",
        labelResId = R.string.label_appearance
    )

    object Onboarding : Screen(
        route = "onboarding",
        labelResId = R.string.label_onboarding
    )

    object Premium : Screen(
        route = "premium",
        labelResId = R.string.label_premium
    )
}

val BottomNavItems = listOf(
    Screen.Calendar,
    Screen.Events,
    Screen.Home,
    Screen.Notifications,
    Screen.Profile
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

    // Log screen view to Firebase Analytics
    LaunchedEffect(currentDestination) {
        currentDestination?.route?.let { route ->
            val context = navController.context
            val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
            val bundle = android.os.Bundle().apply {
                putString(com.google.firebase.analytics.FirebaseAnalytics.Param.SCREEN_NAME, route)
                putString(com.google.firebase.analytics.FirebaseAnalytics.Param.SCREEN_CLASS, route)
            }
            analytics.logEvent(
                com.google.firebase.analytics.FirebaseAnalytics.Event.SCREEN_VIEW,
                bundle
            )
        }
    }

    val showBottomBar = BottomNavItems.any { it.route == currentDestination?.route }
    val gradientBackground by mainViewModel.gradientBackgroundFlow.collectAsState(initial = true)
    val gradientTheme by mainViewModel.gradientThemeFlow.collectAsState(initial = "DEFAULT")
    val isLoginOrRegister =
        currentDestination?.route == Screen.Login.route || currentDestination?.route == Screen.Register.route
    val effectiveGradientBackground = if (isLoginOrRegister) true else gradientBackground
    val effectiveGradientTheme = if (isLoginOrRegister) "DEFAULT" else gradientTheme
    val onboardingCompleted by mainViewModel.onboardingCompletedFlow.collectAsState(initial = null)
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

    if (onboardingCompleted != null) {
        val effectiveThemeMode =
            if (isLoginOrRegister) ThemeMode.AUTOMATIC else mainViewModel.themeMode
        Free2PartyTheme(themeMode = effectiveThemeMode) {
            AppBackground(
                enabled = effectiveGradientBackground,
                themeName = effectiveGradientTheme
            ) {
                Scaffold(
                    topBar = {
                        val isOnline by mainViewModel.isOnline.collectAsState()
                        AnimatedVisibility(
                            visible = !isOnline,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.error)
                                    .statusBarsPadding()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.text_offline_banner),
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    },
                    containerColor =
                        if (effectiveGradientBackground) Color.Transparent
                        else MaterialTheme.colorScheme.surface,
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(
                                navController,
                                notificationsViewModel,
                                eventsViewModel,
                                mainViewModel
                            )
                        }
                    }
                ) { innerPadding ->
                    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    val density = LocalDensity.current
                    val swipeThresholdPx = remember(density, rootCoordinates) {
                        rootCoordinates?.size?.width?.let { it * 0.25f }
                            ?: with(density) { 150.dp.toPx() }
                    }

                    val dragOffset = remember { Animatable(0f) }
                    val coroutineScope = rememberCoroutineScope()

                    val swipeModifier = if (showBottomBar) {
                        Modifier.pointerInput(currentDestination) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    coroutineScope.launch {
                                        dragOffset.snapTo(0f)
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        val currentRoute = currentDestination?.route
                                        val currentIndex =
                                            BottomNavItems.indexOfFirst { it.route == currentRoute }
                                        var navigated = false
                                        if (currentIndex != -1) {
                                            if (dragOffset.value > swipeThresholdPx) {
                                                // Swipe left-to-right: Navigate to left screen
                                                if (currentIndex > 0) {
                                                    navigated = true
                                                    val leftScreen =
                                                        BottomNavItems[currentIndex - 1]
                                                    navController.navigate(leftScreen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            } else if (dragOffset.value < -swipeThresholdPx) {
                                                // Swipe right-to-left: Navigate to right screen
                                                if (currentIndex < BottomNavItems.size - 1) {
                                                    navigated = true
                                                    val rightScreen =
                                                        BottomNavItems[currentIndex + 1]
                                                    navController.navigate(rightScreen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        }
                                        if (navigated) {
                                            dragOffset.snapTo(0f)
                                        } else {
                                            dragOffset.animateTo(
                                                0f,
                                                spring(stiffness = Spring.StiffnessMediumLow)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(
                                            0f,
                                            spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        val currentRoute = currentDestination?.route
                                        val currentIndex =
                                            BottomNavItems.indexOfFirst { it.route == currentRoute }
                                        val isAtBoundary = (dragAmount > 0f && currentIndex == 0) ||
                                                (dragAmount < 0f && currentIndex == BottomNavItems.size - 1)
                                        val finalDrag =
                                            if (isAtBoundary) dragAmount * 0.3f else dragAmount
                                        dragOffset.snapTo(dragOffset.value + finalDrag)
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .then(swipeModifier)
                            .graphicsLayer {
                                translationX = dragOffset.value
                            }
                            .onGloballyPositioned { rootCoordinates = it }
                            .pointerInput(rootCoordinates) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                    val isInsideTextField =
                                        TextFieldRegistry.isPointInsideAnyTextField(
                                            down.position,
                                            rootCoordinates
                                        )
                                    if (!isInsideTextField) {
                                        val up =
                                            waitForUpOrCancellation(pass = PointerEventPass.Initial)
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
                            onboardingCompleted = onboardingCompleted ?: false,
                            startDestinationOverride = startDestination
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingHomeButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        label = "HomeScale"
    )
    val startColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.7f
        ),
        label = "HomeStartColor"
    )
    val endColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.7f
        ),
        label = "HomeEndColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "HomeIconTint"
    )
    val elevation = if (isSelected) 12.dp else 0.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(56.dp)
            .shadow(elevation = elevation, shape = CircleShape, clip = false)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(startColor, endColor)
                ),
                shape = CircleShape
            )
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.Home else Icons.Outlined.Home,
            contentDescription = stringResource(R.string.label_home),
            tint = iconTint,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    notificationsViewModel: NotificationsViewModel,
    eventsViewModel: EventsViewModel,
    mainViewModel: MainViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val unreadCountState = notificationsViewModel.itemsUnreadCount.collectAsState(initial = 0)
    val totalUnread = unreadCountState.value

    val pendingInvitationsCountState =
        eventsViewModel.pendingInvitationsCount.collectAsState(initial = 0)
    val pendingInvitationsCount = pendingInvitationsCountState.value

    val userNavStateState = mainViewModel.userNavState.collectAsState(initial = null)
    val userNavState = userNavStateState.value

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
        ) {
            AppHorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItems.forEach { screen ->
                    val isSelected =
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val label = screen.labelResId?.let { stringResource(it) }

                    if (screen is Screen.Home) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingHomeButton(
                                isSelected = isSelected,
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
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .clickable(
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (screen) {
                                    is Screen.Notifications if totalUnread > 0 -> {
                                        BadgedBox(
                                            badge = { Badge { Text(totalUnread.toString()) } }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) screen.iconSelected!! else screen.icon!!,
                                                contentDescription = label,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    is Screen.Events if pendingInvitationsCount > 0 -> {
                                        BadgedBox(
                                            badge = { Badge { Text(pendingInvitationsCount.toString()) } }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) screen.iconSelected!! else screen.icon!!,
                                                contentDescription = label,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    is Screen.Profile -> {
                                        val isUserFree = userNavState?.isUserFree ?: false
                                        val profilePicUrl = userNavState?.profilePicUrl
                                        val isStatusFromPlan =
                                            userNavState?.isStatusFromPlan ?: false
                                        val statusColor = if (isUserFree) {
                                            val hex = userNavState.statusColor
                                            if (hex.isNotBlank() && hex.startsWith("#")) {
                                                try {
                                                    Color(hex.toColorInt())
                                                } catch (_: Exception) {
                                                    MaterialTheme.colorScheme.available
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.available
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.busy
                                        }

                                        Box(
                                            modifier = Modifier.size(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .border(1.5.dp, statusColor, CircleShape)
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!profilePicUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = profilePicUrl,
                                                        contentDescription = label,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = label,
                                                        tint =
                                                            if (isSelected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }

                                            if (isStatusFromPlan) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .align(Alignment.TopEnd)
                                                        .background(
                                                            MaterialTheme.colorScheme.availableContainer,
                                                            CircleShape
                                                        )
                                                        .padding(1.dp),
                                                    tint = MaterialTheme.colorScheme.onAvailableContainer
                                                )
                                            }

                                            val statusEmoji = userNavState?.statusEmoji ?: ""
                                            if (statusEmoji.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .align(Alignment.BottomEnd)
                                                        .background(
                                                            MaterialTheme.colorScheme.surface,
                                                            CircleShape
                                                        )
                                                        .border(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.outlineVariant,
                                                            CircleShape
                                                        )
                                                        .padding(1.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = statusEmoji,
                                                        fontSize = 10.sp,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    else -> {
                                        Icon(
                                            imageVector = if (isSelected) screen.iconSelected!! else screen.icon!!,
                                            contentDescription = label,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            label?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}


@Composable
fun Free2PartyNavGraph(
    navController: NavHostController,
    notificationsViewModel: NotificationsViewModel,
    onboardingCompleted: Boolean,
    modifier: Modifier = Modifier,
    startDestinationOverride: String? = null
) {
    val startDest = remember(startDestinationOverride, onboardingCompleted) {
        startDestinationOverride
            ?: if (Firebase.auth.currentUser != null) {
                if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route
            } else {
                Screen.Login.route
            }
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
                    val dest =
                        if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route
                    navController.navigate(dest) {
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

        composable(Screen.Onboarding.route) {
            OnboardingRoute(
                viewModel = hiltViewModel(),
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeRoute(
                homeViewModel = hiltViewModel(),
                onNavigateToAddFriend = {
                    navController.navigate(Screen.AddFriend.route)
                }
            )
        }

        composable(Screen.AddFriend.route) {
            val mainViewModel: MainViewModel = hiltViewModel()
            val gradientBackground by mainViewModel.gradientBackgroundFlow.collectAsState(initial = true)

            AddFriendRoute(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                gradientBackground = gradientBackground
            )
        }

        composable(Screen.Circles.route) {
            CirclesRoute(
                onBack = { navController.popBackStack() },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) }
            )
        }

        composable(Screen.Appearance.route) {
            AppearanceRoute(
                onBack = { navController.popBackStack() },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) }
            )
        }

        composable(Screen.Premium.route) {
            PremiumRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarRoute(
                onNavigateToEventDetails = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId, false))
                }
            )
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
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToBlockedUsers = {
                    navController.navigate(Screen.BlockedUsers.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToCircles = {
                    navController.navigate(Screen.Circles.route)
                },
                onNavigateToAppearance = {
                    navController.navigate(Screen.Appearance.route)
                },
                onNavigateToInterests = {
                    navController.navigate(Screen.Interests.route)
                },
                onNavigateToPremium = {
                    navController.navigate(Screen.Premium.route)
                }
            )
        }

        composable(Screen.Interests.route) {
            InterestsRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileRoute(
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
                onBack = { navController.popBackStack() },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }
    }
}

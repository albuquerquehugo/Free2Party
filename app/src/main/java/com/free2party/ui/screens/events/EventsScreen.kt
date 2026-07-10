package com.free2party.ui.screens.events

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import coil.compose.AsyncImage
import com.free2party.data.model.Event
import com.free2party.data.model.EventType
import com.free2party.data.model.GuestStatus
import com.free2party.data.model.Membership
import com.free2party.data.model.DistanceUnit
import com.free2party.R
import com.free2party.ui.components.AdBanner
import com.free2party.ui.components.basic.AppOutlinedTextField
import com.free2party.ui.components.PlanStatus
import com.free2party.ui.components.TopBar
import com.free2party.ui.theme.availableContainer
import com.free2party.ui.theme.busyContainer
import com.free2party.ui.theme.currentActivityContainer
import com.free2party.ui.theme.eventContainer
import com.free2party.ui.theme.onAvailableContainer
import com.free2party.ui.theme.onBusyContainer
import com.free2party.ui.theme.onCurrentActivityContainer
import com.free2party.ui.theme.onEventContainer
import com.free2party.ui.theme.onPendingContainer
import com.free2party.ui.theme.pendingContainer
import com.free2party.util.calculateHaversineDistance
import com.free2party.util.formatDistance
import com.free2party.util.formatPlanDateInFull
import com.free2party.util.formatTimeForDisplay
import com.free2party.util.getLastKnownLocation
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun EventsRoute(
    viewModel: EventsViewModel,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingInvitationsCount by viewModel.pendingInvitationsCount.collectAsState()
    val gradientBackground = viewModel.gradientBackground
    val membership = viewModel.membership
    val currentUserId = viewModel.currentUserId
    val use24HourFormat = viewModel.use24HourFormat
    val distanceUnit = viewModel.distanceUnit
    val selectedTabIndex = viewModel.selectedTabIndex
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val eventFilter by viewModel.eventFilter.collectAsState()

    EventsScreen(
        uiState = uiState,
        pendingInvitationsCount = pendingInvitationsCount,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { viewModel.selectedTabIndex = it },
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        userLocation = userLocation,
        onUserLocationChange = { viewModel.setUserLocation(it) },
        gradientBackground = gradientBackground,
        membership = membership,
        currentUserId = currentUserId,
        use24HourFormat = use24HourFormat,
        distanceUnit = distanceUnit,
        onNavigateToCreateEvent = onNavigateToCreateEvent,
        onNavigateToEventDetails = onNavigateToEventDetails,
        eventFilter = eventFilter,
        onEventFilterChange = { viewModel.setEventFilter(it) }
    )
}

@Composable
fun EventsScreen(
    uiState: EventsUiState,
    pendingInvitationsCount: Int,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    userLocation: UserLocation?,
    onUserLocationChange: (UserLocation?) -> Unit,
    gradientBackground: Boolean,
    membership: Membership,
    currentUserId: String,
    use24HourFormat: Boolean,
    distanceUnit: DistanceUnit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit,
    eventFilter: EventFilter,
    onEventFilterChange: (EventFilter) -> Unit
) {
    val context = LocalContext.current
    var showFilterMenu by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            val loc = getLastKnownLocation(context)
            if (loc != null) {
                onUserLocationChange(UserLocation(loc.latitude, loc.longitude))
            }
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            onEventFilterChange(EventFilter.ALL)
        }
        if (selectedTabIndex == 2) {
            val fineGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (fineGranted || coarseGranted) {
                val loc = getLastKnownLocation(context)
                if (loc != null) {
                    onUserLocationChange(UserLocation(loc.latitude, loc.longitude))
                }
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(
                title = stringResource(R.string.label_events),
                color = MaterialTheme.colorScheme.onSurface,
                showBackButton = false,
                onBack = {},
                titleStartAction = {
                    val isFilterActive = eventFilter != EventFilter.ALL
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isFilterActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.description_filter),
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isFilterActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                            .border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.surface,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            val isFilterEnabled = selectedTabIndex != 0
                            DropdownMenuItem(
                                modifier = Modifier.padding(start = 8.dp),
                                text = {
                                    Text(
                                        text = stringResource(R.string.label_all_events),
                                        fontWeight = if (eventFilter == EventFilter.ALL) FontWeight.ExtraBold
                                        else FontWeight.Normal,
                                        color = if (eventFilter == EventFilter.ALL) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onEventFilterChange(EventFilter.ALL)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                modifier = Modifier.padding(start = 8.dp),
                                enabled = isFilterEnabled,
                                text = {
                                    Text(
                                        text = stringResource(R.string.label_going_self),
                                        fontWeight = if (eventFilter == EventFilter.GOING) FontWeight.ExtraBold
                                        else FontWeight.Normal,
                                        color = if (!isFilterEnabled) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else if (eventFilter == EventFilter.GOING) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    onEventFilterChange(EventFilter.GOING)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                modifier = Modifier.padding(start = 8.dp),
                                enabled = isFilterEnabled,
                                text = {
                                    Text(
                                        text = stringResource(R.string.label_not_going_self),
                                        fontWeight = if (eventFilter == EventFilter.NOT_GOING) FontWeight.ExtraBold
                                        else FontWeight.Normal,
                                        color = if (!isFilterEnabled) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else if (eventFilter == EventFilter.NOT_GOING) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    onEventFilterChange(EventFilter.NOT_GOING)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                modifier = Modifier.padding(start = 8.dp),
                                enabled = isFilterEnabled,
                                text = {
                                    Text(
                                        text = stringResource(R.string.label_pending),
                                        fontWeight = if (eventFilter == EventFilter.PENDING) FontWeight.ExtraBold
                                        else FontWeight.Normal,
                                        color = if (!isFilterEnabled) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else if (eventFilter == EventFilter.PENDING) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    onEventFilterChange(EventFilter.PENDING)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                },
                titleEndAction = {
                    IconButton(onClick = onNavigateToCreateEvent) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.label_create_event),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Tabs Selector
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { onTabSelected(0) },
                    text = {
                        Text(
                            stringResource(R.string.label_tab_my_events),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { onTabSelected(1) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(R.string.label_invited),
                                fontWeight = FontWeight.Bold
                            )
                            if (pendingInvitationsCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge {
                                    Text(pendingInvitationsCount.toString())
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { onTabSelected(2) },
                    text = {
                        Text(
                            stringResource(R.string.label_tab_public),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            // Search Bar for Public Tab
            if (selectedTabIndex == 2) {
                AppOutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholderText = stringResource(R.string.placeholder_search_events),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Content based on tab
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (uiState) {
                    is EventsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is EventsUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.message.asString(),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    is EventsUiState.Success -> {
                        val eventsList = when (selectedTabIndex) {
                            0 -> uiState.myEvents
                            1 -> uiState.pendingEvents
                            else -> uiState.publicEvents
                        }
                        if (eventsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.label_no_events),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(eventsList) { event ->
                                    EventCard(
                                        event = event,
                                        currentUserId = currentUserId,
                                        use24Hour = use24HourFormat,
                                        userLocation = userLocation,
                                        distanceUnit = distanceUnit,
                                        gradientBackground = gradientBackground,
                                        onClick = { onNavigateToEventDetails(event.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (membership == Membership.REGULAR) {
                AdBanner()
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    currentUserId: String,
    use24Hour: Boolean,
    userLocation: UserLocation?,
    distanceUnit: DistanceUnit,
    gradientBackground: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val acceptedCount = remember(event.guests) {
        event.guests.values.count { it == GuestStatus.ACCEPTED.name }
    }

    val myStatus = remember(event.guests, currentUserId) {
        event.guests[currentUserId]?.let { GuestStatus.valueOf(it) } ?: GuestStatus.PENDING
    }

    val distanceText = remember(event.latitude, event.longitude, userLocation, distanceUnit) {
        if (userLocation != null && event.latitude != null && event.longitude != null) {
            val meters = calculateHaversineDistance(
                userLocation.latitude,
                userLocation.longitude,
                event.latitude,
                event.longitude
            )
            formatDistance(context, meters, distanceUnit)
        } else {
            null
        }
    }

    val durationText = remember(event.startDate, event.endDate, event.startTime, event.endTime) {
        val duration = com.free2party.util.calculateDuration(
            event.startDate,
            event.endDate,
            event.startTime,
            event.endTime
        )
        val startDateMillis = com.free2party.util.parseDateToMillis(event.startDate) ?: 0L
        val endDateMillis = com.free2party.util.parseDateToMillis(event.endDate) ?: 0L
        val startTimeMinutes = com.free2party.util.parseTimeToMinutes(event.startTime) ?: 0
        val endTimeMinutes = com.free2party.util.parseTimeToMinutes(event.endTime) ?: 0
        val totalMins =
            ((endDateMillis - startDateMillis) / 60000L) + endTimeMinutes - startTimeMinutes
        if (totalMins <= 0) null else duration
    }

    val eventStatus = remember(event) {
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(event.timezone)
        }
        val startDateTime =
            runCatching { timeFormatter.parse("${event.startDate} ${event.startTime}") }
                .getOrNull()?.time ?: 0L
        val endDateTime =
            runCatching { timeFormatter.parse("${event.endDate} ${event.endTime}") }
                .getOrNull()?.time ?: 0L
        val now = System.currentTimeMillis()
        val isCurrent = now in startDateTime..<endDateTime
        val isPast = now >= endDateTime
        PlanStatus(isCurrent = isCurrent, isPast = isPast, isEditDisabled = false)
    }

    val containerColor = when {
        eventStatus.isCurrent -> MaterialTheme.colorScheme.currentActivityContainer.let {
            if (gradientBackground) it.copy(
                alpha = 0.7f
            ) else it
        }

        eventStatus.isPast -> MaterialTheme.colorScheme.eventContainer.copy(
            alpha =
                if (gradientBackground) 0.4f * 0.7f else 0.4f
        )

        else -> MaterialTheme.colorScheme.eventContainer.let {
            if (gradientBackground) it.copy(alpha = 0.7f) else it
        }
    }

    val baseContentColor = when {
        eventStatus.isCurrent -> MaterialTheme.colorScheme.onCurrentActivityContainer
        else -> MaterialTheme.colorScheme.onEventContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isMyEvent = event.hostId == currentUserId
                if (!isMyEvent) {
                    // Host Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (event.hostProfilePic.isNotBlank()) {
                            AsyncImage(
                                model = event.hostProfilePic,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.free2party_full_foreground_color),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Title & Host Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = baseContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isMyEvent) {
                        Text(
                            text = stringResource(R.string.label_hosted_by, event.hostName),
                            style = MaterialTheme.typography.bodySmall,
                            color = baseContentColor
                        )
                    }
                }

                // Status Badge (for pending events)
                if (!isMyEvent) {
                    val badgeText = when (myStatus) {
                        GuestStatus.ACCEPTED -> stringResource(R.string.label_going_self)
                        GuestStatus.DECLINED -> stringResource(R.string.label_not_going_self)
                        GuestStatus.PENDING -> stringResource(R.string.label_pending)
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (myStatus) {
                            GuestStatus.ACCEPTED -> MaterialTheme.colorScheme.onAvailableContainer
                            GuestStatus.DECLINED -> MaterialTheme.colorScheme.onBusyContainer
                            GuestStatus.PENDING -> MaterialTheme.colorScheme.onPendingContainer
                        },
                        modifier = Modifier
                            .background(
                                when (myStatus) {
                                    GuestStatus.ACCEPTED -> MaterialTheme.colorScheme.availableContainer
                                    GuestStatus.DECLINED -> MaterialTheme.colorScheme.busyContainer
                                    GuestStatus.PENDING -> MaterialTheme.colorScheme.pendingContainer
                                }, CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date and Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = baseContentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val dateText = formatPlanDateInFull(event.startDate)
                val timeText = formatTimeForDisplay(event.startTime, use24Hour)
                Text(
                    text = "$dateText @ $timeText (${event.timezone})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = baseContentColor
                )
                if (durationText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                baseContentColor.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                baseContentColor.copy(alpha = 0.2f),
                                CircleShape
                            )
                            .padding(horizontal = 8.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = durationText.asString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = baseContentColor
                        )
                    }
                }
            }

            // Description (if any)
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = baseContentColor,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = baseContentColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            if (event.locationName.isNotBlank() || distanceText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = baseContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val displayText = when {
                        event.locationName.isNotBlank() && distanceText != null ->
                            "${event.locationName} ($distanceText)"

                        event.locationName.isNotBlank() ->
                            event.locationName

                        else ->
                            distanceText ?: ""
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = baseContentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Guests Count & Type indicator
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = baseContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pluralStringResource(
                            R.plurals.label_guests_going,
                            acceptedCount,
                            acceptedCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = baseContentColor
                    )
                }

                Text(
                    text = when (event.type) {
                        EventType.PUBLIC -> stringResource(R.string.label_public)
                        EventType.PRIVATE -> stringResource(R.string.label_private)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (event.type == EventType.PUBLIC) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(
                            color =
                                if (event.type == EventType.PUBLIC) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}

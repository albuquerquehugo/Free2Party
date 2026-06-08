package com.free2party.ui.screens.events

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.free2party.R
import com.free2party.data.model.Event
import com.free2party.data.model.GuestStatus
import com.free2party.data.model.Membership
import com.free2party.ui.components.AdBanner
import com.free2party.ui.components.TopBar
import com.free2party.ui.theme.available
import com.free2party.ui.theme.busy
import com.free2party.util.formatPlanDateInFull
import com.free2party.util.formatTimeForDisplay

@Composable
fun EventsRoute(
    viewModel: EventsViewModel,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val gradientBackground = viewModel.gradientBackground
    val membership = viewModel.membership
    val currentUserId = viewModel.currentUserId
    val use24HourFormat = viewModel.use24HourFormat

    EventsScreen(
        uiState = uiState,
        gradientBackground = gradientBackground,
        membership = membership,
        currentUserId = currentUserId,
        use24HourFormat = use24HourFormat,
        onNavigateToCreateEvent = onNavigateToCreateEvent,
        onNavigateToEventDetails = onNavigateToEventDetails
    )
}

@Composable
fun EventsScreen(
    uiState: EventsUiState,
    gradientBackground: Boolean,
    membership: Membership,
    currentUserId: String,
    use24HourFormat: Boolean,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(
                showBackButton = false,
                onBack = {}
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
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.label_my_events), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.label_invitations), fontWeight = FontWeight.Bold) }
                )
            }

            // Content based on tab (wrapped in weight box to push AdBanner to the bottom)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (uiState) {
                    is EventsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is EventsUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = uiState.message.asString(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is EventsUiState.Success -> {
                        val eventsList = if (selectedTabIndex == 0) uiState.myEvents else uiState.invitedEvents
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

        FloatingActionButton(
            onClick = onNavigateToCreateEvent,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (membership == Membership.REGULAR) 66.dp else 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.title_create_event))
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    currentUserId: String,
    use24Hour: Boolean,
    onClick: () -> Unit
) {
    val acceptedCount = remember(event.guests) {
        event.guests.values.count { it == GuestStatus.ACCEPTED.name }
    }
    
    val myStatus = remember(event.guests, currentUserId) {
        event.guests[currentUserId]?.let { GuestStatus.valueOf(it) } ?: GuestStatus.INVITED
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                
                // Title & Host Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.label_hosted_by, event.hostName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge (for invited events)
                if (event.hostId != currentUserId) {
                    val badgeColor = when (myStatus) {
                        GuestStatus.ACCEPTED -> MaterialTheme.colorScheme.available
                        GuestStatus.DECLINED -> MaterialTheme.colorScheme.busy
                        GuestStatus.INVITED -> MaterialTheme.colorScheme.primary
                    }
                    val badgeText = when (myStatus) {
                        GuestStatus.ACCEPTED -> stringResource(R.string.label_accepted)
                        GuestStatus.DECLINED -> stringResource(R.string.label_declined)
                        GuestStatus.INVITED -> stringResource(R.string.label_invited)
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, badgeColor.copy(alpha = 0.3f), CircleShape)
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val dateText = formatPlanDateInFull(event.startDate)
                val timeText = formatTimeForDisplay(event.startTime, use24Hour)
                Text(
                    text = "$dateText @ $timeText (${event.timezone})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Location
            if (event.locationName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.locationName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Guests Count & Type indicator
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$acceptedCount accepted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Public/Private indicator
                Text(
                    text = event.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

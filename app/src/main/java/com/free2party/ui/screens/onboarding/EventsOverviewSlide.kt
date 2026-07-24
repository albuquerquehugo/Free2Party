package com.free2party.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.free2party.R
import com.free2party.ui.components.BadgedIconContainer
import com.free2party.ui.components.basic.AppDropdownMenu
import com.free2party.ui.components.NumberBadge
import com.free2party.ui.components.ProfileAvatar
import com.free2party.ui.components.ProfileAvatarSize
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.theme.availableContainer
import com.free2party.ui.theme.eventContainer
import com.free2party.ui.theme.onAvailableContainer
import com.free2party.ui.theme.onEventContainer
import com.free2party.ui.theme.onPendingContainer
import com.free2party.ui.theme.pendingContainer

@Composable
fun LiveEventsOverviewPreview() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            selectedFilter = "ALL"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Symmetrical Top bar mock
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Filter icon button on the left (Badge 1)
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Box(
                        modifier = Modifier
                            .clickable { showFilterMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedIconContainer(number = 1) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val isFilterActive = selectedFilter != "ALL"
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
                                    contentDescription = null,
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                    }

                    AppDropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        val isFilterEnabled = selectedTab != 0
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.label_all_events),
                                    fontWeight = if (selectedFilter == "ALL") FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (selectedFilter == "ALL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedFilter = "ALL"
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            enabled = isFilterEnabled,
                            text = {
                                Text(
                                    text = stringResource(R.string.label_going_self),
                                    fontWeight = if (selectedFilter == "GOING") FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (!isFilterEnabled) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    } else if (selectedFilter == "GOING") {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                selectedFilter = "GOING"
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            enabled = isFilterEnabled,
                            text = {
                                Text(
                                    text = stringResource(R.string.label_not_going_self),
                                    fontWeight = if (selectedFilter == "NOT_GOING") FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (!isFilterEnabled) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    } else if (selectedFilter == "NOT_GOING") {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                selectedFilter = "NOT_GOING"
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            enabled = isFilterEnabled,
                            text = {
                                Text(
                                    text = stringResource(R.string.label_pending),
                                    fontWeight = if (selectedFilter == "PENDING") FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (!isFilterEnabled) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    } else if (selectedFilter == "PENDING") {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                selectedFilter = "PENDING"
                                showFilterMenu = false
                            }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.label_events),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Add icon button on the right
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Tab selectors (Badge 2)
            BadgedIconContainer(number = 2) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = stringResource(R.string.label_tab_my_events),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.label_invited),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .requiredSize(16.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "1",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            )
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            BadgedIconContainer(number = 4) {
                                Text(
                                    text = stringResource(R.string.label_tab_public),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            }

            // Mock Search Bar for Public Tab (Badge 4)
            if (selectedTab == 2) {
                BadgedIconContainer(number = 4) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        enabled = false,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.placeholder_search_events),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.2f
                            ),
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.6f
                            )
                        ),
                        singleLine = true
                    )
                }
            }

            // Tab Contents List (Badge 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                BadgedIconContainer(number = 3) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                MockEventCard(
                                    title = stringResource(R.string.onboarding_mock_event_pizza_title),
                                    hostName = null,
                                    dateText = stringResource(R.string.onboarding_mock_event_pizza_date),
                                    locationText = "${stringResource(R.string.onboarding_mock_event_pizza_location)} (${
                                        stringResource(
                                            R.string.onboarding_mock_event_pizza_distance
                                        )
                                    })",
                                    guestCount = 3,
                                    isPublic = false,
                                    durationText = "3h",
                                    descriptionText = stringResource(R.string.onboarding_mock_event_pizza_description)
                                )
                            }

                            1 -> {
                                val showBeachVolleyball =
                                    selectedFilter == "ALL" || selectedFilter == "PENDING"
                                if (showBeachVolleyball) {
                                    MockEventCard(
                                        title = stringResource(R.string.onboarding_mock_event_volleyball_title),
                                        hostName = stringResource(R.string.onboarding_mock_event_host1),
                                        dateText = stringResource(R.string.onboarding_mock_event_volleyball_date),
                                        locationText = "${stringResource(R.string.onboarding_mock_event_volleyball_location)} (${
                                            stringResource(
                                                R.string.onboarding_mock_event_volleyball_distance
                                            )
                                        })",
                                        guestCount = 5,
                                        isPublic = false,
                                        statusText = stringResource(R.string.label_pending),
                                        statusBackground = MaterialTheme.colorScheme.pendingContainer,
                                        statusColor = MaterialTheme.colorScheme.onPendingContainer,
                                        durationText = "2h",
                                        descriptionText = stringResource(R.string.onboarding_mock_event_volleyball_description)
                                    )
                                }
                            }

                            2 -> {
                                val showArtWalk =
                                    selectedFilter == "ALL" || selectedFilter == "GOING"
                                if (showArtWalk) {
                                    MockEventCard(
                                        title = stringResource(R.string.onboarding_mock_event_artwalk_title),
                                        hostName = stringResource(R.string.onboarding_mock_event_host2),
                                        dateText = stringResource(R.string.onboarding_mock_event_artwalk_date),
                                        locationText = "${stringResource(R.string.onboarding_mock_event_artwalk_location)} (${
                                            stringResource(
                                                R.string.onboarding_mock_event_artwalk_distance
                                            )
                                        })",
                                        guestCount = 28,
                                        isPublic = true,
                                        statusText = stringResource(R.string.label_going_self),
                                        statusBackground = MaterialTheme.colorScheme.availableContainer,
                                        statusColor = MaterialTheme.colorScheme.onAvailableContainer,
                                        durationText = "4h",
                                        descriptionText = stringResource(R.string.onboarding_mock_event_artwalk_description)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Symmetrical horizontal divider
            AppHorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: Hints 1 & 2
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        NumberBadge(
                            number = 1,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_events_filter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        NumberBadge(
                            number = 2,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_events_tabs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                // Right Column: Hints 3 & 4
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        NumberBadge(
                            number = 3,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_events_tap),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        NumberBadge(
                            number = 4,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_events_public),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MockEventCard(
    title: String,
    hostName: String?,
    dateText: String,
    locationText: String,
    guestCount: Int,
    isPublic: Boolean = true,
    statusText: String? = null,
    statusBackground: Color = Color.Unspecified,
    statusColor: Color = Color.Unspecified,
    durationText: String? = null,
    descriptionText: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.eventContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hostName != null) {
                    // Mock Host Avatar
                    ProfileAvatar(size = ProfileAvatarSize.SMALL)
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Title & Host Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onEventContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hostName != null) {
                        Text(
                            text = stringResource(R.string.label_hosted_by, hostName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onEventContainer
                        )
                    }
                }

                // Status Badge (for pending events / status)
                if (statusText != null) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier
                            .background(statusBackground, CircleShape)
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
                    tint = MaterialTheme.colorScheme.onEventContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onEventContainer
                )
                if (durationText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.onEventContainer.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onEventContainer.copy(alpha = 0.2f),
                                CircleShape
                            )
                            .padding(horizontal = 8.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onEventContainer
                        )
                    }
                }
            }

            // Description (if any)
            if (descriptionText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onEventContainer,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onEventContainer,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onEventContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onEventContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Guests Going & Type Badge
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onEventContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pluralStringResource(
                            R.plurals.label_guests_going,
                            guestCount,
                            guestCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onEventContainer
                    )
                }

                Text(
                    text = stringResource(if (isPublic) R.string.label_public else R.string.label_private),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (isPublic) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(
                            color =
                                if (isPublic) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}

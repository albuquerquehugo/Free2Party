package com.free2party.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.components.BadgedIconContainer
import com.free2party.ui.components.basic.AppDropdownMenu
import com.free2party.ui.components.NumberBadge
import com.free2party.ui.components.ProfileAvatar
import com.free2party.ui.components.ProfileAvatarSize
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.theme.available
import com.free2party.ui.theme.availableContainer
import com.free2party.ui.theme.busy
import com.free2party.ui.theme.busyContainer
import com.free2party.ui.theme.onAvailableContainer
import com.free2party.ui.theme.onBusyContainer

// Slide 3: Friends List with Interactive Circles Filter Chips
@Composable
fun LiveFriendsCirclesPreview() {
    val everyoneLabel = stringResource(R.string.label_everyone)
    val closeFriendsLabel = stringResource(R.string.onboarding_mock_circle_close_friends)
    val familyLabel = stringResource(R.string.onboarding_mock_circle_family)

    var selectedCircle by remember(everyoneLabel) { mutableStateOf(everyoneLabel) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val aliceName = stringResource(R.string.onboarding_mock_friend_alice)
    val bobName = stringResource(R.string.onboarding_mock_friend_bob)
    val mockFriends = remember(aliceName, bobName, closeFriendsLabel, familyLabel) {
        listOf(
            MockFriend(aliceName, closeFriendsLabel, isFree = true),
            MockFriend(bobName, familyLabel, isFree = false)
        )
    }

    val filteredFriends = remember(selectedCircle, mockFriends) {
        if (selectedCircle == everyoneLabel) mockFriends
        else mockFriends.filter { it.circle == selectedCircle }
    }

    val freeFriends = filteredFriends.filter { it.isFree }
    val busyFriends = filteredFriends.filter { !it.isFree }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
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
            // Mock Top bar section exactly like HomeScreen
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Left: Filter Icon & Dropdown menu wrapped in CenterStart Box
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { showFilterMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedIconContainer(number = 1) {
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val isFiltered = selectedCircle != everyoneLabel
                                if (isFiltered) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                            .border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.description_filter),
                                    tint = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isFiltered) {
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
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = everyoneLabel,
                                    fontWeight = if (selectedCircle == everyoneLabel) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (selectedCircle == everyoneLabel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedCircle = everyoneLabel
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = closeFriendsLabel,
                                    fontWeight = if (selectedCircle == closeFriendsLabel) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (selectedCircle == closeFriendsLabel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedCircle = closeFriendsLabel
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = familyLabel,
                                    fontWeight = if (selectedCircle == familyLabel) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (selectedCircle == familyLabel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                selectedCircle = familyLabel
                                showFilterMenu = false
                            }
                        )
                    }
                }

                // Center: Title Text
                Text(
                    text = stringResource(R.string.label_friends),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Right: Add Icon Button
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BadgedIconContainer(number = 2) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = stringResource(R.string.description_add_friend),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Expandable list items styled exactly as Home screen
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Section 1: Free
                MockExpandableSection(
                    title = stringResource(R.string.label_friend_section_free),
                    friends = freeFriends
                )

                // Section 2: Busy
                MockExpandableSection(
                    title = stringResource(R.string.label_friend_section_busy),
                    friends = busyFriends
                )
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
                    Row(verticalAlignment = Alignment.Top) {
                        NumberBadge(
                            number = 1,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_filter_circles),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        NumberBadge(
                            number = 2,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_add_friends),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Row(verticalAlignment = Alignment.Top) {
                        NumberBadge(
                            number = 3,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_calendar),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        NumberBadge(
                            number = 4,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_message_friends),
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
fun MockExpandableSection(
    title: String,
    friends: List<MockFriend>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.label_friend_section, title, friends.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = null
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (friends.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_friends_in_section),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            } else {
                friends.forEach { friend ->
                    MockFriendItem(friend = friend)
                }
            }
        }
    }
}

@Composable
fun MockFriendItem(
    friend: MockFriend
) {
    val statusColor =
        if (friend.isFree) MaterialTheme.colorScheme.available else MaterialTheme.colorScheme.busy
    val containerColor =
        if (friend.isFree) MaterialTheme.colorScheme.availableContainer else MaterialTheme.colorScheme.busyContainer
    val onContainerColor =
        if (friend.isFree) MaterialTheme.colorScheme.onAvailableContainer else MaterialTheme.colorScheme.onBusyContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                size = ProfileAvatarSize.SMALL,
                statusColor = statusColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = friend.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = onContainerColor
            )

            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BadgedIconContainer(number = 3) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = onContainerColor
                        )
                    }
                }
                BadgedIconContainer(number = 4) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = onContainerColor
                        )
                    }
                }
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = onContainerColor
                    )
                }
            }
        }
    }
}

data class MockFriend(val name: String, val circle: String, val isFree: Boolean)

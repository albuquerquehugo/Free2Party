package com.example.free2party.ui.screens.notifications

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.R
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.Notification
import com.example.free2party.ui.theme.Red80
import com.example.free2party.ui.theme.inactive

@Composable
fun NotificationsRoute(viewModel: NotificationsViewModel = viewModel()) {
    val items by viewModel.notificationItems.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    NotificationsScreen(
        items = items,
        unreadCount = unreadCount,
        onAcceptRequest = { viewModel.acceptFriendRequest(it) },
        onDeclineRequest = { viewModel.declineFriendRequest(it.id) },
        onToggleRead = { viewModel.toggleReadStatus(it) },
        onDelete = { viewModel.deleteNotification(it) },
        onMarkVisibleAsRead = { viewModel.markAllVisibleAsRead(it) }
    )
}

@Composable
fun NotificationsScreen(
    items: List<NotificationItem>,
    unreadCount: Int,
    onAcceptRequest: (FriendRequest) -> Unit,
    onDeclineRequest: (FriendRequest) -> Unit,
    onToggleRead: (Notification) -> Unit,
    onDelete: (String) -> Unit,
    onMarkVisibleAsRead: (List<String>) -> Unit
) {
    val listState = rememberLazyListState()

    // Track which unread notifications are currently visible
    val visibleUnreadIds by remember(listState, items) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { visibleItem ->
                val item = items.getOrNull(visibleItem.index)
                if (item is NotificationItem.Info && !item.notification.isRead) {
                    item.notification.id
                } else null
            }
        }
    }

    // Capture the latest visible IDs to ensure the dispose effect uses fresh data
    val latestVisibleUnreadIds by rememberUpdatedState(visibleUnreadIds)

    // TODO: Change behavior to mark as read if the notification appeared once in the screen
    DisposableEffect(onMarkVisibleAsRead) {
        onDispose {
            if (latestVisibleUnreadIds.isNotEmpty()) {
                onMarkVisibleAsRead(latestVisibleUnreadIds)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.free2party_full_transparent_light),
                contentDescription = "Free2Party Logo",
                modifier = Modifier.height(20.dp),
                contentScale = ContentScale.Fit
            )
        }

        val titleText = if (unreadCount > 0) "Notifications ($unreadCount)" else "Notifications"
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.inactive
                )
                Text("All caught up!", color = MaterialTheme.colorScheme.inactive)
            }
        } else {
            LazyColumn(state = listState) {
                items(items, key = { it.id }) { item ->
                    when (item) {
                        is NotificationItem.Request -> {
                            FriendRequestItem(
                                request = item.friendRequest,
                                onAccept = { onAcceptRequest(item.friendRequest) },
                                onDecline = { onDeclineRequest(item.friendRequest) }
                            )
                        }

                        is NotificationItem.Info -> {
                            DismissibleNotificationItem(
                                notification = item.notification,
                                onToggleRead = { onToggleRead(item.notification) },
                                onDelete = { onDelete(item.notification.id) }
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissibleNotificationItem(
    notification: Notification,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    val currentOnToggleRead by rememberUpdatedState(onToggleRead)
    val currentOnDelete by rememberUpdatedState(onDelete)

    key(notification.id, notification.isRead) {
        @Suppress("DEPRECATION")
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        currentOnToggleRead()
                        false // Snap back to center after release
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        currentOnDelete()
                        true // Perform dismissal
                    }

                    else -> false
                }
            },
            positionalThreshold = { it * 0.6f }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = { DismissBackground(notification, dismissState) },
            content = { NotificationBox(notification) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissBackground(notification: Notification, dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    if (direction == SwipeToDismissBoxValue.Settled) return

    val markAsReadColor = Color(0xFF4CAF50) // Green
    val markAsUnreadColor = Color(0xFF2196F3) // Blue

    // TODO: Improve dismiss background color
    val backgroundColor by animateColorAsState(
        when (dismissState.targetValue) {
            SwipeToDismissBoxValue.StartToEnd ->
                if (notification.isRead) markAsUnreadColor else markAsReadColor

            SwipeToDismissBoxValue.EndToStart -> Red80
            else -> Color.Transparent
        },
        label = "backgroundColor"
    )

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
    }

    val icon: ImageVector? = when (direction) {
        SwipeToDismissBoxValue.StartToEnd ->
            if (notification.isRead) Icons.Default.MarkEmailUnread
            else Icons.Default.MarkEmailRead

        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
    }

    val scale by animateFloatAsState(
        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f
        else 1.2f,
        label = "iconScale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.scale(scale),
                tint =
                    if (direction == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error
                    else if (notification.isRead) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.inactive
            )
        }
    }
}

@Composable
fun NotificationBox(notification: Notification) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        val backgroundColor =
            if (notification.isRead) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                .compositeOver(MaterialTheme.colorScheme.surface)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        .compositeOver(MaterialTheme.colorScheme.surface)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Friend Request: ${request.senderName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = request.senderEmail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { onDecline() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Decline",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onAccept() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Accept",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

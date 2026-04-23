package com.example.free2party.ui.screens.notifications

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.Notification
import com.example.free2party.ui.theme.inactive
import com.example.free2party.util.formatTimeAgo
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NotificationsRoute(viewModel: NotificationsViewModel) {
    val context = LocalContext.current
    val items by viewModel.notificationItems.collectAsState()
    val itemsUnreadCount by viewModel.itemsUnreadCount.collectAsState()
    val notificationsUnreadCount by viewModel.notificationsUnreadCount.collectAsState()
    val gradientBackground by viewModel.gradientBackground.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is NotificationsUiEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        event.message.asString(context),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    NotificationsScreen(
        items = items,
        itemsUnreadCount = itemsUnreadCount,
        notificationsUnreadCount = notificationsUnreadCount,
        gradientBackground = gradientBackground,
        onAcceptRequest = { viewModel.acceptFriendRequest(it.id) },
        onDeclineRequest = { viewModel.declineFriendRequest(it.id) },
        onDeclineAndBlockRequest = { viewModel.declineAndBlockFriendRequest(it.id) },
        onToggleRead = { viewModel.toggleReadStatus(it) },
        onDelete = { viewModel.deleteNotification(it) },
        onMarkAllAsRead = { viewModel.markAllAsRead() }
    )
}

@Composable
fun NotificationsScreen(
    items: List<NotificationItem>,
    itemsUnreadCount: Int,
    notificationsUnreadCount: Int,
    gradientBackground: Boolean,
    onAcceptRequest: (FriendRequest) -> Unit,
    onDeclineRequest: (FriendRequest) -> Unit,
    onDeclineAndBlockRequest: (FriendRequest) -> Unit,
    onToggleRead: (Notification) -> Unit,
    onDelete: (String) -> Unit,
    onMarkAllAsRead: () -> Unit
) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.free2party_full_transparent),
                contentDescription = stringResource(R.string.description_logo_content),
                modifier = Modifier.height(20.dp),
                contentScale = ContentScale.Fit
            )
        }

        val titleText = if (itemsUnreadCount > 0) {
            stringResource(R.string.title_notifications_with_count, itemsUnreadCount)
        } else {
            stringResource(R.string.title_notifications)
        }

        Text(
            text = titleText,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
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
                Text(
                    text = stringResource(R.string.text_all_caught_up),
                    color = MaterialTheme.colorScheme.inactive
                )
            }
        } else {
            val requests = items.filterIsInstance<NotificationItem.Request>()
            val notifications = items.filterIsInstance<NotificationItem.Info>()

            LazyColumn(state = listState, horizontalAlignment = Alignment.CenterHorizontally) {
                if (requests.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                    items(requests, key = { it.id }) { item ->
                        FriendRequestItem(
                            request = item.friendRequest,
                            gradientBackground = gradientBackground,
                            onAccept = { onAcceptRequest(item.friendRequest) },
                            onDecline = { onDeclineRequest(item.friendRequest) },
                            onDeclineAndBlock = { onDeclineAndBlockRequest(item.friendRequest) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }
                }

                if (notifications.isNotEmpty()) {
                    item {
                        TextButton(
                            enabled = notificationsUnreadCount > 0,
                            onClick = onMarkAllAsRead,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(stringResource(R.string.label_mark_all_as_read))
                        }
                    }
                    item {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }
                    items(notifications, key = { it.id }) { item ->
                        DismissibleNotificationItem(
                            notification = item.notification,
                            gradientBackground = gradientBackground,
                            onToggleRead = { onToggleRead(item.notification) },
                            onDelete = { onDelete(item.notification.id) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    gradientBackground: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDeclineAndBlock: () -> Unit
) {
    val (showDeclineDialog, setShowDeclineDialog) = remember { mutableStateOf(false) }

    if (showDeclineDialog) {
        DeclineFriendRequestDialog(
            onDismiss = { setShowDeclineDialog(false) },
            onDeclineOnly = {
                onDecline()
                setShowDeclineDialog(false)
            },
            onDeclineAndBlock = {
                onDeclineAndBlock()
                setShowDeclineDialog(false)
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (gradientBackground) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.primaryContainer
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.title_friend_request),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (request.senderProfilePicUrl.isNotBlank()) {
                    AsyncImage(
                        model = request.senderProfilePicUrl,
                        contentDescription = stringResource(R.string.label_friends_picture),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = request.senderName,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = request.senderEmail,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatTimeAgo(request.timestamp).asString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { setShowDeclineDialog(true) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.label_decline),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(20.dp)
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
                    contentDescription = stringResource(R.string.label_accept),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DeclineFriendRequestDialog(
    onDismiss: () -> Unit,
    onDeclineOnly: () -> Unit,
    onDeclineAndBlock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.title_decline_dialog))
        },
        text = {
            Text(text = stringResource(R.string.text_decline_dialog_message))
        },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDeclineOnly,
                    modifier = Modifier.width(160.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.label_decline_only))
                }
                Button(
                    onClick = onDeclineAndBlock,
                    modifier = Modifier.width(160.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.label_decline_and_block))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.width(160.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = stringResource(R.string.button_cancel))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissibleNotificationItem(
    notification: Notification,
    gradientBackground: Boolean,
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
                        false
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        currentOnDelete()
                        true
                    }

                    else -> false
                }
            },
            positionalThreshold = { it * 0.6f }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = { DismissBackground(notification, dismissState) },
            content = {
                NotificationBox(
                    notification = notification,
                    gradientBackground = gradientBackground,
                    onToggleRead = currentOnToggleRead,
                    onDelete = currentOnDelete
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissBackground(notification: Notification, dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    if (direction == SwipeToDismissBoxValue.Settled) return

    val backgroundColor by animateColorAsState(
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd ->
                if (notification.isRead) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent

            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer
        },
        label = "backgroundColor"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        contentAlignment =
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            }
    ) {
        val icon: ImageVector? = when (direction) {
            SwipeToDismissBoxValue.StartToEnd ->
                if (notification.isRead) Icons.Default.MarkEmailUnread
                else Icons.Default.MarkEmailRead

            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        }

        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (notification.isRead) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationBox(
    notification: Notification,
    gradientBackground: Boolean,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val backgroundColor = when {
        notification.isRead && gradientBackground -> Color.Transparent
        notification.isRead -> MaterialTheme.colorScheme.background
        gradientBackground -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = notification.message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
            )

            Text(
                text = formatTimeAgo(notification.timestamp).asString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.label_options_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (notification.isRead) stringResource(R.string.label_mark_as_unread)
                            else stringResource(R.string.label_mark_as_read)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                if (notification.isRead) Icons.Default.MarkEmailUnread
                                else Icons.Default.MarkEmailRead,
                            tint =
                                if (notification.isRead) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onToggleRead()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.text_delete)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}

package com.example.free2party.ui.screens.home

import android.widget.Toast
import com.example.free2party.ui.screens.friends.FriendViewModel
import com.example.free2party.ui.screens.friends.FriendUiEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.ui.components.AdBanner
import com.example.free2party.ui.components.dialogs.AboutDialog
import com.example.free2party.ui.components.dialogs.ConfirmationDialog
import com.example.free2party.ui.components.dialogs.FriendCalendarDialog
import com.example.free2party.ui.theme.available
import com.example.free2party.ui.theme.availableContainer
import com.example.free2party.ui.theme.busy
import com.example.free2party.ui.theme.busyContainer
import com.example.free2party.ui.theme.inactiveContainer
import com.example.free2party.ui.theme.onAvailableContainer
import com.example.free2party.ui.theme.onBusyContainer
import com.example.free2party.ui.theme.onInactiveContainer
import com.example.free2party.ui.theme.TelegramColor
import com.example.free2party.ui.theme.WhatsAppColor
import com.example.free2party.util.SocialPlatform
import com.example.free2party.util.openEmail
import com.example.free2party.util.openSMS
import com.example.free2party.util.openSocialMessage
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel,
    friendViewModel: FriendViewModel,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInviteFriend: () -> Unit
) {
    val context = LocalContext.current

    val inviteSentTemplate = stringResource(R.string.toast_invite_sent)

    LaunchedEffect(Unit) {
        homeViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        event.message.asString(context),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is HomeUiEvent.Logout -> onLogout()
            }
        }
    }

    LaunchedEffect(Unit) {
        friendViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is FriendUiEvent.InviteSentSuccessfully -> {
                    Toast.makeText(
                        context,
                        inviteSentTemplate,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val gradientBackground =
        (homeViewModel.uiState as? HomeUiState.Success)?.gradientBackground ?: true

    HomeScreen(
        homeUiState = homeViewModel.uiState,
        gradientBackground = gradientBackground,
        onLogoutClick = { homeViewModel.logout(onLogout) },
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToBlockedUsers = onNavigateToBlockedUsers,
        onNavigateToSettings = onNavigateToSettings,
        onToggleAvailability = { homeViewModel.toggleAvailability() },
        onRemoveFriend = { uid -> homeViewModel.removeFriend(uid) },
        onRemoveAndBlockFriend = { uid -> homeViewModel.removeAndBlockFriend(uid) },
        onCancelInvite = { uid -> homeViewModel.cancelFriendInvite(uid) },
        onInviteFriendClick = onNavigateToInviteFriend
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeUiState: HomeUiState,
    gradientBackground: Boolean,
    onLogoutClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onToggleAvailability: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    onRemoveAndBlockFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onInviteFriendClick: () -> Unit
) {
    var showUserMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRemoveFriendDialog by remember { mutableStateOf(false) }
    var friendIdToRemove by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var selectedFriend by remember { mutableStateOf<FriendInfo?>(null) }
    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Redirect initial focus to the root container to avoid highlighting the user menu
        rootFocusRequester.requestFocus()
    }

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(rootFocusRequester)
                .focusable()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .clickable { showUserMenu = true }
                        .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val successState = homeUiState as? HomeUiState.Success
                    val profilePicUrl = successState?.profilePicUrl
                    val isUserFree = successState?.isUserFree ?: false
                    val statusColor =
                        if (isUserFree) MaterialTheme.colorScheme.available else MaterialTheme.colorScheme.busy

                    if (successState != null) {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = successState.userName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text =
                                    if (isUserFree) stringResource(R.string.label_status_free)
                                    else stringResource(
                                        successState.userGender.getStringRes(R.string.label_status_busy)
                                    ),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .border(3.dp, statusColor, CircleShape)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePicUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = stringResource(R.string.description_user_menu),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = stringResource(R.string.description_user_menu),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_profile)) },
                                onClick = {
                                    showUserMenu = false
                                    onNavigateToProfile()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_blocked_users)) },
                                onClick = {
                                    showUserMenu = false
                                    onNavigateToBlockedUsers()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_settings)) },
                                onClick = {
                                    showUserMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_about)) },
                                onClick = {
                                    showUserMenu = false
                                    showAboutDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null
                                    )
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.text_logout)) },
                                onClick = {
                                    showUserMenu = false
                                    showLogoutDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            when (homeUiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is HomeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = homeUiState.message.asString(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is HomeUiState.Success -> {
                    HomeContent(
                        paddingValues = paddingValues,
                        isUserFree = homeUiState.isUserFree,
                        friendsList = homeUiState.friendsList,
                        isActionLoading = homeUiState.isActionLoading,
                        gradientBackground = gradientBackground,
                        onToggleAvailability = onToggleAvailability,
                        onRemoveFriend = { uid ->
                            friendIdToRemove = uid
                            showRemoveFriendDialog = true
                        },
                        onCancelInvite = onCancelInvite,
                        onInviteFriendClick = onInviteFriendClick,
                        onFriendItemClick = { friend -> selectedFriend = friend }
                    )
                }
            }
        }

        if (showLogoutDialog) {
            ConfirmationDialog(
                title = stringResource(R.string.text_logout),
                text = stringResource(R.string.text_logout_confirmation),
                confirmButtonText = stringResource(R.string.text_logout),
                onConfirm = {
                    showLogoutDialog = false
                    onLogoutClick()
                },
                dismissButtonText = stringResource(R.string.button_cancel),
                onDismiss = { showLogoutDialog = false },
                isDestructive = true
            )
        }

        if (showRemoveFriendDialog) {
            ConfirmationDialog(
                title = stringResource(R.string.title_remove_friend),
                text = stringResource(R.string.text_remove_friend_confirmation),
                confirmButtonText = stringResource(R.string.button_remove),
                onConfirm = {
                    showRemoveFriendDialog = false
                    friendIdToRemove?.let { onRemoveFriend(it) }
                    friendIdToRemove = null
                },
                secondaryButtonText = stringResource(R.string.title_remove_and_block_friend),
                onSecondaryAction = {
                    showRemoveFriendDialog = false
                    friendIdToRemove?.let { onRemoveAndBlockFriend(it) }
                    friendIdToRemove = null
                },
                dismissButtonText = stringResource(R.string.button_cancel),
                onDismiss = {
                    showRemoveFriendDialog = false
                    friendIdToRemove = null
                },
                isDestructive = true
            )
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }

        selectedFriend?.let { friend ->
            FriendCalendarDialog(
                friend = friend,
                onDismiss = { selectedFriend = null }
            )
        }
    }
}

@Composable
fun HomeContent(
    paddingValues: PaddingValues,
    isUserFree: Boolean,
    friendsList: List<FriendInfo>,
    isActionLoading: Boolean,
    gradientBackground: Boolean,
    onToggleAvailability: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onInviteFriendClick: () -> Unit,
    onFriendItemClick: (FriendInfo) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val logoScale by animateFloatAsState(targetValue = if (isUserFree) 1.0f else 0.9f)
            val glowColor =
                if (!isUserFree) MaterialTheme.colorScheme.available else MaterialTheme.colorScheme.busy
            val containerColor =
                if (!isUserFree) MaterialTheme.colorScheme.availableContainer else MaterialTheme.colorScheme.busyContainer
            val onContainerColor =
                if (!isUserFree) MaterialTheme.colorScheme.onAvailableContainer else MaterialTheme.colorScheme.onBusyContainer

            ElevatedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleAvailability()
                },
                enabled = !isActionLoading,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .height(64.dp)
                    .wrapContentWidth()
                    .shadow(
                        elevation = if (!isUserFree) 16.dp else 8.dp,
                        spotColor = glowColor,
                        shape = CircleShape,
                        ambientColor = glowColor,
                        clip = false
                    ),
                shape = CircleShape,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = containerColor,
                    contentColor = onContainerColor
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isActionLoading) 0f else 1f
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.free2party_full_transparent),
                            contentDescription = null,
                            modifier = Modifier
                                .height(28.dp)
                                .graphicsLayer(
                                    scaleX = logoScale,
                                    scaleY = logoScale
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }

                    if (isActionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = onContainerColor
                        )
                    }
                }
            }

            FriendsListSection(
                friends = friendsList,
                gradientBackground = gradientBackground,
                onRemoveFriend = onRemoveFriend,
                onCancelInvite = onCancelInvite,
                onInviteFriendClick = onInviteFriendClick,
                onFriendItemClick = onFriendItemClick
            )
        }

        AdBanner()
    }
}

@Composable
fun FriendsListSection(
    friends: List<FriendInfo>,
    gradientBackground: Boolean,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onInviteFriendClick: () -> Unit,
    onFriendItemClick: (FriendInfo) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.title_your_friends),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(
            onClick = onInviteFriendClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = stringResource(R.string.title_invite_friend),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (friends.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.text_no_friends_yet),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.text_add_friends),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val freeFriends =
            friends.filter { it.inviteStatus == InviteStatus.ACCEPTED && it.isFreeNow }
        val busyFriends =
            friends.filter { it.inviteStatus == InviteStatus.ACCEPTED && !it.isFreeNow }
        val invitedFriends = friends.filter { it.inviteStatus == InviteStatus.INVITED }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                ExpandableFriendSection(
                    title = stringResource(R.string.title_friend_section_free),
                    friends = freeFriends,
                    gradientBackground = gradientBackground,
                    onRemoveFriend = onRemoveFriend,
                    onCancelInvite = onCancelInvite,
                    onClick = onFriendItemClick
                )
            }
            item {
                ExpandableFriendSection(
                    title = stringResource(R.string.title_friend_section_busy),
                    friends = busyFriends,
                    gradientBackground = gradientBackground,
                    onRemoveFriend = onRemoveFriend,
                    onCancelInvite = onCancelInvite,
                    onClick = onFriendItemClick
                )
            }
            item {
                ExpandableFriendSection(
                    title = stringResource(R.string.title_friend_section_invited),
                    friends = invitedFriends,
                    gradientBackground = gradientBackground,
                    onRemoveFriend = onRemoveFriend,
                    onCancelInvite = onCancelInvite,
                    onClick = onFriendItemClick
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ExpandableFriendSection(
    title: String,
    friends: List<FriendInfo>,
    gradientBackground: Boolean,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onClick: (FriendInfo) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "rotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.title_friend_section, title, friends.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription =
                    if (isExpanded) stringResource(R.string.description_collapse)
                    else stringResource(R.string.description_expand),
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
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
                        FriendItem(
                            friend = friend,
                            gradientBackground = gradientBackground,
                            onRemoveFriend = { onRemoveFriend(friend.uid) },
                            onCancelInvite = { onCancelInvite(friend.uid) },
                            onClick = { onClick(friend) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: FriendInfo,
    gradientBackground: Boolean,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onClick: () -> Unit
) {
    var showFriendMenu by remember { mutableStateOf(false) }
    var showContactMenu by remember { mutableStateOf(false) }
    val isInvited = friend.inviteStatus == InviteStatus.INVITED
    val context = LocalContext.current
    val hangOutMessage = stringResource(R.string.text_hang_out_message)

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = { if (!isInvited) onClick() },
                    onLongClick = { showFriendMenu = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor = (when {
                    isInvited -> MaterialTheme.colorScheme.inactiveContainer
                    friend.isFreeNow -> MaterialTheme.colorScheme.availableContainer
                    else -> MaterialTheme.colorScheme.busyContainer
                }).let { if (gradientBackground) it.copy(alpha = 0.7f) else it }
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isInvited) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = stringResource(R.string.label_status_pending),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onInactiveContainer
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (friend.isFreeNow) MaterialTheme.colorScheme.available
                                    else MaterialTheme.colorScheme.busy
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    color = when {
                        isInvited -> MaterialTheme.colorScheme.onInactiveContainer
                        friend.isFreeNow -> MaterialTheme.colorScheme.onAvailableContainer
                        else -> MaterialTheme.colorScheme.onBusyContainer
                    }
                )

                if (!isInvited) {
                    Box {
                        IconButton(onClick = { showContactMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = stringResource(R.string.description_contact_options),
                                tint = if (friend.isFreeNow) MaterialTheme.colorScheme.onAvailableContainer
                                else MaterialTheme.colorScheme.onBusyContainer
                            )
                        }

                        DropdownMenu(
                            expanded = showContactMenu,
                            onDismissRequest = { showContactMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            if (friend.email.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_email)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openEmail(context, friend.email)
                                    }
                                )
                            }

                            val smsNumber = "${friend.phoneCode}${friend.phoneNumber}"
                            if (smsNumber.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_sms)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Sms,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openSMS(context, smsNumber)
                                    }
                                )
                            }

                            if (friend.socials.facebookUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_facebook_messenger)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.messenger_color),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openSocialMessage(
                                            context,
                                            SocialPlatform.MESSENGER,
                                            friend.socials.facebookUsername
                                        )
                                    }
                                )
                            }

                            if (friend.socials.instagramUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_instagram)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.instagram_color),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openSocialMessage(
                                            context,
                                            SocialPlatform.INSTAGRAM,
                                            friend.socials.instagramUsername
                                        )
                                    }
                                )
                            }

                            if (friend.socials.telegramUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_telegram)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.telegram),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = TelegramColor
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openSocialMessage(
                                            context,
                                            SocialPlatform.TELEGRAM,
                                            friend.socials.telegramUsername
                                        )
                                    }
                                )
                            }

                            if (friend.socials.tiktokUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_tiktok)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.tiktok_color),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openSocialMessage(
                                            context,
                                            SocialPlatform.TIKTOK,
                                            friend.socials.tiktokUsername
                                        )
                                    }
                                )
                            }

                            val waNumber = friend.socials.whatsappFullNumber
                            if (waNumber.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_whatsapp)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.whatsapp),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = WhatsAppColor
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openSocialMessage(
                                            context,
                                            SocialPlatform.WHATSAPP,
                                            waNumber,
                                            message = hangOutMessage
                                        )
                                    }
                                )
                            }

                            if (friend.socials.xUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_x)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.x),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openSocialMessage(
                                            context,
                                            SocialPlatform.X,
                                            friend.socials.xUsername
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showFriendMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.description_friend_options),
                            tint = when {
                                isInvited -> MaterialTheme.colorScheme.onInactiveContainer
                                friend.isFreeNow -> MaterialTheme.colorScheme.onAvailableContainer
                                else -> MaterialTheme.colorScheme.onBusyContainer
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = showFriendMenu,
                        onDismissRequest = { showFriendMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isInvited) stringResource(R.string.label_cancel_invite) else stringResource(
                                        R.string.title_remove_friend
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showFriendMenu = false
                                if (isInvited) {
                                    onCancelInvite(friend.uid)
                                } else {
                                    onRemoveFriend(friend.uid)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

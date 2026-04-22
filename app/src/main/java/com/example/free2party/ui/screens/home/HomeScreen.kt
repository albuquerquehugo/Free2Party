package com.example.free2party.ui.screens.home

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.ui.components.AdBanner
import com.example.free2party.ui.components.dialogs.AboutDialog
import com.example.free2party.ui.components.dialogs.ConfirmationDialog
import com.example.free2party.ui.components.dialogs.EmailDialog
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
    homeViewModel: HomeViewModel = viewModel(),
    friendViewModel: FriendViewModel = viewModel(),
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val (showInviteFriendDialog, setShowInviteFriendDialog) = remember { mutableStateOf(false) }

    val inviteSentTemplate = stringResource(R.string.message_invite_sent)

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
                        inviteSentTemplate.format(event.email),
                        Toast.LENGTH_SHORT
                    ).show()
                    setShowInviteFriendDialog(false)
                }
            }
        }
    }

    val gradientBackground =
        (homeViewModel.uiState as? HomeUiState.Success)?.gradientBackground ?: true

    HomeScreen(
        homeUiState = homeViewModel.uiState,
        inviteFriendUiState = friendViewModel.uiState,
        showInviteFriendDialog = showInviteFriendDialog,
        gradientBackground = gradientBackground,
        onLogoutClick = { homeViewModel.logout(onLogout) },
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToSettings = onNavigateToSettings,
        onToggleAvailability = { homeViewModel.toggleAvailability() },
        onRemoveFriend = { uid -> homeViewModel.removeFriend(uid) },
        onCancelInvite = { uid -> homeViewModel.cancelFriendInvite(uid) },
        onInviteFriendClick = { setShowInviteFriendDialog(true) },
        onInviteFriendConfirm = { email -> friendViewModel.inviteFriend(email) },
        onInviteFriendDismiss = {
            friendViewModel.resetState()
            setShowInviteFriendDialog(false)
        },
        onInviteFriendResetState = { friendViewModel.resetState() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeUiState: HomeUiState,
    inviteFriendUiState: InviteFriendUiState,
    showInviteFriendDialog: Boolean,
    gradientBackground: Boolean,
    onLogoutClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onToggleAvailability: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onInviteFriendClick: () -> Unit,
    onInviteFriendConfirm: (String) -> Unit,
    onInviteFriendDismiss: () -> Unit,
    onInviteFriendResetState: () -> Unit
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
                    if (successState != null) {
                        Text(
                            text = successState.userName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        val profilePicUrl = (homeUiState as? HomeUiState.Success)?.profilePicUrl
                        val isUserFree = (homeUiState as? HomeUiState.Success)?.isUserFree ?: false

                        if (!profilePicUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = stringResource(R.string.description_user_menu),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = stringResource(R.string.description_user_menu),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Status Circle Badge
                        if (homeUiState is HomeUiState.Success) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(
                                        color =
                                            if (isUserFree) MaterialTheme.colorScheme.available
                                            else MaterialTheme.colorScheme.busy,
                                        CircleShape
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            if (homeUiState is HomeUiState.Success) {
                                DropdownMenuItem(
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row {
                                                Text(
                                                    text = stringResource(R.string.status),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text =
                                                        if (isUserFree) stringResource(R.string.status_free)
                                                        else stringResource(R.string.status_busy),
                                                    color =
                                                        if (isUserFree) MaterialTheme.colorScheme.available
                                                        else MaterialTheme.colorScheme.busy
                                                )
                                            }
                                        }
                                    },
                                    onClick = { },
                                    enabled = false
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
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
                            HorizontalDivider()
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
                text = stringResource(R.string.logout_confirmation_text),
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
                title = stringResource(R.string.remove_friend),
                text = stringResource(R.string.remove_friend_confirmation_text),
                confirmButtonText = stringResource(R.string.button_remove),
                onConfirm = {
                    showRemoveFriendDialog = false
                    friendIdToRemove?.let { onRemoveFriend(it) }
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

        if (showInviteFriendDialog) {
            var inviteEmail by remember { mutableStateOf("") }
            EmailDialog(
                title = stringResource(R.string.invite_friend),
                description = stringResource(R.string.invite_friend_description),
                email = inviteEmail,
                onValueChange = {
                    inviteEmail = it
                    if (inviteFriendUiState is InviteFriendUiState.Error) onInviteFriendResetState()
                },
                onDismiss = onInviteFriendDismiss,
                onConfirm = { onInviteFriendConfirm(inviteEmail) },
                isLoading = inviteFriendUiState is InviteFriendUiState.Searching,
                errorMessage =
                    if (inviteFriendUiState is InviteFriendUiState.Error) {
                        inviteFriendUiState.message.asString()
                    } else null,
                confirmButtonLabel = stringResource(R.string.send_invite)
            )
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
            val logoScale by animateFloatAsState(targetValue = if (!isUserFree) 1.0f else 0.9f)
            val glowColor =
                if (!isUserFree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

            Card(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .shadow(
                        elevation = if (!isUserFree) 20.dp else 10.dp,
                        spotColor = glowColor,
                        shape = CircleShape,
                        ambientColor = glowColor,
                        clip = false
                    )
                    .clip(CircleShape)
                    .clickable(
                        enabled = !isActionLoading,
                        onClick = onToggleAvailability
                    ),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (!isUserFree) 8.dp else 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (!isUserFree) MaterialTheme.colorScheme.availableContainer
                        else MaterialTheme.colorScheme.busyContainer
                )
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.free2party_full_transparent),
                        contentDescription = stringResource(R.string.description_logo_content),
                        modifier = Modifier
                            .height(32.dp)
                            .graphicsLayer(
                                scaleX = logoScale,
                                scaleY = logoScale
                            ),
                        contentScale = ContentScale.Fit
                    )

                    if (isActionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color =
                                if (!isUserFree) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.busy
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
                contentDescription = stringResource(R.string.invite_friend),
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
                text = stringResource(R.string.no_friends_yet),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.add_friends_message),
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
                    title = stringResource(R.string.section_title_free),
                    friends = freeFriends,
                    gradientBackground = gradientBackground,
                    onRemoveFriend = onRemoveFriend,
                    onCancelInvite = onCancelInvite,
                    onClick = onFriendItemClick
                )
            }
            item {
                ExpandableFriendSection(
                    title = stringResource(R.string.section_title_busy),
                    friends = busyFriends,
                    gradientBackground = gradientBackground,
                    onRemoveFriend = onRemoveFriend,
                    onCancelInvite = onCancelInvite,
                    onClick = onFriendItemClick
                )
            }
            item {
                ExpandableFriendSection(
                    title = stringResource(R.string.section_title_invited),
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
                text = "$title (${friends.size})",
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
                        text = stringResource(R.string.no_friends_in_section),
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
                            contentDescription = stringResource(R.string.status_pending),
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
                                contentDescription = stringResource(R.string.contact_options),
                                tint = if (friend.isFreeNow) MaterialTheme.colorScheme.onAvailableContainer
                                else MaterialTheme.colorScheme.onBusyContainer
                            )
                        }

                        DropdownMenu(
                            expanded = showContactMenu,
                            onDismissRequest = { showContactMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            offset = DpOffset(x = (-16).dp, y = 0.dp)
                        ) {
                            if (friend.email.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.text_email)) },
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
                                    text = { Text(stringResource(R.string.text_sms)) },
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
                                    text = { Text(stringResource(R.string.text_facebook_messenger)) },
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
                                    text = { Text(stringResource(R.string.text_instagram)) },
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
                                    text = { Text(stringResource(R.string.text_telegram)) },
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
                                    text = { Text(stringResource(R.string.text_tiktok)) },
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
                                    text = { Text(stringResource(R.string.text_whatsapp)) },
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
                                    text = { Text(stringResource(R.string.text_x)) },
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
                                    if (isInvited) stringResource(R.string.cancel_invite) else stringResource(
                                        R.string.remove_friend
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

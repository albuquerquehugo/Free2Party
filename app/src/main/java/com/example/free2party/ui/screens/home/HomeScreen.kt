package com.example.free2party.ui.screens.home

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.ui.components.dialogs.AboutDialog
import com.example.free2party.ui.components.dialogs.FriendCalendarDialog
import com.example.free2party.ui.components.dialogs.InviteFriendDialog
import com.example.free2party.ui.theme.TelegramColor
import com.example.free2party.ui.theme.available
import com.example.free2party.ui.theme.availableContainer
import com.example.free2party.ui.theme.busy
import com.example.free2party.ui.theme.busyContainer
import com.example.free2party.ui.theme.inactiveContainer
import com.example.free2party.ui.theme.onAvailableContainer
import com.example.free2party.ui.theme.onBusyContainer
import com.example.free2party.ui.theme.onInactiveContainer
import com.example.free2party.ui.theme.WhatsAppColor
import com.example.free2party.util.openSMS
import com.example.free2party.util.openThirdPartyApp
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

    LaunchedEffect(Unit) {
        homeViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        event.message,
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
                        "Invite sent to ${event.email}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    setShowInviteFriendDialog(false)
                }
            }
        }
    }

    HomeScreen(
        uiState = homeViewModel.uiState,
        inviteFriendUiState = friendViewModel.uiState,
        showInviteFriendDialog = showInviteFriendDialog,
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
    uiState: HomeUiState,
    inviteFriendUiState: InviteFriendUiState,
    showInviteFriendDialog: Boolean,
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
    var showAboutDialog by remember { mutableStateOf(false) }
    var selectedFriend by remember { mutableStateOf<FriendInfo?>(null) }
    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Redirect initial focus to the root container to avoid highlighting the user menu
        rootFocusRequester.requestFocus()
    }

    Scaffold { paddingValues ->
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
                    val successState = uiState as? HomeUiState.Success
                    if (successState != null) {
                        Text(
                            text = successState.userName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        val profilePicUrl = (uiState as? HomeUiState.Success)?.profilePicUrl
                        if (!profilePicUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = "User Menu",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User Menu",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Profile") },
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
                                text = { Text("Settings") },
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
                                text = { Text("About") },
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
                                text = { Text("Logout") },
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

            when (uiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is HomeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is HomeUiState.Success -> {
                    HomeContent(
                        paddingValues = paddingValues,
                        isUserFree = uiState.isUserFree,
                        friendsList = uiState.friendsList,
                        isActionLoading = uiState.isActionLoading,
                        onToggleAvailability = onToggleAvailability,
                        onRemoveFriend = onRemoveFriend,
                        onCancelInvite = onCancelInvite,
                        onInviteFriendClick = onInviteFriendClick,
                        onFriendItemClick = { friend -> selectedFriend = friend }
                    )
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }

        if (showInviteFriendDialog) {
            InviteFriendDialog(
                uiState = inviteFriendUiState,
                onDismiss = onInviteFriendDismiss,
                onConfirm = onInviteFriendConfirm,
                onResetState = onInviteFriendResetState
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
    onToggleAvailability: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onInviteFriendClick: () -> Unit,
    onFriendItemClick: (FriendInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.free2party_full_transparent_light),
                contentDescription = "Free2Party Logo",
                modifier = Modifier.height(32.dp),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = if (isUserFree) "You are Free2Party" else "You are currently busy",
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isUserFree) MaterialTheme.colorScheme.availableContainer
                    else MaterialTheme.colorScheme.busyContainer
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color =
                if (isUserFree) MaterialTheme.colorScheme.onAvailableContainer
                else MaterialTheme.colorScheme.onBusyContainer
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .width(176.dp)
                .height(48.dp)
                .padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isUserFree) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                .clickable(enabled = !isActionLoading) { onToggleAvailability() },
            contentAlignment = Alignment.Center
        ) {
            if (isActionLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(
                    text = if (isUserFree) "Make Me Busy" else "Make Me Free",
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (isUserFree) MaterialTheme.colorScheme.onError
                        else MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 32.dp, bottom = 16.dp))

        FriendsListSection(
            friends = friendsList,
            onRemoveFriend = onRemoveFriend,
            onCancelInvite = onCancelInvite,
            onInviteFriendClick = onInviteFriendClick,
            onFriendItemClick = onFriendItemClick
        )
    }
}

@Composable
fun FriendsListSection(
    friends: List<FriendInfo>,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onInviteFriendClick: () -> Unit,
    onFriendItemClick: (FriendInfo) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "Your Friends",
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(
            onClick = onInviteFriendClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Invite Friend",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (friends.isEmpty()) {
        Text(
            text = "You don't have any friends yet!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
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
                    title = "Free2Party",
                    friends = freeFriends,
                    onRemoveFriend = onRemoveFriend,
                    onCancelInvite = onCancelInvite,
                    onClick = onFriendItemClick
                )
            }
            item {
                ExpandableFriendSection(
                    title = "Busy",
                    friends = busyFriends,
                    onRemoveFriend = onRemoveFriend,
                    onCancelInvite = onCancelInvite,
                    onClick = onFriendItemClick
                )
            }
            item {
                ExpandableFriendSection(
                    title = "Invited",
                    friends = invitedFriends,
                    onRemoveFriend = onRemoveFriend,
                    onCancelInvite = onCancelInvite,
                    onClick = onFriendItemClick
                )
            }
        }
    }
}

@Composable
fun ExpandableFriendSection(
    title: String,
    friends: List<FriendInfo>,
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (friends.isEmpty()) {
                    Text(
                        text = "No friends in this section",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                } else {
                    friends.forEach { friend ->
                        FriendItem(
                            friend = friend,
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
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onClick: () -> Unit
) {
    var showFriendMenu by remember { mutableStateOf(false) }
    var showContactMenu by remember { mutableStateOf(false) }
    val isInvited = friend.inviteStatus == InviteStatus.INVITED
    val context = LocalContext.current

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
                containerColor = when {
                    isInvited -> MaterialTheme.colorScheme.inactiveContainer
                    friend.isFreeNow -> MaterialTheme.colorScheme.availableContainer
                    else -> MaterialTheme.colorScheme.busyContainer
                }
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
                            contentDescription = "Pending",
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
                                contentDescription = "Contact Options",
                                tint = if (friend.isFreeNow) MaterialTheme.colorScheme.onAvailableContainer
                                else MaterialTheme.colorScheme.onBusyContainer
                            )
                        }

                        DropdownMenu(
                            expanded = showContactMenu,
                            onDismissRequest = { showContactMenu = false }
                        ) {
                            // SMS
                            val smsNumber = "${friend.phoneCode}${friend.phoneNumber}"
                            if (smsNumber.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("SMS") },
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

                            // Facebook
                            if (friend.socials.facebookUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Facebook") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Facebook,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color(0xFF1877F2)
                                        )
                                    },
                                    onClick = {
                                        showContactMenu = false
                                        openThirdPartyApp(
                                            context,
                                            "https://facebook.com/${friend.socials.facebookUsername}"
                                        )
                                    }
                                )
                            }

                            // Instagram
                            if (friend.socials.instagramUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Instagram") },
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
                                        openThirdPartyApp(
                                            context,
                                            "https://instagram.com/${friend.socials.instagramUsername}"
                                        )
                                    }
                                )
                            }

                            // Telegram
                            if (friend.socials.telegramUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Telegram") },
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
                                        openThirdPartyApp(
                                            context,
                                            "https://t.me/${friend.socials.telegramUsername}"
                                        )
                                    }
                                )
                            }

                            // TikTok
                            if (friend.socials.tiktokUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("TikTok") },
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
                                        openThirdPartyApp(
                                            context,
                                            "https://tiktok.com/@${friend.socials.tiktokUsername}"
                                        )
                                    }
                                )
                            }

                            // WhatsApp
                            val waNumber = friend.socials.whatsappFullNumber.ifBlank {
                                "${friend.phoneCode}${friend.phoneNumber}".replace("+", "")
                                    .filter { it.isDigit() }
                            }
                            if (waNumber.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("WhatsApp") },
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
                                        openThirdPartyApp(
                                            context,
                                            "https://wa.me/$waNumber?text=${Uri.encode("Hey! Saw you're Free2Party, want to hang out?")}"
                                        )
                                    }
                                )
                            }

                            // X
                            if (friend.socials.xUsername.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("X") },
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
                                        openThirdPartyApp(
                                            context,
                                            "https://x.com/${friend.socials.xUsername}"
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
                            contentDescription = "Friend Options",
                            tint = when {
                                isInvited -> MaterialTheme.colorScheme.onInactiveContainer
                                friend.isFreeNow -> MaterialTheme.colorScheme.onAvailableContainer
                                else -> MaterialTheme.colorScheme.onBusyContainer
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = showFriendMenu,
                        onDismissRequest = { showFriendMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isInvited) "Cancel Invite" else "Remove Friend",
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

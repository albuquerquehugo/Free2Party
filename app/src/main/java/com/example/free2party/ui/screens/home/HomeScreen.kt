package com.example.free2party.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.R
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.ui.theme.available
import com.example.free2party.ui.theme.availableContainer
import com.example.free2party.ui.theme.busy
import com.example.free2party.ui.theme.busyContainer
import com.example.free2party.ui.theme.inactiveContainer
import com.example.free2party.ui.theme.onAvailableContainer
import com.example.free2party.ui.theme.onBusyContainer
import com.example.free2party.ui.theme.onInactiveContainer
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    val (showLogoutDialog, setShowLogoutDialog) = remember { mutableStateOf(false) }
    val (showInviteFriendDialog, setShowInviteFriendDialog) = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // TODO: add an icon for user account
            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = { setShowLogoutDialog(true) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        when (viewModel.uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = (viewModel.uiState as HomeUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is HomeUiState.Success -> {
                HomeContent(
                    paddingValues = paddingValues,
                    isUserFree = (viewModel.uiState as HomeUiState.Success).isUserFree,
                    friendsList = (viewModel.uiState as HomeUiState.Success).friendsList,
                    isActionLoading = (viewModel.uiState as HomeUiState.Success).isActionLoading,
                    onToggleAvailability = { viewModel.toggleAvailability() },
                    onRemoveFriend = { uid -> viewModel.removeFriend(uid) },
                    onCancelInvite = { uid -> viewModel.cancelFriendInvite(uid) },
                    onInviteFriendClick = { setShowInviteFriendDialog(true) }
                )
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { setShowLogoutDialog(false) },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    TextButton(onClick = {
                        setShowLogoutDialog(false)
                        viewModel.logout(onLogout)
                    }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { setShowLogoutDialog(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showInviteFriendDialog) {
            InviteFriendDialog(onDismiss = { setShowInviteFriendDialog(false) })
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
    onInviteFriendClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
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
                painter = painterResource(id = R.drawable.logo_light_full),
                contentDescription = "Free2Party Logo",
                modifier = Modifier.height(120.dp),
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
                .padding(horizontal = 32.dp, vertical = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
            color =
                if (isUserFree) MaterialTheme.colorScheme.onAvailableContainer
                else MaterialTheme.colorScheme.onBusyContainer
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp)
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
                    style = MaterialTheme.typography.titleLarge,
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
            onInviteFriendClick = onInviteFriendClick
        )
    }
}

@Composable
fun InviteFriendDialog(
    viewModel: FriendViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is FriendUiEvent.InviteSentSuccessfully -> {
                    Toast.makeText(
                        context,
                        "Invite successfully sent to ${event.email}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.resetState()
            onDismiss()
        },
        title = { Text(text = "Invite Friend", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "Enter your friend's email to send an invite.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.uiState !is InviteFriendUiState.Searching,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.inviteFriend() }
                    )
                )

                if (viewModel.uiState is InviteFriendUiState.Error) {
                    Text(
                        text = (viewModel.uiState as InviteFriendUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.inviteFriend() },
                enabled = viewModel.uiState !is InviteFriendUiState.Searching
            ) {
                if (viewModel.uiState is InviteFriendUiState.Searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send invite")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.resetState()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FriendsListSection(
    friends: List<FriendInfo>,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onInviteFriendClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "Your Friends",
            style = MaterialTheme.typography.headlineSmall
        )

        IconButton(
            onClick = onInviteFriendClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Invite Friend"
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (friends.isEmpty()) {
        Text(
            text = "You don't have any friends yet!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(friends) { friend ->
                FriendItem(
                    friend = friend,
                    onRemoveFriend = { onRemoveFriend(friend.uid) },
                    onCancelInvite = { onCancelInvite(friend.uid) }
                )
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: FriendInfo,
    onRemoveFriend: (String) -> Unit,
    onCancelInvite: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isInvited = friend.inviteStatus == InviteStatus.INVITED

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
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
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInvited) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Pending",
                        modifier = Modifier.size(12.dp),
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

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        isInvited -> MaterialTheme.colorScheme.onInactiveContainer
                        friend.isFreeNow -> MaterialTheme.colorScheme.onAvailableContainer
                        else -> MaterialTheme.colorScheme.onBusyContainer
                    }
                )

                Spacer(modifier = Modifier.weight(1.0f))

                Text(
                    text = when {
                        isInvited -> "Invited"
                        friend.isFreeNow -> "Free2Party"
                        else -> "Busy"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        isInvited -> MaterialTheme.colorScheme.onInactiveContainer
                        friend.isFreeNow -> MaterialTheme.colorScheme.onAvailableContainer
                        else -> MaterialTheme.colorScheme.onBusyContainer
                    }
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
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
                    showMenu = false
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

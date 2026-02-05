package com.example.free2party.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.ui.theme.available
import com.example.free2party.ui.theme.availableContainer
import com.example.free2party.ui.theme.busy
import com.example.free2party.ui.theme.busyContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onLogout: () -> Unit,
    onAddFriendClick: () -> Unit
) {
    val (showLogoutDialog, setShowLogoutDialog) = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Free2Party") },
                actions = {
                    IconButton(onClick = { setShowLogoutDialog(true) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (viewModel.isUserFree) "You are Free2Party" else "You are currently busy",
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (viewModel.isUserFree) MaterialTheme.colorScheme.availableContainer
                        else MaterialTheme.colorScheme.busyContainer
                    )
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
                color =
                    if (viewModel.isUserFree) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (viewModel.isUserFree) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    .clickable { viewModel.toggleAvailability() },
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = if (viewModel.isUserFree) "Make Me Busy" else "Make Me Free",
                        style = MaterialTheme.typography.titleLarge,
                        color =
                            if (viewModel.isUserFree) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onError
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 32.dp, bottom = 16.dp))

            FriendsListSection(
                friends = viewModel.friendsStatusList,
                onDeleteFriend = { uid -> viewModel.removeFriend(uid) },
                onAddFriendClick = onAddFriendClick
            )
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
    }
}

@Composable
fun FriendsListSection(
    friends: List<FriendStatus>,
    onDeleteFriend: (String) -> Unit,
    onAddFriendClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "Your Friends",
            style = MaterialTheme.typography.headlineSmall
        )

        IconButton(
            onClick = onAddFriendClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Add Friend"
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
            items(
                items = friends,
                key = { it.uid }
            ) { friend ->
                FriendItem(friend = friend, onRemove = { uid -> onDeleteFriend(uid) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendItem(friend: FriendStatus, onRemove: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor =
                    if (friend.isFree) MaterialTheme.colorScheme.availableContainer
                    else MaterialTheme.colorScheme.busyContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (friend.isFree) MaterialTheme.colorScheme.available
                                else MaterialTheme.colorScheme.busy
                            )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.weight(1.0f))

                Text(
                    text = if (friend.isFree) "Free2Party" else "Busy",
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (friend.isFree) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error

                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Remove Friend", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showMenu = false
                    onRemove(friend.uid)
                }
            )
        }
    }
}

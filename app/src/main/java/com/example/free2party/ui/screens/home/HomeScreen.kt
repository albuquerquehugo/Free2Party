package com.example.free2party.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.ui.theme.available
import com.example.free2party.ui.theme.busy

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
                    IconButton(onClick = onAddFriendClick) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Friend"
                        )
                    }

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
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (viewModel.isUserFree) "You are Free2Party" else "You are currently busy",
                style = MaterialTheme.typography.headlineMedium,
                color =
                    if (viewModel.isUserFree) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Availability toggle button
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
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

            FriendsListSection(friends = viewModel.friendsStatusList)
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
fun FriendsListSection(friends: List<FriendStatus>) {
    Text(
        text = "Friends' Availability",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 16.dp)
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(friends) { friend ->
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (friend.isFree) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                )
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (friend.isFree) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.weight(1.0f))

                    Text(
                        text = if (friend.isFree) "Free2Party" else "Busy",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

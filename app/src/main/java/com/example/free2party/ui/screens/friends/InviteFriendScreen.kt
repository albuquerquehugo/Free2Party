package com.example.free2party.ui.screens.friends

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.UserRelationship
import com.example.free2party.ui.components.InputTextField
import com.example.free2party.ui.components.TopBar
import com.example.free2party.ui.components.dialogs.ConfirmationDialog
import kotlinx.coroutines.flow.collectLatest

@Composable
fun InviteFriendRoute(
    viewModel: FriendViewModel,
    onBack: () -> Unit,
    gradientBackground: Boolean = true
) {
    val context = LocalContext.current
    val inviteSentTemplate = stringResource(R.string.toast_invite_sent)
    val unblockToInviteTemplate = stringResource(R.string.error_unblock_to_invite)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is FriendUiEvent.InviteSentSuccessfully -> {
                    Toast.makeText(context, inviteSentTemplate, Toast.LENGTH_SHORT).show()
                    onBack()
                }

                is FriendUiEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        event.message.asString(context),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    InviteFriendScreen(
        uiState = viewModel.uiState,
        searchResults = viewModel.searchResults,
        isSearching = viewModel.isSearchingUsers,
        onQueryChange = { viewModel.searchUsers(it) },
        onUserSelected = { user ->
            if (user.relationship == UserRelationship.BLOCKED) {
                Toast.makeText(context, unblockToInviteTemplate, Toast.LENGTH_SHORT).show()
            } else {
                viewModel.inviteFriend(user.email)
            }
        },
        onBack = onBack,
        onResetState = { viewModel.resetState() },
        gradientBackground = gradientBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendScreen(
    uiState: InviteFriendUiState,
    searchResults: List<com.example.free2party.data.model.UserSearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onUserSelected: (com.example.free2party.data.model.UserSearchResult) -> Unit,
    onBack: () -> Unit,
    onResetState: () -> Unit,
    gradientBackground: Boolean
) {
    var query by remember { mutableStateOf("") }
    var userToInvite by remember {
        mutableStateOf<com.example.free2party.data.model.UserSearchResult?>(
            null
        )
    }

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_invite_friend),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            InputTextField(
                value = query,
                onValueChange = {
                    query = it
                    onQueryChange(it)
                    if (uiState is InviteFriendUiState.Error) onResetState()
                },
                label = stringResource(R.string.title_search),
                placeholder = stringResource(R.string.text_placeholder_search_user),
                icon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (query.isNotBlank() && searchResults.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_results_found),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(searchResults) { user ->
                        ListItem(
                            headlineContent = { Text(user.fullName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(user.email) },
                            leadingContent = {
                                if (user.profilePicUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.profilePicUrl,
                                        contentDescription = null,
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
                            },
                            trailingContent = {
                                when (user.relationship) {
                                    UserRelationship.FRIEND -> Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    UserRelationship.INVITED -> Icon(
                                        imageVector = Icons.Default.Mail,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    UserRelationship.BLOCKED -> Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )

                                    UserRelationship.NONE -> {}
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (gradientBackground) {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(
                                    enabled = ((user.relationship == UserRelationship.NONE) ||
                                            (user.relationship == UserRelationship.INVITED) ||
                                            (user.relationship == UserRelationship.BLOCKED))
                                ) {
                                    if (user.relationship == UserRelationship.NONE) {
                                        userToInvite = user
                                    } else {
                                        onUserSelected(user)
                                    }
                                }
                        )
                    }
                }
            }
        }

        if (userToInvite != null) {
            ConfirmationDialog(
                title = stringResource(R.string.title_invite_friend),
                text = stringResource(
                    R.string.text_send_invite_confirmation,
                    userToInvite!!.fullName,
                    userToInvite!!.email
                ),
                confirmButtonText = stringResource(R.string.button_confirm),
                onConfirm = {
                    onUserSelected(userToInvite!!)
                    userToInvite = null
                },
                dismissButtonText = stringResource(R.string.button_cancel),
                onDismiss = { userToInvite = null }
            )
        }
    }
}

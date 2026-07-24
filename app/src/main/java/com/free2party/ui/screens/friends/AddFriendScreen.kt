package com.free2party.ui.screens.friends

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.free2party.data.model.UserRelationship
import com.free2party.data.model.UserSearchResult
import com.free2party.R
import com.free2party.ui.components.basic.AppOutlinedTextField
import com.free2party.ui.components.dialogs.FriendActionType
import com.free2party.ui.components.dialogs.FriendConfirmationDialog
import com.free2party.ui.components.TopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AddFriendRoute(
    viewModel: FriendViewModel,
    onBack: () -> Unit,
    gradientBackground: Boolean = true
) {
    val context = LocalContext.current
    val friendRequestSentTemplate = stringResource(R.string.toast_friend_request_sent)
    val unblockToAddFriendTemplate = stringResource(R.string.error_unblock_to_add_friend)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is FriendUiEvent.FriendRequestSentSuccessfully -> {
                    Toast.makeText(context, friendRequestSentTemplate, Toast.LENGTH_SHORT).show()
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

    AddFriendScreen(
        uiState = viewModel.uiState,
        searchResults = viewModel.searchResults,
        isSearching = viewModel.isSearchingUsers,
        isSendingRequest = viewModel.isSendingRequest,
        processingRequestIds = viewModel.processingRequestIds,
        onQueryChange = { viewModel.searchUsers(it) },
        onUserSelected = { user ->
            if (user.relationship == UserRelationship.BLOCKED) {
                Toast.makeText(context, unblockToAddFriendTemplate, Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addFriend(user.email)
            }
        },
        onAcceptFriendRequest = { reqId, email -> viewModel.acceptFriendRequest(reqId, email) },
        onDeclineFriendRequest = { reqId, email -> viewModel.declineFriendRequest(reqId, email) },
        onBack = onBack,
        onResetState = { viewModel.resetState() },
        gradientBackground = gradientBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    uiState: AddFriendUiState,
    searchResults: List<UserSearchResult>,
    isSearching: Boolean,
    isSendingRequest: Boolean = false,
    processingRequestIds: Set<String> = emptySet(),
    onQueryChange: (String) -> Unit,
    onUserSelected: (UserSearchResult) -> Unit,
    onAcceptFriendRequest: (String, String) -> Unit = { _, _ -> },
    onDeclineFriendRequest: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onResetState: () -> Unit,
    gradientBackground: Boolean
) {
    var query by remember { mutableStateOf("") }
    var userToAdd by remember {
        mutableStateOf<UserSearchResult?>(
            null
        )
    }

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.label_add_friend),
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

            AppOutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onQueryChange(it)
                    if (uiState is AddFriendUiState.Error) onResetState()
                },
                placeholderText = stringResource(R.string.text_placeholder_search_user),
                icon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (query.isNotBlank() && searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.text_no_results_found),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
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

                                    UserRelationship.PENDING -> Icon(
                                        imageVector = Icons.Default.Mail,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    UserRelationship.PENDING_INCOMING -> {
                                        val isProcessing = user.requestId != null && processingRequestIds.contains(user.requestId)
                                        if (isProcessing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.tertiary)
                                                        .clickable {
                                                            user.requestId?.let { reqId ->
                                                                onDeclineFriendRequest(
                                                                    reqId,
                                                                    user.email
                                                                )
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.description_decline),
                                                        tint = MaterialTheme.colorScheme.onTertiary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .clickable {
                                                            user.requestId?.let { reqId ->
                                                                onAcceptFriendRequest(reqId, user.email)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = stringResource(R.string.description_accept),
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

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
                                            (user.relationship == UserRelationship.PENDING) ||
                                            (user.relationship == UserRelationship.BLOCKED))
                                ) {
                                    if (user.relationship == UserRelationship.NONE) {
                                        userToAdd = user
                                    } else {
                                        onUserSelected(user)
                                    }
                                }
                        )
                    }
                }
            }
        }

        if (userToAdd != null) {
            FriendConfirmationDialog(
                name = userToAdd!!.fullName,
                email = userToAdd!!.email,
                profilePicUrl = userToAdd!!.profilePicUrl,
                actionType = FriendActionType.ADD,
                onConfirm = {
                    onUserSelected(userToAdd!!)
                    userToAdd = null
                },
                onDismissRequest = { userToAdd = null }
            )
        }

        if (isSendingRequest) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

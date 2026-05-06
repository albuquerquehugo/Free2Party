package com.example.free2party.ui.screens.circles

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.Circle
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.ui.components.TopBar
import com.example.free2party.ui.components.dialogs.CircleDialog
import com.example.free2party.ui.components.dialogs.ConfirmationDialog
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CirclesRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: CircleViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is CircleUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT)
                        .show()
                }

                is CircleUiEvent.CircleActionSuccess -> {
                    // Handled internally or if we needed to navigate away
                }
            }
        }
    }

    val friends by viewModel.friendsList.collectAsState()

    CirclesScreen(
        circles = viewModel.circles,
        friends = friends,
        gradientBackground = viewModel.gradientBackground,
        isActionLoading = viewModel.isActionLoading,
        onBack = onBack,
        onCreateCircle = { name, selectedFriends -> viewModel.createCircle(name, selectedFriends) },
        onUpdateCircle = { id, name, friends -> viewModel.updateCircle(id, name, friends) },
        onDeleteCircle = { id -> viewModel.deleteCircle(id) }
    )
}

@Composable
fun CirclesScreen(
    circles: List<Circle>,
    friends: List<FriendInfo>,
    gradientBackground: Boolean,
    isActionLoading: Boolean,
    onBack: () -> Unit,
    onCreateCircle: (String, List<String>) -> Unit,
    onUpdateCircle: (String, String, List<String>) -> Unit,
    onDeleteCircle: (String) -> Unit
) {
    val (showCreateDialog, setShowCreateDialog) = remember { mutableStateOf(false) }
    var circleToEdit by remember { mutableStateOf<Circle?>(null) }
    var circleToDelete by remember { mutableStateOf<Circle?>(null) }

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_circles),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { setShowCreateDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.title_create_circle)
                )
            }
        }
    ) { paddingValues ->
        CirclesContent(
            paddingValues = paddingValues,
            circles = circles,
            friends = friends,
            onEditCircle = { circleToEdit = it },
            onDeleteCircle = { circleToDelete = it }
        )

        if (showCreateDialog) {
            CircleDialog(
                friends = friends,
                onDismiss = { setShowCreateDialog(false) },
                onConfirm = { name, selectedFriends ->
                    onCreateCircle(name, selectedFriends)
                    setShowCreateDialog(false)
                },
                isLoading = isActionLoading
            )
        }

        circleToEdit?.let { circle ->
            CircleDialog(
                circle = circle,
                friends = friends,
                onDismiss = { circleToEdit = null },
                onConfirm = { name, selectedFriends ->
                    onUpdateCircle(circle.id, name, selectedFriends)
                    circleToEdit = null
                },
                isLoading = isActionLoading
            )
        }

        circleToDelete?.let { circle ->
            ConfirmationDialog(
                title = stringResource(R.string.title_delete_circle),
                text = stringResource(R.string.text_delete_circle_confirmation),
                confirmButtonText = stringResource(R.string.text_delete),
                onConfirm = {
                    onDeleteCircle(circle.id)
                    circleToDelete = null
                },
                dismissButtonText = stringResource(R.string.button_cancel),
                onDismiss = { circleToDelete = null },
                isDestructive = true
            )
        }
    }
}

@Composable
private fun CirclesContent(
    paddingValues: PaddingValues,
    circles: List<Circle>,
    friends: List<FriendInfo>,
    onEditCircle: (Circle) -> Unit,
    onDeleteCircle: (Circle) -> Unit
) {
    if (circles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.text_circles_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(circles, key = { it.id }) { circle ->
                CircleListItem(
                    circle = circle,
                    friends = friends,
                    onEdit = { onEditCircle(circle) },
                    onDelete = { onDeleteCircle(circle) }
                )
            }
        }
    }
}

@Composable
private fun CircleListItem(
    circle: Circle,
    friends: List<FriendInfo>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    val circleFriends = remember(circle.friendIds, friends) {
        friends.filter { it.uid in circle.friendIds }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .rotate(rotationState)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = circle.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.label_circle_member_count,
                                circle.friendIds.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.description_more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.text_edit)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                onEdit()
                                showMenu = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.text_delete),
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
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(start = 52.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (circleFriends.isEmpty()) {
                        Text(
                            text = stringResource(R.string.text_no_friends_in_circle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        circleFriends.forEach { friend ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (friend.profilePicUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = friend.profilePicUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = friend.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

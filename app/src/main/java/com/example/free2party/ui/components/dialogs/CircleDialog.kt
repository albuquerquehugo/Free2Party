package com.example.free2party.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.Circle
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.ui.components.InputTextField
import com.example.free2party.util.capitalizeFirstLetter

@Composable
fun CircleDialog(
    circle: Circle? = null,
    friends: List<FriendInfo>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, selectedFriendIds: List<String>) -> Unit,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf(circle?.name ?: "") }
    var selectedFriendIds by remember { mutableStateOf(circle?.friendIds?.toSet() ?: emptySet()) }
    val focusManager = LocalFocusManager.current

    val hasChanges by remember(name, selectedFriendIds, circle) {
        derivedStateOf {
            val initialName = circle?.name ?: ""
            val initialFriendIds = circle?.friendIds?.toSet() ?: emptySet()
            name.trim() != initialName || selectedFriendIds != initialFriendIds
        }
    }

    BaseDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text =
                    if (circle == null) stringResource(R.string.title_create_circle)
                    else stringResource(R.string.title_edit_circle),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            InputTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.label_circle_name),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.label_select_friends),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (friends.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_friends_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(friends) { friend ->
                        val isSelected = selectedFriendIds.contains(friend.uid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isLoading) {
                                    selectedFriendIds = if (isSelected) {
                                        selectedFriendIds - friend.uid
                                    } else {
                                        selectedFriendIds + friend.uid
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedFriendIds = if (checked) {
                                        selectedFriendIds + friend.uid
                                    } else {
                                        selectedFriendIds - friend.uid
                                    }
                                },
                                enabled = !isLoading
                            )

                            if (friend.profilePicUrl.isNotBlank()) {
                                AsyncImage(
                                    model = friend.profilePicUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = friend.email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text(stringResource(R.string.button_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onConfirm(
                            name.trim().capitalizeFirstLetter(),
                            selectedFriendIds.toList()
                        )
                    },
                    enabled = name.isNotBlank() && !isLoading && (circle == null || hasChanges)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.button_save))
                    }
                }
            }
        }
    }
}

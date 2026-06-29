package com.free2party.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.data.model.Circle
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.InviteStatus
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.components.basic.AppOutlinedCard

@Composable
fun FriendSelector(
    friends: List<FriendInfo>,
    circles: List<Circle>,
    selectedFriendIds: List<String>,
    onToggleFriend: (String) -> Unit,
    onAddFriends: (List<String>) -> Unit,
    onRemoveFriends: (List<String>) -> Unit,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit
) {
    Column {
        AppOutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .heightIn(min = 200.dp, max = 400.dp)
        ) {
            if (friends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.label_no_friends_to_select),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(4.dp)) {
                    if (circles.isNotEmpty()) {
                        items(circles) { circle ->
                            val validCircleFriendIds = remember(circle, friends) {
                                val currentIds = friends.map { it.uid }.toSet()
                                circle.friendIds.filter { it in currentIds }
                            }
                            val isEnabled = validCircleFriendIds.isNotEmpty()
                            val isCircleSelected = isEnabled &&
                                    validCircleFriendIds.all { it in selectedFriendIds }

                            CircleSelectorItem(
                                circleName = circle.name,
                                memberCount = validCircleFriendIds.size,
                                isSelected = isCircleSelected,
                                enabled = isEnabled,
                                onToggle = {
                                    if (isCircleSelected) {
                                        onRemoveFriends(validCircleFriendIds)
                                    } else {
                                        onAddFriends(validCircleFriendIds)
                                    }
                                }
                            )
                        }
                        item {
                            AppHorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    items(friends) { friend ->
                        FriendSelectorItem(
                            friend = friend,
                            isSelected = friend.uid in selectedFriendIds,
                            onToggle = { onToggleFriend(friend.uid) }
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(R.string.label_unselect_all),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .testTag("unselect_all")
                    .clickable { onUnselectAll() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.label_select_all),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .testTag("select_all")
                    .clickable { onSelectAll() }
            )
        }
    }
}

@Composable
fun FriendSelectorItem(friend: FriendInfo, isSelected: Boolean, onToggle: () -> Unit) {
    val isPending = friend.inviteStatus == InviteStatus.PENDING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                if (isPending) {
                    Text(
                        text = " " + stringResource(R.string.label_pending_observation),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            Text(
                text = friend.email,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CircleSelectorItem(
    circleName: String,
    memberCount: Int,
    isSelected: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onToggle() } else Modifier)
            .padding(8.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            enabled = enabled
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = circleName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = pluralStringResource(
                    R.plurals.label_circle_member_count,
                    memberCount,
                    memberCount
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

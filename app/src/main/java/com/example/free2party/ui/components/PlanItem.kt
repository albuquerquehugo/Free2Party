package com.example.free2party.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.data.model.PlanVisibility
import com.example.free2party.util.calculateDuration
import com.example.free2party.util.formatPlanDateInFull
import com.example.free2party.util.formatTimeForDisplay
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMillis
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Calendar
import java.util.TimeZone

data class PlanStatus(
    val isCurrent: Boolean,
    val isPast: Boolean,
    val isEditDisabled: Boolean
)

@Composable
fun PlanItem(
    modifier: Modifier = Modifier,
    plan: FuturePlan,
    use24HourFormat: Boolean,
    currentTimeMillis: Long,
    friends: List<FriendInfo>,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isExpandedExternally: Boolean = false,
    onExpandChange: (Boolean) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    val currentUserId = remember { Firebase.auth.currentUser?.uid }
    val isOwnPlan = plan.userId == currentUserId
    val isReadOnly = onEdit == null && onDelete == null || !isOwnPlan

    val planStatus by remember(plan, currentTimeMillis) {
        derivedStateOf {
            val startDateMillis = parseDateToMillis(plan.startDate) ?: 0L
            val endDateMillis = parseDateToMillis(plan.endDate) ?: 0L
            val startTimeMillis = parseTimeToMillis(plan.startTime) ?: 0L
            val endTimeMillis = parseTimeToMillis(plan.endTime) ?: 0L

            val startDateTimeMillis = startDateMillis + startTimeMillis
            val endDateTimeMillis = endDateMillis + endTimeMillis

            val localNow = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
            val nowUtcMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(
                    localNow.get(Calendar.YEAR),
                    localNow.get(Calendar.MONTH),
                    localNow.get(Calendar.DAY_OF_MONTH),
                    localNow.get(Calendar.HOUR_OF_DAY),
                    localNow.get(Calendar.MINUTE),
                    0
                )
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            PlanStatus(
                isCurrent = nowUtcMillis in startDateTimeMillis..<endDateTimeMillis,
                isPast = nowUtcMillis >= endDateTimeMillis,
                isEditDisabled = nowUtcMillis >= startDateTimeMillis
            )
        }
    }

    val duration = remember(plan.startDate, plan.endDate, plan.startTime, plan.endTime) {
        calculateDuration(plan.startDate, plan.endDate, plan.startTime, plan.endTime)
    }

    val friendsSelection = remember(plan.visibility, plan.friendsSelection, friends) {
        when (plan.visibility) {
            PlanVisibility.EVERYONE -> "Everyone"
            PlanVisibility.EXCEPT -> {
                val names = plan.friendsSelection.mapNotNull { uid ->
                    friends.find { it.uid == uid }?.name
                }
                if (names.isEmpty()) "Everyone" else "Everyone except: ${names.joinToString(", ")}"
            }

            PlanVisibility.ONLY -> {
                val names = plan.friendsSelection.mapNotNull { uid ->
                    friends.find { it.uid == uid }?.name
                }
                if (names.isEmpty()) "Only you" else "Only: ${names.joinToString(", ")}"
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable {
                if (hasOverflow || isExpandedExternally) {
                    onExpandChange(!isExpandedExternally)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                planStatus.isCurrent -> MaterialTheme.colorScheme.tertiaryContainer
                planStatus.isPast -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = when {
                planStatus.isCurrent -> MaterialTheme.colorScheme.onTertiaryContainer
                planStatus.isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(start = 24.dp, top = 16.dp, bottom = 16.dp)
            ) {
                // Time and Duration Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (plan.startDate == plan.endDate) {
                            Text(
                                text = formatTimeForDisplay(
                                    plan.startTime,
                                    use24HourFormat
                                ) + " - " +
                                        formatTimeForDisplay(plan.endTime, use24HourFormat),
                                style = MaterialTheme.typography.titleSmall
                            )
                        } else {
                            DateTimeLabel(
                                time = formatTimeForDisplay(plan.startTime, use24HourFormat),
                                date = plan.startDate
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                DateTimeLabel(
                                    time = formatTimeForDisplay(plan.endTime, use24HourFormat),
                                    date = plan.endDate
                                )
                            }
                        }

                        DurationBadge(text = duration, status = planStatus)
                    }
                }

                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.height(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = friendsSelection,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 8.dp),
                        maxLines = if (isExpandedExternally) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (plan.note.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = plan.note,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (isExpandedExternally) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                hasOverflow =
                                    textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 1
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Action Menu Section (Only for own plans)
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 48.dp)
                ) {
                    if (isOwnPlan && !isReadOnly) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Plan Actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        PlanActionsMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            onEdit = onEdit,
                            onDelete = onDelete,
                            editEnabled = !planStatus.isEditDisabled
                        )
                    }
                }

                if (hasOverflow || isExpandedExternally) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 24.dp)
                            .padding(end = 12.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector =
                                if (isExpandedExternally) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateTimeLabel(time: String, date: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = time,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = " (${formatPlanDateInFull(date)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 2.dp),
            fontSize = 10.sp
        )
    }
}

@Composable
fun DurationBadge(text: String, status: PlanStatus) {
    val color = when {
        status.isCurrent -> MaterialTheme.colorScheme.tertiary
        status.isPast -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.wrapContentHeight()
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun PlanActionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    editEnabled: Boolean = true
) {
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        onEdit?.let {
            DropdownMenuItem(
                text = { Text("Edit") },
                enabled = editEnabled,
                onClick = {
                    onDismissRequest()
                    it()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
        }
        onDelete?.let {
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDismissRequest()
                    it()
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

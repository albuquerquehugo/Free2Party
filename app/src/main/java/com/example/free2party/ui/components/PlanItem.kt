package com.example.free2party.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.example.free2party.util.formatPlanDateInFull
import com.example.free2party.util.formatTimeForDisplay
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMillis
import com.example.free2party.util.parseTimeToMinutes
import java.util.Calendar
import java.util.TimeZone

@Composable
fun PlanItem(
    modifier: Modifier = Modifier,
    plan: FuturePlan,
    use24HourFormat: Boolean,
    currentTimeMillis: Long,
    friends: List<FriendInfo>,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    val isReadOnly = onEdit == null && onDelete == null

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

            object {
                val isCurrent = nowUtcMillis in startDateTimeMillis..<endDateTimeMillis
                val isPast = nowUtcMillis >= endDateTimeMillis
                val isEditDisabled = nowUtcMillis >= startDateTimeMillis
            }
        }
    }

    val duration = remember(plan.startDate, plan.endDate, plan.startTime, plan.endTime) {
        val startDateMillis = parseDateToMillis(plan.startDate) ?: 0L
        val endDateMillis = parseDateToMillis(plan.endDate) ?: 0L
        val startTimeMinutes = parseTimeToMinutes(plan.startTime) ?: 0
        val endTimeMinutes = parseTimeToMinutes(plan.endTime) ?: 0

        val durationMins = ((endDateMillis - startDateMillis) / 60000L) +
                endTimeMinutes - startTimeMinutes
        val hours = durationMins / 60
        val minutes = durationMins % 60
        when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
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

    Box {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .combinedClickable(
                    onClick = {
                        if (hasOverflow || isExpanded) {
                            isExpanded = !isExpanded
                        }
                    },
                    onLongClick = { if (!isReadOnly) showMenu = true }
                ),
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
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (plan.startDate == plan.endDate) {
                        Text(
                            text = formatTimeForDisplay(plan.startTime, use24HourFormat) + " - " +
                                    formatTimeForDisplay(plan.endTime, use24HourFormat),
                            style = MaterialTheme.typography.titleSmall
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DateTimeLabel(
                                time = formatTimeForDisplay(plan.startTime, use24HourFormat),
                                date = plan.startDate
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            DateTimeLabel(
                                time = formatTimeForDisplay(plan.endTime, use24HourFormat),
                                date = plan.endDate
                            )
                        }
                    }

                    DurationBadge(text = duration, isCurrent = planStatus.isCurrent)
                }

                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        modifier = Modifier.padding(start = 4.dp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (plan.note.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = plan.note,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                hasOverflow =
                                    textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 1
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (hasOverflow || isExpanded) {
                            Icon(
                                imageVector =
                                    if (isExpanded) Icons.Default.KeyboardArrowUp
                                    else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isReadOnly) {
            PlanActionsMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                onEdit = onEdit,
                onDelete = onDelete,
                editEnabled = !planStatus.isEditDisabled
            )
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
fun DurationBadge(text: String, isCurrent: Boolean) {
    val color =
        (if (isCurrent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color =
                if (isCurrent) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary
        )
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
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
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

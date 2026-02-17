package com.example.free2party.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.util.isDateTimeCurrent
import com.example.free2party.util.isDateTimeInPast
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMinutes
import java.util.Calendar

@Composable
fun PlanItem(
    modifier: Modifier = Modifier,
    plan: FuturePlan,
    currentTimeMillis: Long,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val isReadOnly = onEdit == null && onDelete == null

    val planStatus by remember(plan, currentTimeMillis) {
        derivedStateOf {
            val millis = parseDateToMillis(plan.date) ?: 0L
            val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }

            object {
                val isCurrent = isDateTimeCurrent(millis, plan.startTime, plan.endTime, calendar)
                val isPast = isDateTimeInPast(millis, plan.endTime, calendar)
                val isEditDisabled = isDateTimeInPast(millis, plan.startTime, calendar)
            }
        }
    }

    val duration = remember(plan.startTime, plan.endTime) {
        val durationMins = parseTimeToMinutes(plan.endTime) - parseTimeToMinutes(plan.startTime)
        val hours = durationMins / 60
        val minutes = durationMins % 60
        when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    Box {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { },
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
                    Text(
                        text = "${plan.startTime} - ${plan.endTime}",
                        style = MaterialTheme.typography.titleSmall
                    )

                    DurationBadge(text = duration, isCurrent = planStatus.isCurrent)
                }

                if (plan.note.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = plan.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
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
fun DurationBadge(text: String, isCurrent: Boolean) {
    val color =
        (if (isCurrent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isCurrent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
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

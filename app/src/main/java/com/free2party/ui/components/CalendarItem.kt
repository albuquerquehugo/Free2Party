package com.free2party.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.FuturePlan
import com.free2party.data.model.Event
import com.free2party.data.model.GuestStatus
import com.free2party.data.model.PlanVisibility
import com.free2party.R
import com.free2party.ui.theme.eventContainer
import com.free2party.ui.theme.onEventContainer
import com.free2party.util.calculateDuration
import com.free2party.util.formatPlanDateInFull
import com.free2party.util.formatTimeForDisplay
import com.free2party.util.parseDateToMillis
import com.free2party.util.parseTimeToMillis
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
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
    gradientBackground: Boolean = false,
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
    }.asString()

    val friendsSelection = when (plan.visibility) {
        PlanVisibility.EVERYONE -> stringResource(R.string.label_everyone)
        PlanVisibility.EXCEPT -> {
            val names = plan.friendsSelection.mapNotNull { uid ->
                friends.find { it.uid == uid }?.name
            }
            if (names.isEmpty()) stringResource(R.string.label_everyone)
            else stringResource(R.string.label_everyone_except, names.joinToString(", "))
        }

        PlanVisibility.ONLY -> {
            val names = plan.friendsSelection.mapNotNull { uid ->
                friends.find { it.uid == uid }?.name
            }
            if (names.isEmpty()) stringResource(R.string.label_only_you)
            else stringResource(R.string.label_only_selected_people, names.joinToString(", "))
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
                planStatus.isCurrent -> MaterialTheme.colorScheme.secondaryContainer.let {
                    if (gradientBackground) it.copy(
                        alpha = 0.7f
                    ) else it
                }

                planStatus.isPast -> MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = if (gradientBackground) 0.4f * 0.7f else 0.4f
                )

                else -> MaterialTheme.colorScheme.primaryContainer.let {
                    if (gradientBackground) it.copy(
                        alpha = 0.7f
                    ) else it
                }
            },
            contentColor = when {
                planStatus.isCurrent -> MaterialTheme.colorScheme.onSecondaryContainer
                planStatus.isPast -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, bottom = 16.dp)
            ) {
                // Time and Duration Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (plan.startDate == plan.endDate) {
                            Text(
                                text = stringResource(
                                    R.string.time_range,
                                    formatTimeForDisplay(plan.startTime, use24HourFormat),
                                    formatTimeForDisplay(plan.endTime, use24HourFormat)
                                ),
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

                if (isOwnPlan) {
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 48.dp)
                            .height(24.dp),
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
                }

                if (plan.note.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = plan.note,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (isExpandedExternally) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                hasOverflow =
                                    textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 1
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (hasOverflow || isExpandedExternally) {
                                Icon(
                                    imageVector =
                                        if (isExpandedExternally) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(y = (-4).dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action Menu Section (Only for own plans)
            if (isOwnPlan && !isReadOnly) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp)
                        .size(width = 48.dp, height = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.description_plan_actions_content),
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
        }
    }
}

@Composable
fun EventItem(
    modifier: Modifier = Modifier,
    event: Event,
    use24HourFormat: Boolean,
    currentTimeMillis: Long,
    gradientBackground: Boolean = false,
    onClick: () -> Unit,
    isExpandedExternally: Boolean = false,
    onExpandChange: (Boolean) -> Unit = {}
) {
    var hasOverflow by remember { mutableStateOf(false) }

    val currentUserId = remember { Firebase.auth.currentUser?.uid ?: "" }
    val isHost = event.hostId == currentUserId

    val duration = remember(event.startDate, event.endDate, event.startTime, event.endTime) {
        calculateDuration(event.startDate, event.endDate, event.startTime, event.endTime)
    }.asString()

    val eventStatus by remember(event, currentTimeMillis) {
        derivedStateOf {
            val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone(event.timezone)
            }
            val startDateTime =
                runCatching { timeFormatter.parse("${event.startDate} ${event.startTime}") }.getOrNull()?.time
                    ?: 0L
            val endDateTime =
                runCatching { timeFormatter.parse("${event.endDate} ${event.endTime}") }.getOrNull()?.time
                    ?: 0L

            val isCurrent = currentTimeMillis in startDateTime..<endDateTime
            val isPast = currentTimeMillis >= endDateTime

            PlanStatus(isCurrent = isCurrent, isPast = isPast, isEditDisabled = false)
        }
    }

    val containerColor = when {
        eventStatus.isCurrent -> MaterialTheme.colorScheme.secondaryContainer.let {
            if (gradientBackground) it.copy(
                alpha = 0.7f
            ) else it
        }

        eventStatus.isPast -> MaterialTheme.colorScheme.eventContainer.copy(
            alpha = if (gradientBackground) 0.4f * 0.7f else 0.4f
        )

        else -> MaterialTheme.colorScheme.eventContainer.let {
            if (gradientBackground) it.copy(alpha = 0.7f) else it
        }
    }

    val baseContentColor = when {
        eventStatus.isCurrent -> MaterialTheme.colorScheme.onSecondaryContainer
        eventStatus.isPast -> MaterialTheme.colorScheme.onEventContainer
        else -> MaterialTheme.colorScheme.onEventContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = baseContentColor
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 24.dp)
            ) {
                // Event Badge and Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = baseContentColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Small "Event" badge to clearly differentiate from plans
                    Surface(
                        color = baseContentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.badge_event),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = baseContentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                        if (event.startDate == event.endDate) {
                            Text(
                                text = stringResource(
                                    R.string.time_range,
                                    formatTimeForDisplay(event.startTime, use24HourFormat),
                                    formatTimeForDisplay(event.endTime, use24HourFormat)
                                ),
                                style = MaterialTheme.typography.titleSmall
                            )
                        } else {
                            DateTimeLabel(
                                time = formatTimeForDisplay(event.startTime, use24HourFormat),
                                date = event.startDate
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = baseContentColor.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                DateTimeLabel(
                                    time = formatTimeForDisplay(event.endTime, use24HourFormat),
                                    date = event.endDate
                                )
                            }
                        }

                        // Event duration badge
                        Surface(
                            color = baseContentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.wrapContentHeight()
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = duration,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = baseContentColor
                                )
                            }
                        }
                    }
                }

                // Hosted by / Location Section
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hostString = if (isHost) {
                        stringResource(R.string.label_hosted_by, R.string.label_you)
                    } else {
                        stringResource(R.string.label_hosted_by, event.hostName)
                    }
                    Text(
                        text = "$hostString • ${event.locationName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = baseContentColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Description (if any)
                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = event.description,
                            color = baseContentColor.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (isExpandedExternally) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                hasOverflow =
                                    textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 1
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (hasOverflow || isExpandedExternally) {
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(16.dp)
                                    .clickable { onExpandChange(!isExpandedExternally) },
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Icon(
                                    imageVector =
                                        if (isExpandedExternally) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = baseContentColor.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(y = (-4).dp)
                                )
                            }
                        }
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
            text = " " + stringResource(R.string.date_in_parentheses, formatPlanDateInFull(date)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 2.dp),
            fontSize = 10.sp
        )
    }
}

@Composable
fun DurationBadge(text: String, status: PlanStatus) {
    val baseColor = when {
        status.isCurrent -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = baseColor.copy(alpha = 0.15f),
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
                color = baseColor
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val editDisabledMsg = stringResource(R.string.error_plan_edit_current_past)
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        onEdit?.let {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.label_edit),
                        color = if (editEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                },
                onClick = {
                    onDismissRequest()
                    if (editEnabled) {
                        it()
                    } else {
                        Toast.makeText(
                            context,
                            editDisabledMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (editEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            )
        }
        onDelete?.let {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.label_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
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

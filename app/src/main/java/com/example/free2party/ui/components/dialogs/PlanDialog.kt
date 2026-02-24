package com.example.free2party.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.data.model.PlanVisibility
import com.example.free2party.util.formatTime
import com.example.free2party.util.formatTimeForDisplay
import com.example.free2party.util.isDateTimeInPast
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMinutes
import com.example.free2party.util.unformatTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDialog(
    editingPlan: FuturePlan?,
    use24HourFormat: Boolean,
    friends: List<FriendInfo>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, PlanVisibility, List<String>) -> Unit,
    startDatePickerState: DatePickerState,
    endDatePickerState: DatePickerState,
    startTimeState: TimePickerState,
    endTimeState: TimePickerState
) {
    var note by remember { mutableStateOf(editingPlan?.note ?: "") }
    var visibility by remember {
        mutableStateOf(
            editingPlan?.visibility ?: PlanVisibility.EVERYONE
        )
    }

    var exceptFriendIds by remember {
        mutableStateOf(
            if (editingPlan?.visibility == PlanVisibility.EXCEPT) editingPlan.friendsSelection else emptyList()
        )
    }
    var onlyFriendIds by remember {
        mutableStateOf(
            if (editingPlan?.visibility == PlanVisibility.ONLY) editingPlan.friendsSelection else emptyList()
        )
    }

    val originalVisibility = remember { editingPlan?.visibility ?: PlanVisibility.EVERYONE }
    val originalFriendIds = remember { editingPlan?.friendsSelection ?: emptyList() }

    val (showStartDatePicker, setShowStartDatePicker) = remember { mutableStateOf(false) }
    val (showEndDatePicker, setShowEndDatePicker) = remember { mutableStateOf(false) }
    val (showStartTimePicker, setShowStartTimePicker) = remember { mutableStateOf(false) }
    val (showEndTimePicker, setShowEndTimePicker) = remember { mutableStateOf(false) }

    val format = remember {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val startDateText =
        startDatePickerState.selectedDateMillis?.let { format.format(Date(it)) }
            ?: "Select start date"
    val endDateText =
        endDatePickerState.selectedDateMillis?.let { format.format(Date(it)) } ?: "Select end date"

    val isDateTimeValid = remember(
        startDatePickerState.selectedDateMillis,
        endDatePickerState.selectedDateMillis,
        startTimeState.hour,
        startTimeState.minute,
        endTimeState.hour,
        endTimeState.minute
    ) {
        val startMillis = startDatePickerState.selectedDateMillis ?: 0L
        val endMillis = endDatePickerState.selectedDateMillis ?: 0L

        if (startMillis < endMillis) return@remember true
        if (startMillis > endMillis) return@remember false

        val startMins =
            parseTimeToMinutes(formatTime(startTimeState.hour, startTimeState.minute)) ?: 0
        val endMins = parseTimeToMinutes(formatTime(endTimeState.hour, endTimeState.minute)) ?: 0
        startMins < endMins
    }

    val isStartDateInPast = startDatePickerState.selectedDateMillis?.let {
        isDateTimeInPast(it, null)
    } ?: false
    val isStartTimeInPast = startDatePickerState.selectedDateMillis?.let {
        isDateTimeInPast(it, formatTime(startTimeState.hour, startTimeState.minute))
    } ?: false

    // Initialize state when editing an existing plan
    LaunchedEffect(editingPlan) {
        editingPlan?.let {
            val start = unformatTime(it.startTime)
            val end = unformatTime(it.endTime)
            startTimeState.hour = start.first
            startTimeState.minute = start.second
            endTimeState.hour = end.first
            endTimeState.minute = end.second

            startDatePickerState.selectedDateMillis = parseDateToMillis(it.startDate)
            endDatePickerState.selectedDateMillis = parseDateToMillis(it.endDate)

            visibility = it.visibility
            if (it.visibility == PlanVisibility.EXCEPT) {
                exceptFriendIds = it.friendsSelection
            } else if (it.visibility == PlanVisibility.ONLY) {
                onlyFriendIds = it.friendsSelection
            }
        }
    }

    // Update default times for new plans
    LaunchedEffect(startDatePickerState.selectedDateMillis) {
        if (editingPlan == null) {
            val now = Calendar.getInstance()
            val selectedMillis = startDatePickerState.selectedDateMillis

            val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH),
                    0,
                    0,
                    0
                )
                set(Calendar.MILLISECOND, 0)
            }
            val isToday = selectedMillis == todayUtc.timeInMillis

            if (isToday) {
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val minute = now.get(Calendar.MINUTE)

                when {
                    hour < 12 -> {
                        startTimeState.hour = 12
                        startTimeState.minute = 0
                        endTimeState.hour = 13
                        endTimeState.minute = 0
                        endDatePickerState.selectedDateMillis = selectedMillis
                    }

                    hour >= 23 -> {
                        startTimeState.hour = hour
                        startTimeState.minute = if (minute < 59) minute + 1 else 59
                        endTimeState.hour = 23
                        endTimeState.minute = 59
                        endDatePickerState.selectedDateMillis = selectedMillis
                    }

                    else -> {
                        startTimeState.hour = hour + 1
                        startTimeState.minute = 0
                        if (startTimeState.hour < 23) {
                            endTimeState.hour = startTimeState.hour + 1
                            endTimeState.minute = 0
                            endDatePickerState.selectedDateMillis = selectedMillis
                        } else {
                            endTimeState.hour = 0
                            endTimeState.minute = 0
                            endDatePickerState.selectedDateMillis = selectedMillis.plus(86400000L)
                        }
                    }
                }
            } else if (selectedMillis != null) {
                startTimeState.hour = 12
                startTimeState.minute = 0
                endTimeState.hour = 13
                endTimeState.minute = 0
                endDatePickerState.selectedDateMillis = selectedMillis
            }
        }
    }

    val currentSelectedIds = when (visibility) {
        PlanVisibility.EXCEPT -> exceptFriendIds
        PlanVisibility.ONLY -> onlyFriendIds
        else -> emptyList()
    }

    val hasSocialChanges =
        originalVisibility != visibility || originalFriendIds != currentSelectedIds

    val isConfirmEnabled = isDateTimeValid && !isStartDateInPast && !isStartTimeInPast &&
            (visibility == PlanVisibility.EVERYONE || currentSelectedIds.isNotEmpty()) &&
            (editingPlan == null || hasSocialChanges)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (editingPlan == null) "Schedule your plan" else "Edit your plan",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Time Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Start:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = { setShowStartDatePicker(true) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = startDateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (isStartDateInPast || !isDateTimeValid) {
                                            MaterialTheme.colorScheme.error
                                        } else Color.Unspecified
                                )
                            }
                        }
                        OutlinedCard(
                            modifier = Modifier
                                .weight(0.6f)
                                .height(48.dp),
                            onClick = { setShowStartTimePicker(true) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatTimeForDisplay(
                                        formatTime(startTimeState.hour, startTimeState.minute),
                                        use24HourFormat
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (isStartTimeInPast || !isDateTimeValid) {
                                            MaterialTheme.colorScheme.error
                                        } else Color.Unspecified
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "End:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = { setShowEndDatePicker(true) }) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = endDateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!isDateTimeValid) MaterialTheme.colorScheme.error else Color.Unspecified
                                )
                            }
                        }
                        OutlinedCard(
                            modifier = Modifier
                                .weight(0.6f)
                                .height(48.dp),
                            onClick = { setShowEndTimePicker(true) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatTimeForDisplay(
                                        formatTime(endTimeState.hour, endTimeState.minute),
                                        use24HourFormat
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (!isDateTimeValid) MaterialTheme.colorScheme.error
                                        else Color.Unspecified
                                )
                            }
                        }
                    }
                }

                if (isStartDateInPast || isStartTimeInPast) {
                    Text(
                        text = "Cannot schedule plans in the past",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                if (!isDateTimeValid) {
                    Text(
                        text = "End must be after start",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = {
                        Text(
                            "What are you planning?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    placeholder = {
                        Text(
                            "Enter a brief note...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Visibility Section
                Text(
                    text = "Who can see this plan?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Column {
                    VisibilityOption(
                        label = "Everyone",
                        selected = visibility == PlanVisibility.EVERYONE,
                        onClick = {
                            visibility = PlanVisibility.EVERYONE
                        })

                    VisibilityOption(
                        label = "Everyone except...",
                        selected = visibility == PlanVisibility.EXCEPT,
                        onClick = {
                            visibility = PlanVisibility.EXCEPT
                        })
                    AnimatedVisibility(visible = visibility == PlanVisibility.EXCEPT) {
                        FriendSelector(
                            friends = friends,
                            selectedFriendIds = exceptFriendIds,
                            onToggleFriend = { friendId ->
                                exceptFriendIds = if (friendId in exceptFriendIds) {
                                    exceptFriendIds - friendId
                                } else {
                                    exceptFriendIds + friendId
                                }
                            },
                            onSelectAll = { exceptFriendIds = friends.map { it.uid } },
                            onUnselectAll = { exceptFriendIds = emptyList() }
                        )
                    }

                    VisibilityOption(
                        label = "Only selected people...",
                        selected = visibility == PlanVisibility.ONLY,
                        onClick = {
                            visibility = PlanVisibility.ONLY
                        })
                    AnimatedVisibility(visible = visibility == PlanVisibility.ONLY) {
                        FriendSelector(
                            friends = friends,
                            selectedFriendIds = onlyFriendIds,
                            onToggleFriend = { friendId ->
                                onlyFriendIds = if (friendId in onlyFriendIds) {
                                    onlyFriendIds - friendId
                                } else {
                                    onlyFriendIds + friendId
                                }
                            },
                            onSelectAll = { onlyFriendIds = friends.map { it.uid } },
                            onUnselectAll = { onlyFriendIds = emptyList() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startDateMillis = startDatePickerState.selectedDateMillis
                    val endDateMillis = endDatePickerState.selectedDateMillis

                    if (startDateMillis != null && endDateMillis != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onConfirm(
                            sdf.format(Date(startDateMillis)),
                            sdf.format(Date(endDateMillis)),
                            formatTime(startTimeState.hour, startTimeState.minute),
                            formatTime(endTimeState.hour, endTimeState.minute),
                            note,
                            visibility,
                            currentSelectedIds
                        )
                    }
                },
                enabled = isConfirmEnabled
            ) {
                Text(if (editingPlan == null) "Add" else "Update")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { setShowStartDatePicker(false) },
            confirmButton = { TextButton(onClick = { setShowStartDatePicker(false) }) { Text("OK") } }) {
            DatePicker(
                state = startDatePickerState,
                title = {
                    Text(
                        text = "Select start date",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { setShowEndDatePicker(false) },
            confirmButton = { TextButton(onClick = { setShowEndDatePicker(false) }) { Text("OK") } }) {
            DatePicker(
                state = endDatePickerState,
                title = {
                    Text(
                        text = "Select end date",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
    if (showStartTimePicker || showEndTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = if (showStartTimePicker) startTimeState.hour else endTimeState.hour,
            initialMinute = if (showStartTimePicker) startTimeState.minute else endTimeState.minute,
            is24Hour = use24HourFormat
        )
        AlertDialog(
            onDismissRequest = { setShowStartTimePicker(false); setShowEndTimePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    if (showStartTimePicker) {
                        startTimeState.hour = pickerState.hour
                        startTimeState.minute = pickerState.minute
                    } else {
                        endTimeState.hour = pickerState.hour
                        endTimeState.minute = pickerState.minute
                    }
                    setShowStartTimePicker(false)
                    setShowEndTimePicker(false)
                }) { Text("OK") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (showStartTimePicker) "Select start time" else "Select end time",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = pickerState)
                }
            }
        )
    }
}

@Composable
fun VisibilityOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun FriendSelector(
    friends: List<FriendInfo>,
    selectedFriendIds: List<String>,
    onToggleFriend: (String) -> Unit,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit
) {
    Column {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 150.dp)
        ) {
            if (friends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No friends to select",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(4.dp)) {
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
                .padding(top = 4.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Unselect all",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onUnselectAll() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Select all",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSelectAll() }
            )
        }
    }
}

@Composable
fun FriendSelectorItem(friend: FriendInfo, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = null)
        Text(
            text = friend.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

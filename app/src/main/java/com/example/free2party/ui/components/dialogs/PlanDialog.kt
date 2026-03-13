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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
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
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.free2party.R
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.data.model.InviteStatus
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

    val acceptedFriends = remember(friends) {
        friends.filter { it.inviteStatus == InviteStatus.ACCEPTED }
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
            ?: stringResource(R.string.select_start_date)
    val endDateText =
        endDatePickerState.selectedDateMillis?.let { format.format(Date(it)) }
            ?: stringResource(R.string.select_end_date)

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

    val hasChanges = remember(
        note, visibility, currentSelectedIds,
        startDatePickerState.selectedDateMillis,
        endDatePickerState.selectedDateMillis,
        startTimeState.hour, startTimeState.minute,
        endTimeState.hour, endTimeState.minute,
        editingPlan
    ) {
        if (editingPlan == null) true
        else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val currentStartDate =
                startDatePickerState.selectedDateMillis?.let { sdf.format(Date(it)) }
            val currentEndDate = endDatePickerState.selectedDateMillis?.let { sdf.format(Date(it)) }
            val currentStartTime = formatTime(startTimeState.hour, startTimeState.minute)
            val currentEndTime = formatTime(endTimeState.hour, endTimeState.minute)

            currentStartDate != editingPlan.startDate ||
                    currentEndDate != editingPlan.endDate ||
                    currentStartTime != editingPlan.startTime ||
                    currentEndTime != editingPlan.endTime ||
                    note != editingPlan.note ||
                    visibility != editingPlan.visibility ||
                    currentSelectedIds != editingPlan.friendsSelection
        }
    }

    val isConfirmEnabled = isDateTimeValid && !isStartDateInPast && !isStartTimeInPast &&
            (visibility == PlanVisibility.EVERYONE || currentSelectedIds.isNotEmpty()) &&
            hasChanges

    val dialogColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    BaseDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text =
                    if (editingPlan == null) stringResource(R.string.schedule_plan)
                    else stringResource(R.string.edit_plan),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.start_label),
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
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = startDateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isStartDateInPast || !isDateTimeValid) MaterialTheme.colorScheme.error else Color.Unspecified
                                )
                            }
                        }
                        OutlinedCard(
                            modifier = Modifier
                                .weight(0.6f)
                                .height(48.dp),
                            onClick = { setShowStartTimePicker(true) }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = formatTimeForDisplay(
                                        formatTime(
                                            startTimeState.hour,
                                            startTimeState.minute
                                        ), use24HourFormat
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isStartTimeInPast || !isDateTimeValid) MaterialTheme.colorScheme.error else Color.Unspecified
                                )
                            }
                        }
                    }
                }

                // End Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.end_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            onClick = { setShowEndDatePicker(true) }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = formatTimeForDisplay(
                                        formatTime(
                                            endTimeState.hour,
                                            endTimeState.minute
                                        ), use24HourFormat
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!isDateTimeValid) MaterialTheme.colorScheme.error else Color.Unspecified
                                )
                            }
                        }
                    }
                }

                if (isStartDateInPast || isStartTimeInPast) {
                    Text(
                        text = stringResource(R.string.error_past_plan),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                if (!isDateTimeValid) {
                    Text(
                        text = stringResource(R.string.error_invalid_datetime),
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
                            stringResource(R.string.plan_note_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.plan_note_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Visibility Section
                Text(
                    text = stringResource(R.string.visibility_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Column {
                    VisibilityOption(
                        stringResource(R.string.everyone),
                        visibility == PlanVisibility.EVERYONE
                    ) { visibility = PlanVisibility.EVERYONE }
                    VisibilityOption(
                        stringResource(R.string.everyone_except_label),
                        visibility == PlanVisibility.EXCEPT
                    ) { visibility = PlanVisibility.EXCEPT }
                    AnimatedVisibility(visible = visibility == PlanVisibility.EXCEPT) {
                        FriendSelector(
                            acceptedFriends,
                            exceptFriendIds,
                            { id ->
                                exceptFriendIds =
                                    if (id in exceptFriendIds) exceptFriendIds - id else exceptFriendIds + id
                            },
                            { exceptFriendIds = acceptedFriends.map { it.uid } },
                            { exceptFriendIds = emptyList() })
                    }
                    VisibilityOption(
                        stringResource(R.string.only_selected_people_label),
                        visibility == PlanVisibility.ONLY
                    ) { visibility = PlanVisibility.ONLY }
                    AnimatedVisibility(visible = visibility == PlanVisibility.ONLY) {
                        FriendSelector(
                            acceptedFriends,
                            onlyFriendIds,
                            { id ->
                                onlyFriendIds =
                                    if (id in onlyFriendIds) onlyFriendIds - id else onlyFriendIds + id
                            },
                            { onlyFriendIds = acceptedFriends.map { it.uid } },
                            { onlyFriendIds = emptyList() })
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val startMillis = startDatePickerState.selectedDateMillis
                        val endMillis = endDatePickerState.selectedDateMillis
                        if (startMillis != null && endMillis != null) {
                            val sdf = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            ).apply { timeZone = TimeZone.getTimeZone("UTC") }
                            onConfirm(
                                sdf.format(Date(startMillis)),
                                sdf.format(Date(endMillis)),
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
                    Text(
                        if (editingPlan == null) stringResource(R.string.add)
                        else stringResource(R.string.update)
                    )
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { setShowStartDatePicker(false) },
            colors = DatePickerDefaults.colors(containerColor = dialogColor),
            confirmButton = {
                TextButton(onClick = { setShowStartDatePicker(false) }) {
                    Text(stringResource(R.string.ok))
                }
            }) {
            DatePicker(
                state = startDatePickerState,
                colors = DatePickerDefaults.colors(containerColor = dialogColor),
                title = {
                    Text(
                        text = stringResource(R.string.select_start_date),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            )
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { setShowEndDatePicker(false) },
            colors = DatePickerDefaults.colors(containerColor = dialogColor),
            confirmButton = {
                TextButton(onClick = { setShowEndDatePicker(false) }) {
                    Text(stringResource(R.string.ok))
                }
            }) {
            DatePicker(
                state = endDatePickerState,
                colors = DatePickerDefaults.colors(containerColor = dialogColor),
                title = {
                    Text(
                        text = stringResource(R.string.select_end_date),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.headlineMedium
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
        BaseDialog(onDismissRequest = {
            setShowStartTimePicker(false); setShowEndTimePicker(false)
        }) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text =
                        if (showStartTimePicker) stringResource(R.string.select_start_time)
                        else stringResource(R.string.select_end_time),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TimePicker(
                    state = pickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = dialogColor,
                        containerColor = dialogColor,
                        periodSelectorUnselectedContainerColor = dialogColor,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        timeSelectorUnselectedContainerColor = dialogColor
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
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
                    }) { Text(stringResource(R.string.ok)) }
                }
            }
        }
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
                .padding(vertical = 4.dp)
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
                        stringResource(R.string.no_friends_to_select),
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
                .padding(top = 4.dp, bottom = 4.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(R.string.unselect_all),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onUnselectAll() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.select_all),
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

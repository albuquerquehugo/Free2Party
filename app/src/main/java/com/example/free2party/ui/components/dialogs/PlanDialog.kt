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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.free2party.R
import com.example.free2party.data.model.Circle
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.data.model.InviteStatus
import com.example.free2party.data.model.PlanVisibility
import com.example.free2party.ui.theme.inactive
import com.example.free2party.util.formatTime
import com.example.free2party.util.formatTimeForDisplay
import com.example.free2party.util.isDateTimeInPast
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMinutes
import com.example.free2party.util.unformatTime
import com.example.free2party.util.capitalizeWords
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
    circles: List<Circle>,
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
        val initialIds =
            if (editingPlan?.visibility == PlanVisibility.EXCEPT) editingPlan.friendsSelection else emptyList()
        val currentFriendIds = friends.map { it.uid }.toSet()
        mutableStateOf(initialIds.filter { it in currentFriendIds })
    }
    var onlyFriendIds by remember {
        val initialIds =
            if (editingPlan?.visibility == PlanVisibility.ONLY) editingPlan.friendsSelection else emptyList()
        val currentFriendIds = friends.map { it.uid }.toSet()
        mutableStateOf(initialIds.filter { it in currentFriendIds })
    }

    val (showStartDatePicker, setShowStartDatePicker) = remember { mutableStateOf(false) }
    val (showEndDatePicker, setShowEndDatePicker) = remember { mutableStateOf(false) }
    val (showStartTimePicker, setShowStartTimePicker) = remember { mutableStateOf(false) }
    val (showEndTimePicker, setShowEndTimePicker) = remember { mutableStateOf(false) }

    LaunchedEffect(friends) {
        val currentFriendIds = friends.map { it.uid }.toSet()
        if (exceptFriendIds.any { it !in currentFriendIds }) {
            exceptFriendIds = exceptFriendIds.filter { it in currentFriendIds }
        }
        if (onlyFriendIds.any { it !in currentFriendIds }) {
            onlyFriendIds = onlyFriendIds.filter { it in currentFriendIds }
        }
    }

    val format = remember {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val startDateText =
        startDatePickerState.selectedDateMillis?.let { format.format(Date(it)).capitalizeWords() }
            ?: stringResource(R.string.select_start_date)
    val endDateText =
        endDatePickerState.selectedDateMillis?.let { format.format(Date(it)).capitalizeWords() }
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
        editingPlan?.let { plan ->
            val start = unformatTime(plan.startTime)
            val end = unformatTime(plan.endTime)
            startTimeState.hour = start.first
            startTimeState.minute = start.second
            endTimeState.hour = end.first
            endTimeState.minute = end.second

            startDatePickerState.selectedDateMillis = parseDateToMillis(plan.startDate)
            endDatePickerState.selectedDateMillis = parseDateToMillis(plan.endDate)

            visibility = plan.visibility
            val currentFriendIds = friends.map { it.uid }.toSet()
            val filteredSelection = plan.friendsSelection.filter { id -> id in currentFriendIds }

            if (plan.visibility == PlanVisibility.EXCEPT) {
                exceptFriendIds = filteredSelection
            } else if (plan.visibility == PlanVisibility.ONLY) {
                onlyFriendIds = filteredSelection
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
        editingPlan,
        friends
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

            val currentFriendIds = friends.map { it.uid }.toSet()
            val filteredOriginalSelection =
                editingPlan.friendsSelection.filter { it in currentFriendIds }

            currentStartDate != editingPlan.startDate ||
                    currentEndDate != editingPlan.endDate ||
                    currentStartTime != editingPlan.startTime ||
                    currentEndTime != editingPlan.endTime ||
                    note != editingPlan.note ||
                    visibility != editingPlan.visibility ||
                    currentSelectedIds != filteredOriginalSelection
        }
    }

    val isConfirmEnabled = isDateTimeValid && !isStartDateInPast && !isStartTimeInPast &&
            (visibility == PlanVisibility.EVERYONE || currentSelectedIds.isNotEmpty()) &&
            hasChanges

    BaseDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text =
                    if (editingPlan == null) stringResource(R.string.schedule_plan)
                    else stringResource(R.string.edit_plan),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Start Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.start_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(60.dp)
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
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
                            .height(44.dp),
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

                // End Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.end_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(60.dp)
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
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
                            .height(44.dp),
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
                    textStyle = MaterialTheme.typography.bodySmall,
                    label = {
                        Text(
                            stringResource(R.string.plan_note_label),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.plan_note_placeholder),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Visibility Section
                Column {
                    Text(
                        text = stringResource(R.string.visibility_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.visibility_label_discretion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inactive
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    VisibilityOption(
                        stringResource(R.string.everyone),
                        visibility == PlanVisibility.EVERYONE
                    ) { visibility = PlanVisibility.EVERYONE }
                    VisibilityOption(
                        stringResource(R.string.everyone_except_label),
                        visibility == PlanVisibility.EXCEPT,
                        Modifier.testTag("visibility_except")
                    ) { visibility = PlanVisibility.EXCEPT }
                    AnimatedVisibility(visible = visibility == PlanVisibility.EXCEPT) {
                        FriendSelector(
                            friends,
                            circles,
                            exceptFriendIds,
                            { id ->
                                exceptFriendIds =
                                    if (id in exceptFriendIds) exceptFriendIds - id else exceptFriendIds + id
                            },
                            { ids ->
                                exceptFriendIds = (exceptFriendIds + ids).distinct()
                            },
                            { ids ->
                                exceptFriendIds = exceptFriendIds - ids.toSet()
                            },
                            { exceptFriendIds = friends.map { it.uid } },
                            { exceptFriendIds = emptyList() })
                    }
                    VisibilityOption(
                        stringResource(R.string.only_selected_people_label),
                        visibility == PlanVisibility.ONLY,
                        Modifier.testTag("visibility_only")
                    ) { visibility = PlanVisibility.ONLY }
                    AnimatedVisibility(visible = visibility == PlanVisibility.ONLY) {
                        FriendSelector(
                            friends,
                            circles,
                            onlyFriendIds,
                            { id ->
                                onlyFriendIds =
                                    if (id in onlyFriendIds) onlyFriendIds - id else onlyFriendIds + id
                            },
                            { ids ->
                                onlyFriendIds = (onlyFriendIds + ids).distinct()
                            },
                            { ids ->
                                onlyFriendIds = onlyFriendIds - ids.toSet()
                            },
                            { onlyFriendIds = friends.map { it.uid } },
                            { onlyFriendIds = emptyList() })
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
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
                    modifier = Modifier.testTag("plan_dialog_confirm_button"),
                    enabled = isConfirmEnabled
                ) {
                    Text(
                        if (editingPlan == null) stringResource(R.string.button_add)
                        else stringResource(R.string.button_update)
                    )
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { setShowStartDatePicker(false) },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                TextButton(onClick = { setShowStartDatePicker(false) }) {
                    Text(stringResource(R.string.button_ok))
                }
            }) {
            DatePicker(
                state = startDatePickerState,
                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
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
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                TextButton(onClick = { setShowEndDatePicker(false) }) {
                    Text(stringResource(R.string.button_ok))
                }
            }) {
            DatePicker(
                state = endDatePickerState,
                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
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
                        clockDialColor = MaterialTheme.colorScheme.surface,
                        containerColor = MaterialTheme.colorScheme.surface,
                        periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface
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
                    }) { Text(stringResource(R.string.button_ok)) }
                }
            }
        }
    }
}

@Composable
fun VisibilityOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

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
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .heightIn(max = 200.dp)
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
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
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
                text = stringResource(R.string.unselect_all),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .testTag("unselect_all")
                    .clickable { onUnselectAll() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.select_all),
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
    val isInvited = friend.inviteStatus == InviteStatus.INVITED
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
                    fontWeight = FontWeight.Bold
                )
                if (isInvited) {
                    Text(
                        text = " " + stringResource(R.string.label_invited_observation),
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
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.label_circle_member_count, memberCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

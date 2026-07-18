package com.free2party.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.data.model.Circle
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.FuturePlan
import com.free2party.data.model.PlanVisibility
import com.free2party.ui.components.FriendSelector
import com.free2party.ui.components.AppDateTimeSection
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.components.basic.AppOutlinedTextField
import com.free2party.ui.theme.inactive
import com.free2party.util.capitalizeWords
import com.free2party.util.formatTime
import com.free2party.util.formatTimeForDisplay
import com.free2party.util.isDateTimeInPast
import com.free2party.util.parseDateToMillis
import com.free2party.util.parseTimeToMinutes
import com.free2party.util.TextFieldRegistry
import com.free2party.util.unformatTime
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDialog(
    editingPlan: FuturePlan?,
    use24HourFormat: Boolean,
    friends: List<FriendInfo>,
    circles: List<Circle>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, PlanVisibility, List<String>) -> Unit
) {
    val key = remember { Any() }
    DisposableEffect(key) {
        onDispose {
            TextFieldRegistry.unregister(key)
        }
    }

    val startDatePickerState = rememberDatePickerState()
    val startTimeState = key(use24HourFormat) {
        rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = use24HourFormat)
    }
    val endDatePickerState = rememberDatePickerState()
    val endTimeState = key(use24HourFormat) {
        rememberTimePickerState(initialHour = 13, initialMinute = 0, is24Hour = use24HourFormat)
    }

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
    var isStartDateSelected by remember { mutableStateOf(editingPlan != null) }
    var isStartTimeSelected by remember { mutableStateOf(editingPlan != null) }
    var isEndDateSelected by remember { mutableStateOf(editingPlan != null) }
    var isEndTimeSelected by remember { mutableStateOf(editingPlan != null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var hasInteractedWithStartDate by remember { mutableStateOf(false) }
    var hasInteractedWithStartTime by remember { mutableStateOf(false) }
    var hasInteractedWithEndDate by remember { mutableStateOf(false) }
    var hasInteractedWithEndTime by remember { mutableStateOf(false) }

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
    val startDateText = if (isStartDateSelected) {
        startDatePickerState.selectedDateMillis?.let { format.format(Date(it)).capitalizeWords() }
            ?: ""
    } else {
        stringResource(R.string.label_select_date)
    }
    val endDateText = if (isEndDateSelected) {
        endDatePickerState.selectedDateMillis?.let { format.format(Date(it)).capitalizeWords() }
            ?: ""
    } else {
        stringResource(R.string.label_select_date)
    }

    val isDateTimeValid = remember(
        isStartDateSelected,
        isEndDateSelected,
        isStartTimeSelected,
        isEndTimeSelected,
        startDatePickerState.selectedDateMillis,
        endDatePickerState.selectedDateMillis,
        startTimeState.hour,
        startTimeState.minute,
        endTimeState.hour,
        endTimeState.minute
    ) {
        if (!isStartDateSelected || !isEndDateSelected || !isStartTimeSelected || !isEndTimeSelected) {
            return@remember true
        }

        val startMillis = startDatePickerState.selectedDateMillis ?: 0L
        val endMillis = endDatePickerState.selectedDateMillis ?: 0L

        if (startMillis < endMillis) return@remember true
        if (startMillis > endMillis) return@remember false

        val startMins =
            parseTimeToMinutes(formatTime(startTimeState.hour, startTimeState.minute)) ?: 0
        val endMins = parseTimeToMinutes(formatTime(endTimeState.hour, endTimeState.minute)) ?: 0
        startMins < endMins
    }

    val isStartDateInPast = if (isStartDateSelected) {
        startDatePickerState.selectedDateMillis?.let {
            isDateTimeInPast(it, null)
        } ?: false
    } else {
        false
    }
    val isStartTimeInPast = if (isStartDateSelected && isStartTimeSelected) {
        startDatePickerState.selectedDateMillis?.let {
            isDateTimeInPast(it, formatTime(startTimeState.hour, startTimeState.minute))
        } ?: false
    } else {
        false
    }

    LaunchedEffect(editingPlan) {
        if (editingPlan == null) {
            startDatePickerState.selectedDateMillis = null
            endDatePickerState.selectedDateMillis = null
            isStartDateSelected = false
            isStartTimeSelected = false
            isEndDateSelected = false
            isEndTimeSelected = false
        } else {
            val start = unformatTime(editingPlan.startTime)
            val end = unformatTime(editingPlan.endTime)
            startTimeState.hour = start.first
            startTimeState.minute = start.second
            endTimeState.hour = end.first
            endTimeState.minute = end.second

            startDatePickerState.selectedDateMillis = parseDateToMillis(editingPlan.startDate)
            endDatePickerState.selectedDateMillis = parseDateToMillis(editingPlan.endDate)
            isStartDateSelected = editingPlan.startDate.isNotBlank()
            isStartTimeSelected = editingPlan.startTime.isNotBlank()
            isEndDateSelected = editingPlan.endDate.isNotBlank()
            isEndTimeSelected = editingPlan.endTime.isNotBlank()

            visibility = editingPlan.visibility
            val currentFriendIds = friends.map { it.uid }.toSet()
            val filteredSelection =
                editingPlan.friendsSelection.filter { id -> id in currentFriendIds }

            if (editingPlan.visibility == PlanVisibility.EXCEPT) {
                exceptFriendIds = filteredSelection
            } else if (editingPlan.visibility == PlanVisibility.ONLY) {
                onlyFriendIds = filteredSelection
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
        if (editingPlan == null) {
            note.isNotEmpty() ||
                    visibility != PlanVisibility.EVERYONE ||
                    currentSelectedIds.isNotEmpty()
        } else {
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

    val isConfirmEnabled = isDateTimeValid &&
            isStartDateSelected && isStartTimeSelected &&
            isEndDateSelected && isEndTimeSelected &&
            !isStartDateInPast && !isStartTimeInPast &&
            (visibility == PlanVisibility.EVERYONE || currentSelectedIds.isNotEmpty()) &&
            (editingPlan == null || hasChanges)

    val handleDismiss = {
        if (hasChanges) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    AppBaseDialog(onDismissRequest = handleDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text =
                    if (editingPlan == null) stringResource(R.string.label_schedule_plan)
                    else stringResource(R.string.label_edit_plan),
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
                AppDateTimeSection(
                    startDateText = startDateText,
                    startTimeText = if (isStartTimeSelected) {
                        formatTimeForDisplay(
                            formatTime(
                                startTimeState.hour,
                                startTimeState.minute
                            ), use24HourFormat
                        )
                    } else {
                        stringResource(R.string.label_select_time)
                    },
                    endDateText = endDateText,
                    endTimeText = if (isEndTimeSelected) {
                        formatTimeForDisplay(
                            formatTime(
                                endTimeState.hour,
                                endTimeState.minute
                            ), use24HourFormat
                        )
                    } else {
                        stringResource(R.string.label_select_time)
                    },
                    isStartDateSelected = isStartDateSelected,
                    isStartTimeSelected = isStartTimeSelected,
                    isEndDateSelected = isEndDateSelected,
                    isEndTimeSelected = isEndTimeSelected,
                    isStartDateInPast = isStartDateInPast,
                    isStartTimeInPast = isStartTimeInPast,
                    isDateTimeValid = isDateTimeValid,
                    onStartDateClick = { setShowStartDatePicker(true) },
                    onStartTimeClick = { setShowStartTimePicker(true) },
                    onEndDateClick = { setShowEndDatePicker(true) },
                    onEndTimeClick = { setShowEndTimePicker(true) },
                    labelStyle = MaterialTheme.typography.labelLarge,
                    cardTextStyle = MaterialTheme.typography.bodySmall,
                    isEndDateTimeRequired = true,
                    pastErrorResId = R.string.error_past_plan,
                    cardTextColorNormal = Color.Unspecified,
                    hasInteractedWithStartDate = hasInteractedWithStartDate,
                    hasInteractedWithStartTime = hasInteractedWithStartTime,
                    hasInteractedWithEndDate = hasInteractedWithEndDate,
                    hasInteractedWithEndTime = hasInteractedWithEndTime
                )

                AppOutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    labelText = stringResource(R.string.label_plan_note),
                    placeholderText = stringResource(R.string.placeholder_plan_note),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            TextFieldRegistry.register(key, coordinates)
                        },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    )
                )

                AppHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Visibility Section
                Column {
                    Text(
                        text = stringResource(R.string.text_visibility),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.label_visibility_discretion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inactive
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    VisibilityOption(
                        stringResource(R.string.label_everyone),
                        visibility == PlanVisibility.EVERYONE
                    ) { visibility = PlanVisibility.EVERYONE }
                    VisibilityOption(
                        stringResource(R.string.label_everyone_except_label),
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
                                    if (id in exceptFriendIds) exceptFriendIds - id
                                    else exceptFriendIds + id
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
                        stringResource(R.string.label_only_selected_people_label),
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
                                    if (id in onlyFriendIds) onlyFriendIds - id
                                    else onlyFriendIds + id
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
                TextButton(onClick = handleDismiss) { Text(stringResource(R.string.label_cancel)) }
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
                        if (editingPlan == null) stringResource(R.string.label_add)
                        else stringResource(R.string.label_update)
                    )
                }
            }
        }
    }

    if (showStartDatePicker) {
        AppDatePickerDialog(
            state = startDatePickerState,
            title = stringResource(R.string.label_start_date),
            onDismiss = {
                setShowStartDatePicker(false)
                hasInteractedWithStartDate = true
            },
            onConfirm = {
                setShowStartDatePicker(false)
                isStartDateSelected = true
                hasInteractedWithStartDate = true
            }
        )
    }

    if (showEndDatePicker) {
        AppDatePickerDialog(
            state = endDatePickerState,
            title = stringResource(R.string.label_end_date),
            onDismiss = {
                setShowEndDatePicker(false)
                hasInteractedWithEndDate = true
            },
            onConfirm = {
                setShowEndDatePicker(false)
                isEndDateSelected = true
                hasInteractedWithEndDate = true
            }
        )
    }
    if (showStartTimePicker || showEndTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = if (showStartTimePicker) startTimeState.hour else endTimeState.hour,
            initialMinute = if (showStartTimePicker) startTimeState.minute else endTimeState.minute,
            is24Hour = use24HourFormat
        )
        AppTimePickerDialog(
            state = pickerState,
            title = if (showStartTimePicker) stringResource(R.string.label_start_time)
            else stringResource(R.string.label_end_time),
            onDismissRequest = {
                if (showStartTimePicker) {
                    hasInteractedWithStartTime = true
                }
                if (showEndTimePicker) {
                    hasInteractedWithEndTime = true
                }
                setShowStartTimePicker(false)
                setShowEndTimePicker(false)
            },
            onConfirm = {
                if (showStartTimePicker) {
                    startTimeState.hour = pickerState.hour
                    startTimeState.minute = pickerState.minute
                    isStartTimeSelected = true
                    hasInteractedWithStartTime = true
                } else {
                    endTimeState.hour = pickerState.hour
                    endTimeState.minute = pickerState.minute
                    isEndTimeSelected = true
                    hasInteractedWithEndTime = true
                }
                setShowStartTimePicker(false)
                setShowEndTimePicker(false)
            }
        )
    }

    if (showDiscardConfirm) {
        AppConfirmationDialog(
            title = stringResource(R.string.label_discard_changes),
            text = stringResource(R.string.text_discard_changes_confirmation),
            confirmButtonText = stringResource(R.string.label_discard_changes),
            onConfirm = {
                showDiscardConfirm = false
                onDismiss()
            },
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismissRequest = { showDiscardConfirm = false },
            isDestructive = true
        )
    }
}

@Composable
fun VisibilityOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                focusManager.clearFocus()
                onClick()
            }
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

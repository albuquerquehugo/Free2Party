package com.example.free2party.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import com.example.free2party.data.model.FuturePlan
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
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit,
    startDatePickerState: DatePickerState,
    endDatePickerState: DatePickerState,
    startTimeState: TimePickerState,
    endTimeState: TimePickerState
) {
    var note by remember { mutableStateOf(editingPlan?.note ?: "") }
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

    // Initialize times when the dialog opens or when the editing plan changes
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
        }
    }

    // Update default times when the date changes, but ONLY for new plans
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

                        // Suggest 1 hour duration
                        if (startTimeState.hour < 23) {
                            endTimeState.hour = startTimeState.hour + 1
                            endTimeState.minute = 0
                            endDatePickerState.selectedDateMillis = selectedMillis
                        } else {
                            // Crosses midnight
                            endTimeState.hour = 0
                            endTimeState.minute = 0
                            endDatePickerState.selectedDateMillis =
                                selectedMillis.plus(86400000L) // +1 day
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (editingPlan == null) "Schedule your future plan" else "Edit plan",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Start section
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

                // End section
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
                            onClick = { setShowEndDatePicker(true) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = endDateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (!isDateTimeValid) MaterialTheme.colorScheme.error
                                        else Color.Unspecified
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
                            note
                        )
                    }
                },
                enabled = isDateTimeValid && !isStartDateInPast && !isStartTimeInPast
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

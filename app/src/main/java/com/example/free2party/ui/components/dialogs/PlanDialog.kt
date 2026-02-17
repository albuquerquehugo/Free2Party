package com.example.free2party.ui.components.dialogs

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.util.formatTime
import com.example.free2party.util.isDateTimeInPast
import com.example.free2party.util.parseTimeToMinutes
import com.example.free2party.util.unformatTime
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDialog(
    editingPlan: FuturePlan?,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String) -> Unit,
    datePickerState: DatePickerState,
    startTimeState: TimePickerState,
    endTimeState: TimePickerState
) {
    var note by remember { mutableStateOf(editingPlan?.note ?: "") }
    val (showDatePicker, setShowDatePicker) = remember { mutableStateOf(false) }
    val (showStartTimePicker, setShowStartTimePicker) = remember { mutableStateOf(false) }
    val (showEndTimePicker, setShowEndTimePicker) = remember { mutableStateOf(false) }

    val context = LocalContext.current

    val selectedDateText = datePickerState.selectedDateMillis?.let {
        val format = DateFormat.getDateInstance()
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(it))
    } ?: "Select date"

    val isTimeValid = remember(
        startTimeState.hour,
        startTimeState.minute,
        endTimeState.hour,
        endTimeState.minute
    ) {
        val start = parseTimeToMinutes(formatTime(startTimeState.hour, startTimeState.minute))
        val end = parseTimeToMinutes(formatTime(endTimeState.hour, endTimeState.minute))
        start < end
    }

    val isSelectedDateInPast = datePickerState.selectedDateMillis?.let {
        isDateTimeInPast(it, null)
    } ?: false
    val isStartTimeInPast = datePickerState.selectedDateMillis?.let {
        isDateTimeInPast(it, formatTime(startTimeState.hour, startTimeState.minute))
    } ?: false
    val isEndTimeInPast = datePickerState.selectedDateMillis?.let {
        isDateTimeInPast(it, formatTime(endTimeState.hour, endTimeState.minute))
    } ?: false

    LaunchedEffect(editingPlan) {
        editingPlan?.let {
            val start = unformatTime(it.startTime)
            val end = unformatTime(it.endTime)
            startTimeState.hour = start.first
            startTimeState.minute = start.second
            endTimeState.hour = end.first
            endTimeState.minute = end.second
        } ?: run {
            startTimeState.hour = 12
            startTimeState.minute = 0
            endTimeState.hour = 13
            endTimeState.minute = 0
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "Date:", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = { setShowDatePicker(true) }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedDateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    if (isSelectedDateInPast) MaterialTheme.colorScheme.error
                                    else Color.Unspecified
                            )
                        }
                    }

                    if (isSelectedDateInPast) {
                        Text(
                            text = "Cannot schedule plans for past dates",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WatchLater,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = "From:", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(
                                    width = if (!isTimeValid || isStartTimeInPast) 1.dp else 0.dp,
                                    color =
                                        if (!isTimeValid || isStartTimeInPast) MaterialTheme.colorScheme.error
                                        else Color.Transparent,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            onClick = { setShowStartTimePicker(true) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatTime(startTimeState.hour, startTimeState.minute),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        if (!isTimeValid || isStartTimeInPast) MaterialTheme.colorScheme.error
                                        else Color.Unspecified
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WatchLater,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = "To:", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(
                                    width = if (!isTimeValid || isEndTimeInPast) 1.dp else 0.dp,
                                    color =
                                        if (!isTimeValid || isEndTimeInPast) MaterialTheme.colorScheme.error
                                        else Color.Transparent,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            onClick = { setShowEndTimePicker(true) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatTime(endTimeState.hour, endTimeState.minute),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        if (!isTimeValid || isEndTimeInPast) MaterialTheme.colorScheme.error
                                        else Color.Unspecified
                                )
                            }
                        }
                    }
                }

                if (!isSelectedDateInPast && (isStartTimeInPast || isEndTimeInPast)) {
                    Text(
                        text = "Cannot schedule plans for past times",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                if (!isTimeValid) {
                    Text(
                        text = "End time must be after start time",
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
                    datePickerState.selectedDateMillis?.let { date ->
                        onConfirm(
                            date,
                            formatTime(startTimeState.hour, startTimeState.minute),
                            formatTime(endTimeState.hour, endTimeState.minute),
                            note
                        )
                    }
                },
                enabled = datePickerState.selectedDateMillis != null && isTimeValid &&
                        !isStartTimeInPast && !isEndTimeInPast
            ) {
                Text(if (editingPlan == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { setShowDatePicker(false) },
            confirmButton = { TextButton(onClick = { setShowDatePicker(false) }) { Text("OK") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showStartTimePicker || showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { setShowStartTimePicker(false); setShowEndTimePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    setShowStartTimePicker(false)
                    setShowEndTimePicker(false)
                    if (!isTimeValid) {
                        Toast.makeText(
                            context,
                            "End time must be after start time!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text("OK") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (showStartTimePicker) "Select start time" else "Select end time",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = if (showStartTimePicker) startTimeState else endTimeState)
                }
            }
        )
    }
}

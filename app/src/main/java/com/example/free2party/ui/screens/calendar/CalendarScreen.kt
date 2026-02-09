package com.example.free2party.ui.screens.calendar

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.util.formatTime
import com.example.free2party.util.isDateTimeInPast
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMinutes
import com.example.free2party.util.unformatTime
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val context = LocalContext.current

    val plannedDays =
        viewModel.getPlannedDaysForMonth(viewModel.displayedYear, viewModel.displayedMonth)

    val (showPlanDialog, setShowPlanDialog) = remember { mutableStateOf(false) }
    val (editingPlan, setEditingPlan) = remember { mutableStateOf<FuturePlan?>(null) }

    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (planToDelete, setPlanToDelete) = remember { mutableStateOf<FuturePlan?>(null) }

    val datePickerState = rememberDatePickerState()
    val startTimeState = rememberTimePickerState(initialHour = 12, initialMinute = 0)
    val endTimeState = rememberTimePickerState(initialHour = 13, initialMinute = 0)

    val selectedDateText = datePickerState.selectedDateMillis?.let {
        val format = DateFormat.getDateInstance()
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(it))
    } ?: ""

    val isSelectedDateInPast =
        viewModel.selectedDateMillis?.let { isDateTimeInPast(it) } ?: false

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MonthCalendar(viewModel = viewModel, plannedDays = plannedDays)

            IconButton(
                onClick = {
                    setEditingPlan(null)
                    setShowPlanDialog(true)
                },
                enabled = !isSelectedDateInPast,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 24.dp)
                    .background(
                        if (isSelectedDateInPast) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Plan",
                    tint = if (isSelectedDateInPast) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.38f
                    )
                    else MaterialTheme.colorScheme.onPrimary
                )
            }

            PlanResults(
                viewModel = viewModel,
                selectedDateText = selectedDateText,
                setShowPlanDialog = setShowPlanDialog,
                setEditingPlan = setEditingPlan,
                setShowDeleteDialog = setShowDeleteDialog,
                setPlanToDelete = setPlanToDelete
            )
        }
    }

    if (showPlanDialog) {
        PlanDialog(
            editingPlan = editingPlan,
            onDismiss = { setShowPlanDialog(false) },
            onConfirm = { date, start, end, note ->
                val onError = { errorMessage: String ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                val onSuccess = { message: String ->
                    setShowPlanDialog(false)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                if (editingPlan == null) {
                    viewModel.savePlan(
                        date = date,
                        startTime = start,
                        endTime = end,
                        note = note,
                        onValidationError = onError,
                        onSuccess = { onSuccess("Plan successfully added!") }
                    )
                } else {
                    viewModel.updatePlan(
                        planId = editingPlan.id,
                        date = date,
                        startTime = start,
                        endTime = end,
                        note = note,
                        onError = onError,
                        onSuccess = { onSuccess("Plan successfully updated!") }
                    )
                }
            },
            datePickerState = datePickerState,
            startTimeState = startTimeState,
            endTimeState = endTimeState
        )
    }

    if (showDeleteDialog && planToDelete != null) {
        AlertDialog(
            title = { Text("Delete Plan") },
            text = { Text("Are you sure you want to delete this plan?") },
            onDismissRequest = { setShowDeleteDialog(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlan(
                            planId = planToDelete.id,
                            onError = { errorMessage: String ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            },
                            onSuccess = {
                                setShowDeleteDialog(false)
                                Toast.makeText(context, "Plan deleted!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowDeleteDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sync Grid/ViewModel and DatePicker to ensure consistency
    LaunchedEffect(viewModel.selectedDateMillis) {
        if (viewModel.selectedDateMillis != datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis = viewModel.selectedDateMillis
        }
    }
    LaunchedEffect(datePickerState.selectedDateMillis) {
        if (datePickerState.selectedDateMillis != viewModel.selectedDateMillis) {
            viewModel.selectedDateMillis = datePickerState.selectedDateMillis
        }
    }
}

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
                    text = if (editingPlan == null) "Schedule your future Free2Party plan" else "Edit plan",
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

@Composable
fun PlanResults(
    viewModel: CalendarViewModel,
    selectedDateText: String,
    setShowPlanDialog: (Boolean) -> Unit,
    setEditingPlan: (FuturePlan?) -> Unit,
    setShowDeleteDialog: (Boolean) -> Unit,
    setPlanToDelete: (FuturePlan?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Results for: $selectedDateText",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (viewModel.filteredPlans.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (viewModel.selectedDateMillis == null) {
                                "Select a day on the calendar to see your future plans"
                            } else "No plans for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                items(viewModel.filteredPlans) { plan ->
                    PlanItem(
                        plan = plan,
                        onEdit = {
                            setEditingPlan(plan)
                            setShowPlanDialog(true)
                        },
                        onDelete = {
                            setPlanToDelete(plan)
                            setShowDeleteDialog(true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlanItem(
    plan: FuturePlan,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val isPastDateTime = remember(plan.date, plan.startTime) {
        val millis = parseDateToMillis(plan.date)
        if (millis != null) {
            isDateTimeInPast(millis, plan.startTime)
        } else {
            Log.e("PlanItem", "Invalid date: ${plan.date}")
            false
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
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${plan.startTime} - ${plan.endTime}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = duration,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (plan.note.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = plan.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                enabled = !isPastDateTime,
                onClick = {
                    showMenu = false
                    onEdit()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "edit"
                    )
                }
            )

            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

@Composable
fun MonthCalendar(
    viewModel: CalendarViewModel,
    plannedDays: Set<Int>
) {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, viewModel.displayedYear)
        set(Calendar.MONTH, viewModel.displayedMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        .format(calendar.time)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month navigation header
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { viewModel.moveToPreviousMonth() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous"
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-8).dp)
            ) {
                Text(text = monthName, style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = { viewModel.goToToday() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = { viewModel.moveToNextMonth() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next"
                )
            }
        }

        // Day of the week headers (S, M, T, W, T, F, S)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.wrapContentHeight(),
            userScrollEnabled = false
        ) {
            // Add empty boxes for days before the 1st day of the month
            items(firstDayOfWeek) { Spacer(modifier = Modifier.fillMaxSize()) }

            items(daysInMonth) { index ->
                val day = index + 1
                val isPlanned = plannedDays.contains(day)

                val dateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.YEAR, viewModel.displayedYear)
                    set(Calendar.MONTH, viewModel.displayedMonth)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                // Check if this specific cell is actually "Today"
                val today = Calendar.getInstance()
                val isToday = day == today.get(Calendar.DAY_OF_MONTH) &&
                        viewModel.displayedMonth == today.get(Calendar.MONTH) &&
                        viewModel.displayedYear == today.get(Calendar.YEAR)

                val isSelected = viewModel.selectedDateMillis?.let {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = it
                    cal.get(Calendar.DAY_OF_MONTH) == day &&
                            cal.get(Calendar.MONTH) == viewModel.displayedMonth &&
                            cal.get(Calendar.YEAR) == viewModel.displayedYear
                } ?: false

                Box(
                    modifier = Modifier
                        .aspectRatio(1.8f)
                        .padding(1.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.selectDate(day) },
                    contentAlignment = Alignment.Center
                ) {
                    // Highlight selection
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else if (isPlanned) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                        )
                    }

                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }

                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isPlanned -> MaterialTheme.colorScheme.onPrimaryContainer
                            isDateTimeInPast(dateMillis) && !isToday -> Color.Gray.copy(alpha = 0.5f)
                            else -> Color.Unspecified
                        }
                    )
                }
            }
        }
    }
}

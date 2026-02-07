package com.example.free2party.ui.screens.calendar

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.util.formatTime
import com.example.free2party.util.timeToMinutes
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
    val focusManager = LocalFocusManager.current

    val plannedDays =
        viewModel.getPlannedDaysForMonth(viewModel.displayedYear, viewModel.displayedMonth)

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val selectedDateText = datePickerState.selectedDateMillis?.let {
        val format = DateFormat.getDateInstance()
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(it))
    } ?: "Select date"

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val startTimeState = rememberTimePickerState(initialHour = 12, initialMinute = 0)
    val endTimeState = rememberTimePickerState(initialHour = 13, initialMinute = 0)
    val isTimeValid = remember(
        startTimeState.hour,
        startTimeState.minute,
        endTimeState.hour,
        endTimeState.minute
    ) {
        val start = timeToMinutes(formatTime(startTimeState.hour, startTimeState.minute))
        val end = timeToMinutes(formatTime(endTimeState.hour, endTimeState.minute))
        start < end
    }

    var note by remember { mutableStateOf("") }

    var editingPlanId by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<FuturePlan?>(null) }
    val closeDeleteDialog = {
        showDeleteDialog = false
        planToDelete = null
    }


    fun clearFieldsAndSelection() {
        startTimeState.hour = 12
        startTimeState.minute = 0
        endTimeState.hour = 13
        endTimeState.minute = 0
        note = ""
        focusManager.clearFocus()
    }

    // TODO: convert new plan fields to a Dialog
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthCalendar(viewModel = viewModel, plannedDays = plannedDays)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            "Schedule your future availability",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Date selection card
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(48.dp),
            onClick = { showDatePicker = true }
        ) {
            ListItem(
                modifier = Modifier.fillMaxHeight(),
                headlineContent = {
                    Text(
                        text = "Date: $selectedDateText",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingContent = { Icon(Icons.Default.DateRange, contentDescription = null) },
                trailingContent = {
                    if (viewModel.selectedDateMillis != null && editingPlanId == null) {
                        IconButton(
                            modifier = Modifier
                                .offset(x = 16.dp)
                                .minimumInteractiveComponentSize(),
                            onClick = { viewModel.clearSelectedDate() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear date",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        // Time selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Start time card
            OutlinedCard(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(
                        width = if (!isTimeValid) 1.dp else 0.dp,
                        color = if (!isTimeValid) MaterialTheme.colorScheme.error else Color.Transparent,
                        shape = MaterialTheme.shapes.medium
                    ),
                onClick = { showStartTimePicker = true }
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Default.WatchLater,
                            contentDescription = null,
                            tint =
                                if (!isTimeValid) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    headlineContent = {
                        Text(
                            text = "From: ${
                                formatTime(startTimeState.hour, startTimeState.minute)
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (!isTimeValid) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                )
            }

            // End time card
            OutlinedCard(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(
                        width = if (!isTimeValid) 1.dp else 0.dp,
                        color = if (!isTimeValid) MaterialTheme.colorScheme.error else Color.Transparent,
                        shape = MaterialTheme.shapes.medium
                    ),
                onClick = { showEndTimePicker = true }
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Default.WatchLater,
                            contentDescription = null,
                            tint =
                                if (!isTimeValid) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    headlineContent = {
                        Text(
                            text = "To: ${formatTime(endTimeState.hour, endTimeState.minute)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (!isTimeValid) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                )
            }
        }

        // Note text-field
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = {
                Text(
                    text = "What are you planning?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            placeholder = {
                Text(
                    text = "Enter a brief note...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cancel update button
            if (editingPlanId != null) {
                OutlinedButton(
                    onClick = {
                        editingPlanId = null
                        clearFieldsAndSelection()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }

            // Add/update button
            Button(
                onClick = {
                    val onError = { errorMessage: String ->
                        Toast.makeText(
                            context,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    val onAdded = {
                        clearFieldsAndSelection()
                        Toast.makeText(
                            context,
                            "Plan successfully added to you calendar!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    val onUpdated = {
                        editingPlanId = null
                        clearFieldsAndSelection()
                        Toast.makeText(
                            context,
                            "Plan successfully updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    viewModel.selectedDateMillis?.let { date ->
                        if (editingPlanId == null) {
                            viewModel.savePlan(
                                date = date,
                                startTime = formatTime(startTimeState.hour, startTimeState.minute),
                                endTime = formatTime(endTimeState.hour, endTimeState.minute),
                                note = note,
                                onValidationError = onError,
                                onSuccess = onAdded
                            )
                        } else {
                            viewModel.updatePlan(
                                planId = editingPlanId!!,
                                date = date,
                                startTime = formatTime(startTimeState.hour, startTimeState.minute),
                                endTime = formatTime(endTimeState.hour, endTimeState.minute),
                                note = note,
                                onError = onError,
                                onSuccess = onUpdated
                            )
                        }

                    }
                },
                enabled = viewModel.selectedDateMillis != null && isTimeValid,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editingPlanId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    contentColor = if (editingPlanId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text(if (editingPlanId == null) "Add plan to calendar" else "Update plan")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Your free periods for the day",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (viewModel.filteredPlans.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (viewModel.selectedDateMillis == null) {
                                "Select a day on the calendar"
                            } else "No plans for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                items(viewModel.filteredPlans) { plan ->
                    val isBeingEdited = plan.id == editingPlanId

                    PlanItem(
                        plan = plan,
                        modifier = Modifier.border(
                            width = if (isBeingEdited) 2.dp else 0.dp,
                            color = if (isBeingEdited) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                        onEdit = {
                            note = plan.note
                            startTimeState.hour = unformatTime(plan.startTime).first
                            startTimeState.minute = unformatTime(plan.startTime).second
                            endTimeState.hour = unformatTime(plan.endTime).first
                            endTimeState.minute = unformatTime(plan.endTime).second
                            editingPlanId = plan.id
                        },
                        onDelete = {
                            planToDelete = plan
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("OK")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Time picker dialog
        if (showStartTimePicker || showEndTimePicker) {
            AlertDialog(
                onDismissRequest = {
                    showStartTimePicker = false
                    showEndTimePicker = false
                },
                confirmButton = {
                    TextButton(onClick = {
                        showStartTimePicker = false
                        showEndTimePicker = false

                        // Validation Toast: Triggers when the user selects an end time earlier than start time
                        if (!isTimeValid) {
                            Toast.makeText(
                                context,
                                "End time must be after start time!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Text("OK")
                    }
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

    if (showDeleteDialog && planToDelete != null) {
        AlertDialog(
            title = { Text("Delete Plan") },
            text = { Text("Are you sure you want to delete this plan?") },
            onDismissRequest = closeDeleteDialog,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlan(
                            planId = planToDelete!!.id,
                            onError = { errorMessage: String ->
                                Toast.makeText(
                                    context,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onSuccess = {
                                closeDeleteDialog()
                                Toast.makeText(
                                    context,
                                    "Plan successfully deleted!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = closeDeleteDialog) {
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

@Composable
fun PlanItem(
    plan: FuturePlan,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val duration = remember(plan.startTime, plan.endTime) {
        val durationMins = timeToMinutes(plan.endTime) - timeToMinutes(plan.startTime)
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
            IconButton(onClick = { viewModel.moveToPreviousMonth() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)) {
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

            IconButton(onClick = { viewModel.moveToNextMonth() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)) {
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
                            else -> Color.Unspecified
                        }
                    )
                }
            }
        }
    }
}

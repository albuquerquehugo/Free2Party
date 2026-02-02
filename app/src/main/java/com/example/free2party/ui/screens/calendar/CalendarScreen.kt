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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var note by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val selectedDateText = datePickerState.selectedDateMillis?.let {
        val format = DateFormat.getDateInstance()
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(it))
    } ?: "Select date"

    val startTimeState = rememberTimePickerState(initialHour = 12, initialMinute = 0)
    val endTimeState = rememberTimePickerState(initialHour = 13, initialMinute = 0)

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val plannedDays =
        viewModel.getPlannedDaysForMonth(viewModel.displayedYear, viewModel.displayedMonth)

    fun formatTime(hour: Int, minute: Int): String {
        val mm = minute.toString().padStart(2, '0')
        return "$hour:$mm"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthCalendar(viewModel = viewModel, plannedDays = plannedDays)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            "Schedule your future availability",
            style = MaterialTheme.typography.titleLarge,
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
                headlineContent = { Text("Date: $selectedDateText") },
                leadingContent = { Icon(Icons.Default.DateRange, contentDescription = null) },
                trailingContent = {
                    if (viewModel.selectedDateMillis != null) {
                        IconButton(onClick = { viewModel.clearSelectedDate() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear selection",
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
                    .height(48.dp),
                onClick = { showStartTimePicker = true }
            ) {
                ListItem(
                    headlineContent = {
                        Text("From: ${formatTime(startTimeState.hour, startTimeState.minute)}")
                    },
                    leadingContent = { Icon(Icons.Default.WatchLater, contentDescription = null) }
                )
            }

            // End time card
            OutlinedCard(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                onClick = { showEndTimePicker = true }
            ) {
                ListItem(
                    headlineContent = {
                        Text("To: ${formatTime(endTimeState.hour, endTimeState.minute)}")
                    },
                    leadingContent = { Icon(Icons.Default.WatchLater, contentDescription = null) }
                )
            }
        }

        // Note text-field
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("What are you planning?") },
            placeholder = { Text("Enter a brief note...") },
            modifier = Modifier.fillMaxWidth()
        )

        // Save button
        Button(
            onClick = {
                viewModel.selectedDateMillis?.let { timestamp ->
                    viewModel.savePlan(
                        date = timestamp,
                        startTime = formatTime(startTimeState.hour, startTimeState.minute),
                        endTime = formatTime(endTimeState.hour, endTimeState.minute),
                        note = note
                    )

                    Toast.makeText(
                        context,
                        "Plan saved to you calendar!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Clear fields and selection after saving
                    viewModel.selectedDateMillis = null
                    startTimeState.hour = 12
                    startTimeState.minute = 0
                    endTimeState.hour = 13
                    endTimeState.minute = 0
                    note = ""
                    focusManager.clearFocus()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = note.isNotBlank() && viewModel.selectedDateMillis != null
        ) {
            Text("Add to calendar")
        }

        Text(
            text = "Your upcoming plans",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.plansList) { plan ->
                PlanItem(plan = plan, onDelete = { viewModel.deletePlan(plan.id) })
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
fun PlanItem(plan: FuturePlan, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDate = remember(plan.date) {
        val format = DateFormat.getDateInstance()
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(plan.date))
    }

    Box {
        Card(
            modifier = Modifier
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = formattedDate, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${plan.startTime} - ${plan.endTime}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (plan.note.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = plan.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Delete plan") },
                onClick = {
                    onDelete()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
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

    val monthName =
        java.text.SimpleDateFormat(
            "MMMM yyyy",
            java.util.Locale.getDefault()
        ).format(calendar.time)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.moveToPreviousMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = monthName, style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = { viewModel.goToToday() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = "Today", style = MaterialTheme.typography.labelSmall)
                }
            }

            IconButton(onClick = { viewModel.moveToNextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }

        // Day of the week headers (S, M, T...)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }

        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.wrapContentHeight(),
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = 8.dp)
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
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.selectDate(day) },
                    contentAlignment = Alignment.Center
                ) {
                    // Highlight selection
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                    // Circle background for planned days
                    if (isPlanned && !isSelected) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                        )
                    }

                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }

                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.bodyMedium,
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

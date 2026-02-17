package com.example.free2party.ui.screens.calendar

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.ui.components.MonthCalendar
import com.example.free2party.ui.components.dialogs.PlanDialog
import com.example.free2party.ui.components.PlanResults
import com.example.free2party.util.isDateTimeInPast
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val context = LocalContext.current

    val currentTimeMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(5_000)
            value = System.currentTimeMillis()
        }
    }

    val plannedDays =
        viewModel.getPlannedDaysForMonth(viewModel.displayedYear, viewModel.displayedMonth)

    val (showPlanDialog, setShowPlanDialog) = remember { mutableStateOf(false) }
    val (editingPlan, setEditingPlan) = remember { mutableStateOf<FuturePlan?>(null) }

    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (planToDelete, setPlanToDelete) = remember { mutableStateOf<FuturePlan?>(null) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    val startTimeState = rememberTimePickerState(initialHour = 12, initialMinute = 0)
    val endTimeState = rememberTimePickerState(initialHour = 13, initialMinute = 0)

    val selectedDateText = startDatePickerState.selectedDateMillis?.let {
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
                plans = viewModel.filteredPlans,
                isDateSelected = viewModel.selectedDateMillis != null,
                selectedDateText = selectedDateText,
                currentTimeMillis = currentTimeMillis,
                onEdit = { plan ->
                    setEditingPlan(plan)
                    setShowPlanDialog(true)
                },
                onDelete = { plan ->
                    setPlanToDelete(plan)
                    setShowDeleteDialog(true)
                }
            )
        }
    }

    if (showPlanDialog) {
        PlanDialog(
            editingPlan = editingPlan,
            onDismiss = { setShowPlanDialog(false) },
            onConfirm = { startDate, endDate, startTime, endTime, note ->
                val onError = { errorMessage: String ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                val onSuccess = { message: String ->
                    setShowPlanDialog(false)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                if (editingPlan == null) {
                    viewModel.savePlan(
                        startDate = startDate,
                        endDate = endDate,
                        startTime = startTime,
                        endTime = endTime,
                        note = note,
                        onValidationError = onError,
                        onSuccess = { onSuccess("Plan successfully added!") }
                    )
                } else {
                    viewModel.updatePlan(
                        planId = editingPlan.id,
                        startDate = startDate,
                        endDate = endDate,
                        startTime = startTime,
                        endTime = endTime,
                        note = note,
                        onError = onError,
                        onSuccess = { onSuccess("Plan successfully updated!") }
                    )
                }
            },
            startDatePickerState = startDatePickerState,
            endDatePickerState = endDatePickerState,
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

    LaunchedEffect(viewModel.selectedDateMillis) {
        if (viewModel.selectedDateMillis != startDatePickerState.selectedDateMillis) {
            startDatePickerState.selectedDateMillis = viewModel.selectedDateMillis
        }
    }
    LaunchedEffect(startDatePickerState.selectedDateMillis) {
        if (startDatePickerState.selectedDateMillis != viewModel.selectedDateMillis) {
            viewModel.selectedDateMillis = startDatePickerState.selectedDateMillis
        }
    }
}

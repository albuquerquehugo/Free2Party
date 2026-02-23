package com.example.free2party.ui.screens.calendar

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.BuildConfig
import com.example.free2party.R
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
fun CalendarRoute(
    viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.provideFactory(
            null
        )
    )
) {
    val context = LocalContext.current
    val use24HourFormat = viewModel.use24HourFormat

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    // Recreate time picker states whenever the 24-hour format preference changes
    val startTimeState = key(use24HourFormat) {
        rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = use24HourFormat
        )
    }
    val endTimeState = key(use24HourFormat) {
        rememberTimePickerState(
            initialHour = 13,
            initialMinute = 0,
            is24Hour = use24HourFormat
        )
    }

    val plannedDays =
        viewModel.getPlannedDaysForMonth(viewModel.displayedYear, viewModel.displayedMonth)

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

    CalendarScreen(
        viewModel = viewModel,
        plannedDays = plannedDays,
        startDatePickerState = startDatePickerState,
        endDatePickerState = endDatePickerState,
        startTimeState = startTimeState,
        endTimeState = endTimeState,
        onSavePlan = { startDate, endDate, startTime, endTime, note, editingPlanId ->
            val onError = { errorMessage: String ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            val onSuccess = { message: String ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }

            if (editingPlanId == null) {
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
                    planId = editingPlanId,
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
        onDeletePlan = { planId ->
            viewModel.deletePlan(
                planId = planId,
                onError = { errorMessage: String ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                },
                onSuccess = {
                    Toast.makeText(context, "Plan deleted!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    plannedDays: Set<Int>,
    startDatePickerState: DatePickerState,
    endDatePickerState: DatePickerState,
    startTimeState: TimePickerState,
    endTimeState: TimePickerState,
    onSavePlan: (String, String, String, String, String, String?) -> Unit,
    onDeletePlan: (String) -> Unit
) {
    val use24HourFormat = viewModel.use24HourFormat

    val currentTimeMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(BuildConfig.updateFrequency)
            value = System.currentTimeMillis()
        }
    }

    val (showPlanDialog, setShowPlanDialog) = remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<FuturePlan?>(null) }
    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<FuturePlan?>(null) }

    val selectedDateText = startDatePickerState.selectedDateMillis?.let {
        val format = DateFormat.getDateInstance()
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(it))
    } ?: ""

    val isSelectedDateInPast = viewModel.selectedDateMillis?.let { isDateTimeInPast(it) } ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_light_full_transparent),
                contentDescription = "Free2Party Logo",
                modifier = Modifier.height(20.dp),
                contentScale = ContentScale.Fit
            )
        }

        MonthCalendar(viewModel = viewModel, plannedDays = plannedDays)

        IconButton(
            onClick = {
                editingPlan = null
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
            use24HourFormat = use24HourFormat,
            onEdit = { plan ->
                editingPlan = plan
                setShowPlanDialog(true)
            },
            onDelete = { plan ->
                planToDelete = plan
                setShowDeleteDialog(true)
            }
        )
    }

    if (showPlanDialog) {
        PlanDialog(
            editingPlan = editingPlan,
            use24HourFormat = use24HourFormat,
            onDismiss = { setShowPlanDialog(false) },
            onConfirm = { startDate, endDate, startTime, endTime, note ->
                onSavePlan(startDate, endDate, startTime, endTime, note, editingPlan?.id)
                setShowPlanDialog(false)
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
                        onDeletePlan(planToDelete!!.id)
                        setShowDeleteDialog(false)
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
}

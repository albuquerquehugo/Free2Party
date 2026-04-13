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
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.BuildConfig
import com.example.free2party.R
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.ui.components.MonthCalendar
import com.example.free2party.ui.components.dialogs.PlanDialog
import com.example.free2party.ui.components.PlanResults
import com.example.free2party.ui.components.dialogs.ConfirmationDialog
import com.example.free2party.util.formatPlanDateInFull
import com.example.free2party.util.isDateTimeInPast
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRoute(
    viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.provideFactory(null)
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

    val planAddedMessage = stringResource(R.string.plan_added_successfully)
    val planUpdatedMessage = stringResource(R.string.plan_updated_successfully)
    val planDeletedMessage = stringResource(R.string.plan_deleted)

    CalendarScreen(
        viewModel = viewModel,
        plannedDays = plannedDays,
        startDatePickerState = startDatePickerState,
        endDatePickerState = endDatePickerState,
        startTimeState = startTimeState,
        endTimeState = endTimeState,
        onSavePlan = { startDate, endDate, startTime, endTime, note, visibility, friendsSelection, editingPlanId ->
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
                    visibility = visibility,
                    friendsSelection = friendsSelection,
                    onValidationError = onError,
                    onSuccess = { onSuccess(planAddedMessage) }
                )
            } else {
                viewModel.updatePlan(
                    planId = editingPlanId,
                    startDate = startDate,
                    endDate = endDate,
                    startTime = startTime,
                    endTime = endTime,
                    note = note,
                    visibility = visibility,
                    friendsSelection = friendsSelection,
                    onError = onError,
                    onSuccess = { onSuccess(planUpdatedMessage) }
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
                    Toast.makeText(context, planDeletedMessage, Toast.LENGTH_SHORT).show()
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
    onSavePlan: (String, String, String, String, String, com.example.free2party.data.model.PlanVisibility, List<String>, String?) -> Unit,
    onDeletePlan: (String) -> Unit
) {
    val use24HourFormat = viewModel.use24HourFormat
    val friends by viewModel.friendsList.collectAsState()

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
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        formatPlanDateInFull(sdf.format(Date(it)))
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
                painter = painterResource(id = R.drawable.free2party_full_transparent),
                contentDescription = stringResource(R.string.logo_content_description),
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
                .padding(top = 8.dp, bottom = 16.dp)
                .background(
                    if (isSelectedDateInPast) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_plan),
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
            friends = friends,
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
            friends = friends,
            onDismiss = { setShowPlanDialog(false) },
            onConfirm = { startDate, endDate, startTime, endTime, note, visibility, friendsSelection ->
                onSavePlan(
                    startDate,
                    endDate,
                    startTime,
                    endTime,
                    note,
                    visibility,
                    friendsSelection,
                    editingPlan?.id
                )
                setShowPlanDialog(false)
            },
            startDatePickerState = startDatePickerState,
            endDatePickerState = endDatePickerState,
            startTimeState = startTimeState,
            endTimeState = endTimeState
        )
    }

    if (showDeleteDialog && planToDelete != null) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_plan),
            text = stringResource(R.string.delete_plan_confirmation_text),
            confirmButtonText = stringResource(R.string.delete),
            onConfirm = {
                onDeletePlan(planToDelete!!.id)
                setShowDeleteDialog(false)
            },
            dismissButtonText = stringResource(R.string.cancel),
            onDismiss = { setShowDeleteDialog(false) },
            isDestructive = true
        )
    }
}

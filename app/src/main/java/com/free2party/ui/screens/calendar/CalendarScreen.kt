package com.free2party.ui.screens.calendar

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.free2party.BuildConfig
import com.free2party.R
import com.free2party.data.model.FuturePlan
import com.free2party.data.model.Membership
import com.free2party.data.model.PlanVisibility
import com.free2party.ui.components.AdBanner
import com.free2party.ui.components.TopBar
import com.free2party.ui.components.MonthCalendar
import com.free2party.ui.components.dialogs.PlanDialog
import com.free2party.ui.components.CalendarResults
import com.free2party.ui.components.dialogs.AppConfirmationDialog
import com.free2party.util.formatPlanDateInFull
import com.free2party.util.isDateTimeInPast
import com.free2party.util.UiText
import com.free2party.util.parseDateToMillis
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRoute(onNavigateToEventDetails: (String) -> Unit) {
    val context = LocalContext.current
    val viewModel: CalendarViewModel = hiltViewModel()
    val startDatePickerState = rememberDatePickerState()
    val plannedDays =
        viewModel.getPlannedDaysForMonth(viewModel.displayedYear, viewModel.displayedMonth)
    val eventDays =
        viewModel.getEventDaysForMonth(viewModel.displayedYear, viewModel.displayedMonth)
    val gradientBackground = viewModel.gradientBackground

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

    val planAddedMessage = stringResource(R.string.toast_plan_added_successfully)
    val planUpdatedMessage = stringResource(R.string.toast_plan_updated_successfully)
    val planDeletedMessage = stringResource(R.string.toast_plan_deleted)

    CalendarScreen(
        viewModel = viewModel,
        plannedDays = plannedDays,
        eventDays = eventDays,
        gradientBackground = gradientBackground,
        startDatePickerState = startDatePickerState,
        onSavePlan = { startDate, endDate, startTime, endTime, note, visibility, friendsSelection, editingPlanId ->
            val onError = { errorMessage: UiText ->
                Toast.makeText(context, errorMessage.asString(context), Toast.LENGTH_LONG).show()
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
                    onSuccess = {
                        onSuccess(planAddedMessage)
                        parseDateToMillis(startDate)?.let { millis ->
                            startDatePickerState.selectedDateMillis = millis
                        }
                    }
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
                onError = { errorMessage: UiText ->
                    Toast.makeText(context, errorMessage.asString(context), Toast.LENGTH_LONG)
                        .show()
                },
                onSuccess = {
                    Toast.makeText(context, planDeletedMessage, Toast.LENGTH_SHORT).show()
                }
            )
        },
        onNavigateToEventDetails = onNavigateToEventDetails
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    plannedDays: Set<Int>,
    eventDays: Set<Int>,
    gradientBackground: Boolean,
    startDatePickerState: DatePickerState,
    onSavePlan: (String, String, String, String, String, PlanVisibility, List<String>, String?) -> Unit,
    onDeletePlan: (String) -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    val use24HourFormat = viewModel.use24HourFormat
    val friends by viewModel.friendsList.collectAsState()
    val circles by viewModel.circles.collectAsState()

    val context = LocalContext.current
    val pastPlanMsg = stringResource(R.string.error_past_plan)

    val currentTimeMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(BuildConfig.updateFrequency.milliseconds)
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
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(showBackButton = false, onBack = {})

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MonthCalendar(
                        viewModel = viewModel,
                        plannedDays = plannedDays,
                        eventDays = eventDays
                    )

                    IconButton(
                        onClick = {
                            if (isSelectedDateInPast) {
                                Toast.makeText(context, pastPlanMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                editingPlan = null
                                setShowPlanDialog(true)
                            }
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(
                                if (isSelectedDateInPast) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.label_add_plan),
                            tint = if (isSelectedDateInPast) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            } else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    CalendarResults(
                        calendarEntries = viewModel.filteredItems,
                        isDateSelected = viewModel.selectedDateMillis != null,
                        selectedDateText = selectedDateText,
                        currentTimeMillis = currentTimeMillis,
                        use24HourFormat = use24HourFormat,
                        friends = friends,
                        gradientBackground = gradientBackground,
                        onEditPlan = { entry ->
                            editingPlan = entry.plan
                            setShowPlanDialog(true)
                        },
                        onDeletePlan = { entry ->
                            planToDelete = entry.plan
                            setShowDeleteDialog(true)
                        },
                        onNavigateToEventDetails = onNavigateToEventDetails
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MonthCalendar(
                    viewModel = viewModel,
                    plannedDays = plannedDays,
                    eventDays = eventDays
                )

                IconButton(
                    onClick = {
                        if (isSelectedDateInPast) {
                            Toast.makeText(context, pastPlanMsg, Toast.LENGTH_SHORT).show()
                        } else {
                            editingPlan = null
                            setShowPlanDialog(true)
                        }
                    },
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
                        contentDescription = stringResource(R.string.label_add_plan),
                        tint = if (isSelectedDateInPast) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        } else MaterialTheme.colorScheme.onPrimary
                    )
                }

                CalendarResults(
                    calendarEntries = viewModel.filteredItems,
                    isDateSelected = viewModel.selectedDateMillis != null,
                    selectedDateText = selectedDateText,
                    currentTimeMillis = currentTimeMillis,
                    use24HourFormat = use24HourFormat,
                    friends = friends,
                    modifier = Modifier.weight(1f),
                    gradientBackground = gradientBackground,
                    onEditPlan = { entry ->
                        editingPlan = entry.plan
                        setShowPlanDialog(true)
                    },
                    onDeletePlan = { entry ->
                        planToDelete = entry.plan
                        setShowDeleteDialog(true)
                    },
                    onNavigateToEventDetails = onNavigateToEventDetails
                )
            }
        }

        if (viewModel.membership == Membership.FREE) {
            AdBanner()
        }
    }

    if (showPlanDialog) {
        PlanDialog(
            editingPlan = editingPlan,
            use24HourFormat = use24HourFormat,
            friends = friends,
            circles = circles,
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
            }
        )
    }

    if (showDeleteDialog && planToDelete != null) {
        AppConfirmationDialog(
            title = stringResource(R.string.label_delete_plan),
            text = stringResource(R.string.text_delete_plan_confirmation_text),
            confirmButtonText = stringResource(R.string.label_delete),
            onConfirm = {
                onDeletePlan(planToDelete!!.id)
                setShowDeleteDialog(false)
            },
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismissRequest = { setShowDeleteDialog(false) },
            isDestructive = true
        )
    }
}

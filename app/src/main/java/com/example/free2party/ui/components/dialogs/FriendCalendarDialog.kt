package com.example.free2party.ui.components.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.ui.components.MonthCalendar
import com.example.free2party.ui.components.PlanResults
import com.example.free2party.ui.screens.calendar.CalendarViewModel
import com.example.free2party.util.provideCalendarViewModelFactory
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

@Composable
fun FriendCalendarDialog(
    friend: FriendInfo,
    onDismiss: () -> Unit
) {
    val viewModel: CalendarViewModel = viewModel(
        key = "calendar_${friend.uid}",
        factory = provideCalendarViewModelFactory(friend.uid)
    )

    val handleDismiss = {
        viewModel.goToToday()
        onDismiss()
    }

    val currentTimeMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(5_000)
            value = System.currentTimeMillis()
        }
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.75f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${friend.name}'s Calendar",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val plannedDays = viewModel.getPlannedDaysForMonth(
                    viewModel.displayedYear,
                    viewModel.displayedMonth
                )
                MonthCalendar(viewModel = viewModel, plannedDays = plannedDays)

                Spacer(modifier = Modifier.height(32.dp))

                val selectedDateText = viewModel.selectedDateMillis?.let {
                    val format = DateFormat.getDateInstance()
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    format.format(Date(it))
                } ?: ""

                PlanResults(
                    plans = viewModel.filteredPlans,
                    isDateSelected = viewModel.selectedDateMillis != null,
                    selectedDateText = selectedDateText,
                    currentTimeMillis = currentTimeMillis,
                    onEdit = {},
                    onDelete = {},
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = handleDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

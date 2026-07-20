package com.free2party.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.data.model.FriendInfo
import com.free2party.ui.screens.calendar.CalendarEntry
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CalendarResults(
    calendarEntries: List<CalendarEntry>,
    isDateSelected: Boolean,
    selectedDateText: String,
    currentTimeMillis: Long,
    use24HourFormat: Boolean,
    friends: List<FriendInfo>,
    modifier: Modifier = Modifier,
    gradientBackground: Boolean = false,
    onEditPlan: ((CalendarEntry.Plan) -> Unit)? = null,
    onDeletePlan: ((CalendarEntry.Plan) -> Unit)? = null,
    onNavigateToEventDetails: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    var expandedItemId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.label_results_for, selectedDateText),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(top = 16.dp)
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (calendarEntries.isEmpty()) {
                item {
                    EmptyStateMessage(isDateSelected)
                }
            } else {
                items(calendarEntries, key = { it.id }) { entry ->
                    val isExpanded = expandedItemId == entry.id

                    when (entry) {
                        is CalendarEntry.Plan -> {
                            PlanItem(
                                plan = entry.plan,
                                use24HourFormat = use24HourFormat,
                                currentTimeMillis = currentTimeMillis,
                                friends = friends,
                                gradientBackground = gradientBackground,
                                onEdit = onEditPlan?.let { { it(entry) } },
                                onDelete = onDeletePlan?.let { { it(entry) } },
                                isExpandedExternally = isExpanded,
                                onExpandChange = { expanded: Boolean ->
                                    expandedItemId = if (expanded) entry.id else null
                                }
                            )
                        }
                        is CalendarEntry.EventItem -> {
                            EventItem(
                                event = entry.event,
                                use24HourFormat = use24HourFormat,
                                currentTimeMillis = currentTimeMillis,
                                gradientBackground = gradientBackground,
                                onClick = { onNavigateToEventDetails?.invoke(entry.event.id) },
                                isExpandedExternally = isExpanded,
                                onExpandChange = { expanded: Boolean ->
                                    expandedItemId = if (expanded) entry.id else null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Scroll to the expanded item to ensure it's visible
    LaunchedEffect(expandedItemId) {
        if (expandedItemId != null) {
            val index = calendarEntries.indexOfFirst { it.id == expandedItemId }
            if (index >= 0) {
                // Small delay to allow the item to finish its expansion animation before scrolling
                delay(100.milliseconds)
                listState.animateScrollToItem(index)
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(isDateSelected: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text =
                if (isDateSelected) stringResource(R.string.text_no_activities_for_day)
                else stringResource(R.string.text_select_day_on_calendar),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

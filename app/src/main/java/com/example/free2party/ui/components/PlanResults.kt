package com.example.free2party.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.model.FuturePlan
import kotlinx.coroutines.delay

@Composable
fun PlanResults(
    plans: List<FuturePlan>,
    isDateSelected: Boolean,
    selectedDateText: String,
    currentTimeMillis: Long,
    use24HourFormat: Boolean,
    friends: List<FriendInfo>,
    modifier: Modifier = Modifier,
    onEdit: ((FuturePlan) -> Unit)? = null,
    onDelete: ((FuturePlan) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    var expandedPlanId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Results for: $selectedDateText",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (plans.isEmpty()) {
                item {
                    EmptyStateMessage(isDateSelected)
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    val isExpanded = expandedPlanId == plan.id
                    
                    PlanItem(
                        plan = plan,
                        use24HourFormat = use24HourFormat,
                        currentTimeMillis = currentTimeMillis,
                        friends = friends,
                        onEdit = onEdit?.let { { it(plan) } },
                        onDelete = onDelete?.let { { it(plan) } },
                        isExpandedExternally = isExpanded,
                        onExpandChange = { expanded: Boolean ->
                            expandedPlanId = if (expanded) plan.id else null
                        }
                    )
                }
            }
        }
    }

    // Scroll to the expanded item to ensure it's visible
    LaunchedEffect(expandedPlanId) {
        if (expandedPlanId != null) {
            val index = plans.indexOfFirst { it.id == expandedPlanId }
            if (index >= 0) {
                // Small delay to allow the item to finish its expansion animation before scrolling
                delay(100)
                listState.animateScrollToItem(index)
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(isDateSelected: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text =
                if (isDateSelected) "No plans for this day"
                else "Select a day on the calendar to see plans",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

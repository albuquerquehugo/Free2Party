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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.free2party.R
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
    gradientBackground: Boolean = false,
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
            text = stringResource(R.string.results_for, selectedDateText),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
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
                        gradientBackground = gradientBackground,
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
                if (isDateSelected) stringResource(R.string.no_plans_for_day)
                else stringResource(R.string.select_day_on_calendar),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

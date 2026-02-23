package com.example.free2party.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.free2party.data.model.FuturePlan

@Composable
fun PlanResults(
    plans: List<FuturePlan>,
    isDateSelected: Boolean,
    selectedDateText: String,
    currentTimeMillis: Long,
    use24HourFormat: Boolean,
    onEdit: (FuturePlan) -> Unit,
    onDelete: (FuturePlan) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Results for: $selectedDateText",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.padding(top = 24.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (plans.isEmpty()) {
                item {
                    EmptyStateMessage(isDateSelected)
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    PlanItem(
                        plan = plan,
                        use24HourFormat = use24HourFormat,
                        currentTimeMillis = currentTimeMillis,
                        onEdit = { onEdit(plan) },
                        onDelete = { onDelete(plan) }
                    )
                }
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

package com.free2party.ui.components.dialogs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.components.basic.AppFilledButton
import com.free2party.ui.components.basic.AppTextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    state: DatePickerState,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val originalDate = remember { state.selectedDateMillis }
    val handleDismiss = {
        state.selectedDateMillis = originalDate
        onDismiss()
    }

    DatePickerDialog(
        onDismissRequest = handleDismiss,
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        confirmButton = {
            AppFilledButton(
                modifier = Modifier.padding(bottom = 16.dp, end = 16.dp),
                onClick = onConfirm,
                enabled = state.selectedDateMillis != null
            ) {
                Text(stringResource(R.string.label_ok))
            }
        },
        dismissButton = {
            AppTextButton(onClick = handleDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        }
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }
}

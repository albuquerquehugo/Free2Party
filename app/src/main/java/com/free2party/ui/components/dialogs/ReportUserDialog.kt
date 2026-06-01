package com.free2party.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.free2party.util.TextFieldRegistry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.free2party.R

@Composable
fun ReportUserDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val key = remember { Any() }
    DisposableEffect(key) {
        onDispose {
            TextFieldRegistry.unregister(key)
        }
    }

    val focusManager = LocalFocusManager.current
    val options = listOf(
        stringResource(R.string.label_report_reason_spam),
        stringResource(R.string.label_report_reason_harassment),
        stringResource(R.string.label_report_reason_inappropriate_content),
        stringResource(R.string.label_report_reason_impersonation),
        stringResource(R.string.label_report_reason_other)
    )
    var selectedOption by remember { mutableStateOf(options[0]) }
    var otherReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.title_report_user)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.text_report_user_reason),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                options.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (text == selectedOption),
                                onClick = {
                                    focusManager.clearFocus()
                                    selectedOption = text
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == selectedOption),
                            onClick = null // Selected by Row's selectable
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                if (selectedOption == options.last()) {
                    OutlinedTextField(
                        value = otherReason,
                        onValueChange = { otherReason = it },
                        placeholder = { Text(stringResource(R.string.placeholder_report_reason_other)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .onGloballyPositioned { coordinates ->
                                TextFieldRegistry.register(key, coordinates)
                            },
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }
        },
        confirmButton = {
            val isOtherSelected = selectedOption == options.last()
            val isEnabled = !isOtherSelected || otherReason.isNotBlank()

            TextButton(
                onClick = {
                    val finalReason = if (isOtherSelected) otherReason else selectedOption
                    onReport(finalReason)
                },
                enabled = isEnabled
            ) {
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

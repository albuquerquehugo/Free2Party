package com.free2party.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.components.basic.AppFilledButton

@Composable
fun AppConfirmationDialog(
    title: String,
    text: String,
    confirmButtonText: String = stringResource(R.string.label_confirm),
    onConfirm: () -> Unit,
    dismissButtonText: String = stringResource(R.string.label_cancel),
    onDismissRequest: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    AppBaseDialog(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            val hasSecondary = secondaryButtonText != null && onSecondaryAction != null

            if (hasSecondary) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = dismissButtonText, color =
                                if (isDestructive) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                        )
                    }

                    AppFilledButton(
                        onClick = onSecondaryAction,
                        colors =
                            if (isDestructive) {
                                ButtonDefaults.textButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            } else {
                                ButtonDefaults.textButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                    ) {
                        Text(secondaryButtonText)
                    }

                    AppFilledButton(
                        onClick = onConfirm,
                        colors =
                            if (isDestructive) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                    ) {
                        Text(confirmButtonText)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = dismissButtonText, color =
                                if (isDestructive) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    AppFilledButton(
                        onClick = onConfirm,
                        colors =
                            if (isDestructive) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                    ) {
                        Text(text = confirmButtonText)
                    }
                }
            }
        }
    }
}

package com.example.free2party.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.free2party.ui.components.InputTextField
import com.example.free2party.ui.screens.home.InviteFriendUiState

@Composable
fun InviteFriendDialog(
    uiState: InviteFriendUiState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onResetState: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            onResetState()
            onDismiss()
        },
        title = { Text(text = "Invite Friend", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "Enter your friend's email to send an invite.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                InputTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (uiState is InviteFriendUiState.Error) onResetState()
                    },
                    label = "Email",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is InviteFriendUiState.Searching,
                    isError = uiState is InviteFriendUiState.Error,
                    supportingText = {
                        if (uiState is InviteFriendUiState.Error) {
                            Text(text = uiState.message)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (email.isNotBlank() && uiState !is InviteFriendUiState.Searching) {
                                onConfirm(email)
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email) },
                enabled = email.isNotBlank() && uiState !is InviteFriendUiState.Searching
            ) {
                if (uiState is InviteFriendUiState.Searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send invite")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onResetState()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.example.free2party.ui.screens.home.FriendUiEvent
import com.example.free2party.ui.screens.home.FriendViewModel
import com.example.free2party.ui.screens.home.InviteFriendUiState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun InviteFriendDialog(
    viewModel: FriendViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is FriendUiEvent.InviteSentSuccessfully -> {
                    Toast.makeText(
                        context,
                        "Invite successfully sent to ${event.email}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.resetState()
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

                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.uiState !is InviteFriendUiState.Searching,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.inviteFriend() }
                    )
                )

                if (viewModel.uiState is InviteFriendUiState.Error) {
                    Text(
                        text = (viewModel.uiState as InviteFriendUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.inviteFriend() },
                enabled = viewModel.uiState !is InviteFriendUiState.Searching
            ) {
                if (viewModel.uiState is InviteFriendUiState.Searching) {
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
                viewModel.resetState()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

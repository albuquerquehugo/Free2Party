package com.example.free2party.ui.components.dialogs

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.free2party.ui.components.InputTextField

@Composable
fun EmailDialog(
    title: String,
    description: String,
    inputValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    confirmButtonLabel: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isEmailValid by remember(inputValue) {
        derivedStateOf {
            Patterns.EMAIL_ADDRESS.matcher(inputValue).matches()
        }
    }

    val finalErrorMessage = remember(errorMessage, isEmailValid, inputValue) {
        errorMessage ?: if (inputValue.isNotEmpty() && !isEmailValid) {
            "Please enter a valid email address."
        } else {
            null
        }
    }

    BaseDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            InputTextField(
                value = inputValue,
                onValueChange = onValueChange,
                label = "Email",
                icon = Icons.Default.Email,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = finalErrorMessage != null,
                supportingText = finalErrorMessage?.let {
                    { Text(text = it) }
                },
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    enabled = inputValue.isNotBlank() && !isLoading && isEmailValid
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(confirmButtonLabel)
                    }
                }
            }
        }
    }
}

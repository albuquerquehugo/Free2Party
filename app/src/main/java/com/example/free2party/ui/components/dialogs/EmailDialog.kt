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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.free2party.R
import com.example.free2party.ui.components.InputTextField

@Composable
fun EmailDialog(
    title: String,
    description: String,
    email: String,
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
    keyboardActions: KeyboardActions? = null
) {
    val isEmailValid by remember(email) {
        derivedStateOf {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
    }

    val invalidEmailMessage = stringResource(R.string.error_invalid_email)
    val finalErrorMessage = remember(errorMessage, isEmailValid, email, invalidEmailMessage) {
        errorMessage ?: if (email.isNotEmpty() && !isEmailValid) {
            invalidEmailMessage
        } else {
            null
        }
    }

    val canConfirm = email.isNotBlank() && !isLoading && isEmailValid

    val finalKeyboardActions = keyboardActions ?: KeyboardActions(
        onDone = {
            if (canConfirm) {
                onConfirm()
            }
        }
    )

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
                value = email.trim(),
                onValueChange = onValueChange,
                label = stringResource(R.string.email_label),
                icon = Icons.Default.Email,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = finalErrorMessage != null,
                supportingText = finalErrorMessage?.let {
                    { Text(text = it) }
                },
                keyboardOptions = keyboardOptions,
                keyboardActions = finalKeyboardActions
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
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    enabled = canConfirm
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

package com.example.free2party.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.free2party.R

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmButtonText: String = stringResource(R.string.confirm),
    onConfirm: () -> Unit,
    dismissButtonText: String = stringResource(R.string.cancel),
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    BaseDialog(onDismissRequest = onDismiss) {
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(dismissButtonText)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = if (isDestructive) {
                        MaterialTheme.colorScheme.run {
                            androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = error,
                                contentColor = onError
                            )
                        }
                    } else {
                        androidx.compose.material3.ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(confirmButtonText)
                }
            }
        }
    }
}

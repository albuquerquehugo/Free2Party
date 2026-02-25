package com.example.free2party.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun InputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    icon: ImageVector? = null,
    painter: Painter? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    changeVisibility: () -> Unit = {},
    enabled: Boolean = true,
    isError: Boolean = false,
    showClearIcon: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isMultiLine = maxLines > 1

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = placeholder?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (isMultiLine) Modifier.height(IntrinsicSize.Min) else Modifier),
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        interactionSource = interactionSource,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            visualTransformation
        },
        leadingIcon = if (icon != null || painter != null) {
            {
                Box(
                    modifier = if (isMultiLine) Modifier.fillMaxHeight() else Modifier,
                    contentAlignment = if (isMultiLine) Alignment.TopCenter else Alignment.Center
                ) {
                    Box(modifier = Modifier.padding(top = if (isMultiLine) 16.dp else 0.dp)) {
                        if (painter != null) {
                            Icon(
                                painter,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (icon != null) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        } else null,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showClearIcon && value.isNotEmpty() && isFocused) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (isPassword) {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = changeVisibility) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }

                trailingIcon?.invoke()
            }
        },
        singleLine = !isMultiLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

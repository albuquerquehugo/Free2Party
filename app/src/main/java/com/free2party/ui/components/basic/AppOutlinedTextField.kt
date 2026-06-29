package com.free2party.ui.components.basic

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.free2party.util.TextFieldRegistry
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun appOutlinedTextFieldColors(
    focusedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor: Color = Color.Transparent,
    unfocusedContainerColor: Color = Color.Transparent,
    focusedBorderColor: Color = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor: Color = MaterialTheme.colorScheme.outline,
    focusedLabelColor: Color = MaterialTheme.colorScheme.outline,
    unfocusedLabelColor: Color = MaterialTheme.colorScheme.outline,
    focusedPlaceholderColor: Color = MaterialTheme.colorScheme.outline,
    unfocusedPlaceholderColor: Color = MaterialTheme.colorScheme.outline,
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = focusedTextColor,
    unfocusedTextColor = unfocusedTextColor,
    focusedContainerColor = focusedContainerColor,
    unfocusedContainerColor = unfocusedContainerColor,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedLabelColor = focusedLabelColor,
    unfocusedLabelColor = unfocusedLabelColor,
    focusedPlaceholderColor = focusedPlaceholderColor,
    unfocusedPlaceholderColor = unfocusedPlaceholderColor
)

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    changeVisibility: () -> Unit = {},
    labelText: String? = null,
    placeholderText: String? = null,
    placeholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    icon: ImageVector? = null,
    painter: Painter? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    leadingIconExtra: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    showClearIcon: Boolean = true,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors? = null,
    onClear: (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val key = remember { Any() }
    DisposableEffect(key) {
        onDispose {
            TextFieldRegistry.unregister(key)
        }
    }

    val finalSingleLine = if (maxLines > 1) false else singleLine
    val isMultiLine = maxLines > 1
    val isFocused by interactionSource.collectIsFocusedAsState()
    val clearButtonInteractionSource = remember { MutableInteractionSource() }
    val isClearButtonPressed by clearButtonInteractionSource.collectIsPressedAsState()

    var isFocusedBuffered by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            isFocusedBuffered = true
        } else {
            delay(150.milliseconds)
            isFocusedBuffered = false
        }
    }

    val resolvedLabelColor = if (value.isEmpty() && !isFocused) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else {
        Color.Unspecified
    }

    val defaultColors = appOutlinedTextFieldColors(
        unfocusedLabelColor = resolvedLabelColor
    )

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val finalKeyboardActions = if (keyboardOptions.imeAction == ImeAction.Done) {
        KeyboardActions(onDone = {
            keyboardController?.hide()
        })
    } else {
        keyboardActions
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(100.milliseconds)
            keyboardController?.show()
            delay(100.milliseconds)
            bringIntoViewRequester.bringIntoView(rect = Rect(0f, 0f, 0f, 200f))
        }
    }

    val finalLabel: @Composable (() -> Unit)? = labelText?.let { text ->
        { Text(text, style = MaterialTheme.typography.bodyMedium) }
    }

    val finalPlaceholder: @Composable (() -> Unit)? = placeholderText?.let { text ->
        {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = placeholderColor
            )
        }
    }

    val finalLeadingIcon: @Composable (() -> Unit)? =
        leadingIcon ?: if (icon != null || painter != null || leadingIconExtra != null) {
            {
                Row(
                    modifier = if (isMultiLine) Modifier.fillMaxHeight() else Modifier,
                    verticalAlignment = if (isMultiLine) Alignment.Top else Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (icon != null || painter != null) {
                        Box(
                            modifier = Modifier.padding(
                                top = if (isMultiLine) 16.dp else 0.dp,
                                start = 12.dp,
                                end = if (leadingIconExtra != null) 0.dp else 12.dp
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (painter != null) {
                                Icon(
                                    painter,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (icon != null) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    leadingIconExtra?.invoke()
                }
            }
        } else null

    val hasClearIcon =
        showClearIcon && (isFocusedBuffered || isClearButtonPressed) && (value.isNotEmpty() || onClear != null)
    val hasPasswordIcon = isPassword
    val hasCustomTrailingIcon = trailingIcon != null

    val finalTrailingIcon: @Composable (() -> Unit)? =
        if (hasClearIcon || hasPasswordIcon || hasCustomTrailingIcon) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasClearIcon) {
                        IconButton(
                            onClick = { onClear?.invoke() ?: onValueChange("") },
                            modifier = Modifier.focusProperties { canFocus = false },
                            interactionSource = clearButtonInteractionSource
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (hasPasswordIcon) {
                        val image =
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = changeVisibility) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    }

                    trailingIcon?.invoke()
                }
            }
        } else null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .bringIntoViewRequester(bringIntoViewRequester)
            .then(if (isMultiLine) Modifier.height(IntrinsicSize.Min) else Modifier)
            .onGloballyPositioned { coordinates ->
                TextFieldRegistry.register(key, coordinates)
            },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = finalLabel,
        placeholder = finalPlaceholder,
        leadingIcon = finalLeadingIcon,
        trailingIcon = finalTrailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            visualTransformation
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = finalKeyboardActions,
        singleLine = finalSingleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors ?: defaultColors
    )
}

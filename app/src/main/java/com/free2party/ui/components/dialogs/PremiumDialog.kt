package com.free2party.ui.components.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.free2party.R

@Composable
fun PremiumDialog(
    title: String = "",
    text: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppConfirmationDialog(
        title = title,
        text = text,
        confirmButtonText = stringResource(R.string.label_subscribe),
        dismissButtonText = stringResource(R.string.label_cancel),
        onConfirm = onConfirm,
        onDismissRequest = onDismiss
    )
}

package com.example.free2party.ui.components.dialogs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        }
                    }
                    .focusable()
            ) {
                content()
            }
        }
    }
}

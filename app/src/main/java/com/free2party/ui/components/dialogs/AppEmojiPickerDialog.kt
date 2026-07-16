package com.free2party.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import android.view.ContextThemeWrapper
import androidx.compose.ui.graphics.luminance
import com.free2party.R

@Composable
fun AppEmojiPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BaseDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.text_choose_emoji),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val themeResId =
                    if (isDark) R.style.Theme_CustomEmojiPicker_Dark else R.style.Theme_CustomEmojiPicker_Light

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    factory = { context ->
                        val themedContext = ContextThemeWrapper(context, themeResId)
                        EmojiPickerView(themedContext).apply {
                            setOnEmojiPickedListener { emojiViewItem ->
                                onEmojiSelected(emojiViewItem.emoji)
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

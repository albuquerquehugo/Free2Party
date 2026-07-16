package com.free2party.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.free2party.R
import androidx.core.graphics.toColorInt

@Composable
fun AppColorPickerDialog(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse initial color
    val parsedInitialColor = remember(initialColorHex) {
        if (initialColorHex.isNotBlank() && initialColorHex.startsWith("#")) {
            try {
                Color(initialColorHex.toColorInt())
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Convert parsed initial color to HSV
    val initialHsv = remember(parsedInitialColor) {
        val hsv = FloatArray(3)
        if (parsedInitialColor != null) {
            android.graphics.Color.RGBToHSV(
                (parsedInitialColor.red * 255).toInt(),
                (parsedInitialColor.green * 255).toInt(),
                (parsedInitialColor.blue * 255).toInt(),
                hsv
            )
        } else {
            // Default to green (available color) HSV
            // Green is around 120 hue, 1f saturation, 0.8f value
            hsv[0] = 120f
            hsv[1] = 1.0f
            hsv[2] = 0.8f
        }
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }

    // Calculated selected color
    val selectedColor = remember(hue, saturation, brightness) {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
        Color(rgb)
    }

    val isForbidden = remember(selectedColor) {
        isTooSimilarToBusyColor(selectedColor)
    }

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
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.text_choose_status_color),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Color Preview
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isForbidden) {
                        Text(
                            text = "❌",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Hue Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.label_hue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Red,
                                        Color.Yellow,
                                        Color.Green,
                                        Color.Cyan,
                                        Color.Blue,
                                        Color.Magenta,
                                        Color.Red
                                    )
                                )
                            )
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Saturation Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.label_saturation),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White,
                                        Color(
                                            android.graphics.Color.HSVToColor(
                                                floatArrayOf(
                                                    hue,
                                                    1f,
                                                    1f
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                    )
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Brightness Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.label_brightness),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Black,
                                        Color(
                                            android.graphics.Color.HSVToColor(
                                                floatArrayOf(
                                                    hue,
                                                    saturation,
                                                    1f
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                    )
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Forbidden message
                if (isForbidden) {
                    Text(
                        text = stringResource(R.string.error_red_color_forbidden),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.label_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val hexString = String.format(
                                "#%06X",
                                0xFFFFFF and android.graphics.Color.HSVToColor(
                                    floatArrayOf(
                                        hue,
                                        saturation,
                                        brightness
                                    )
                                )
                            )
                            onColorSelected(hexString)
                        },
                        enabled = !isForbidden
                    ) {
                        Text(stringResource(R.string.label_select))
                    }
                }
            }
        }
    }
}

private fun isTooSimilarToBusyColor(color: Color): Boolean {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    val hue = hsv[0]
    val saturation = hsv[1]
    val value = hsv[2]

    // Red hue is typically 0-20 degrees or 340-360 degrees.
    // If it's in this range, and has a reasonable saturation (> 0.4f) and value/brightness (> 0.3f),
    // then it's "too similar to red" (busy color).
    val isRedHue = hue !in 20f..340f
    val isSaturatedAndBright = saturation > 0.4f && value > 0.3f
    return isRedHue && isSaturatedAndBright
}

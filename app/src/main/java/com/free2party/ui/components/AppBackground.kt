package com.free2party.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.free2party.ui.theme.*

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    themeName: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val baseColor = if (isDark) Color.Black else MaterialTheme.colorScheme.surface

    val themeColors = GradientThemes.getThemeColors(themeName)
    val color1 = themeColors[0]
    val color2 = themeColors[1]
    val color3 = themeColors[2]
    val color4 = themeColors[3]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
            .then(
                if (enabled) {
                    Modifier.drawBehind {
                        val slopeStop = if (isDark) 0.4f else 0.8f
                        val glowAlpha = if (isDark) 0.3f else 0.6f

                        // Top Left
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to color1.copy(alpha = glowAlpha),
                                slopeStop to Color.Transparent,
                                center = Offset(size.width * 0.2f, size.height * 0.3f),
                                radius = size.width * 1.4f
                            ),
                            radius = size.width * 1.4f,
                            center = Offset(size.width * 0.2f, size.height * 0.3f)
                        )

                        // Top Right
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to color2.copy(alpha = glowAlpha),
                                slopeStop to Color.Transparent,
                                center = Offset(size.width * 0.9f, size.height * 0.1f),
                                radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f,
                            center = Offset(size.width * 0.9f, size.height * 0.1f)
                        )

                        // Bottom Left
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to color3.copy(alpha = glowAlpha),
                                slopeStop to Color.Transparent,
                                center = Offset(size.width * 0.1f, size.height * 0.9f),
                                radius = size.width * 1.4f
                            ),
                            radius = size.width * 1.4f,
                            center = Offset(size.width * 0.1f, size.height * 0.9f)
                        )

                        // Bottom Right
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to color4.copy(alpha = glowAlpha),
                                slopeStop to Color.Transparent,
                                center = Offset(size.width * 0.8f, size.height * 0.6f),
                                radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f,
                            center = Offset(size.width * 0.8f, size.height * 0.6f)
                        )
                    }
                } else Modifier
            )
    ) {
        content()
    }
}

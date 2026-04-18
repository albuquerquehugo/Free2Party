package com.example.free2party.ui.components

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
import com.example.free2party.ui.theme.Blue70
import com.example.free2party.ui.theme.Green70
import com.example.free2party.ui.theme.Purple70
import com.example.free2party.ui.theme.Red70

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f

    val baseColor = if (isDark) Color.Black else colorScheme.surface

    if (!enabled) {
        Box(modifier = modifier
            .fillMaxSize()
            .background(baseColor)) { content() }
        return
    }

    val color1 = Blue70
    val color2 = Green70
    val color3 = Purple70
    val color4 = Red70

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
            .drawBehind {
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
    ) {
        content()
    }
}

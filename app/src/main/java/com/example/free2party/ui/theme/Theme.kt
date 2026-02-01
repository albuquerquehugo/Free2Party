package com.example.free2party.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    error = Red80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    error = Red40
)

/* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.available: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Green40 else Green60

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.availableContainer: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Green80 else Green95

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.busy: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Gray40 else Gray60

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.busyContainer: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Gray80 else Gray95

@Composable
fun Free2PartyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

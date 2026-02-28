package com.example.free2party.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.free2party.data.model.ThemeMode

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

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.available: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Green40 else Green60

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.availableContainer: Color
    @Composable
    get() = Blue90

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.onAvailableContainer: Color
    @Composable
    get() = Blue30

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.busy: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Red40 else Red60

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.busyContainer: Color
    @Composable
    get() = Red90

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.onBusyContainer: Color
    @Composable
    get() = Red40

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.inactive: Color
    @Composable
    get() = Gray70

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.inactiveContainer: Color
    @Composable
    get() = Gray90

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.onInactiveContainer: Color
    @Composable
    get() = Gray50

@get:Suppress("UnusedReceiverParameter")
val ColorScheme.userText: Color
    @Composable
    get() = Gray10

@Composable
fun Free2PartyTheme(
    themeMode: ThemeMode = ThemeMode.AUTOMATIC,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.AUTOMATIC -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.free2party.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Blue70,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = Purple80,
    onSecondary = Purple20,
    secondaryContainer = Purple30,
    onSecondaryContainer = Purple90,
    tertiary = Red80,
    onTertiary = Red20,
    tertiaryContainer = Red30,
    onTertiaryContainer = Red90,
    error = Red70,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Gray10,
    onBackground = Gray90,
    surface = Gray15,
    onSurface = Gray90,
    surfaceVariant = Gray20,
    onSurfaceVariant = Gray80,
    outline = Gray60,
    outlineVariant = Gray30,
    scrim = Color.Black,
    inverseSurface = Gray90,
    inverseOnSurface = Gray10,
    inversePrimary = Blue40
)

private val LightColorScheme = lightColorScheme(
    primary = Blue30,
    onPrimary = Color.White,
    primaryContainer = Blue85,
    onPrimaryContainer = Blue30,
    secondary = Purple40,
    onSecondary = Color.White,
    secondaryContainer = Purple90,
    onSecondaryContainer = Purple10,
    tertiary = Red30,
    onTertiary = Color.White,
    tertiaryContainer = Red85,
    onTertiaryContainer = Red30,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Blue95,
    onBackground = Gray10,
    surface = Gray95,
    onSurface = Gray10,
    surfaceVariant = Gray90,
    onSurfaceVariant = Gray30,
    outline = Gray50,
    outlineVariant = Gray80,
    scrim = Color.Black,
    inverseSurface = Gray20,
    inverseOnSurface = Gray95,
    inversePrimary = Blue80
)

// Helper to detect if the resolved color scheme is dark or light
private val ColorScheme.isDark: Boolean
    get() = surface.luminance() < 0.5f

// Custom Extension properties for your app-specific logic
val ColorScheme.available: Color
    @Composable
    get() = if (isDark) Green65 else Green60

val ColorScheme.availableContainer: Color
    @Composable
    get() = if (isDark) Blue20 else Blue80

val ColorScheme.onAvailableContainer: Color
    @Composable
    get() = if (isDark) Blue80 else Blue30

val ColorScheme.busy: Color
    @Composable
    get() = if (isDark) Red65 else Red60

val ColorScheme.busyContainer: Color
    @Composable
    get() = if (isDark) Red20 else Red80

val ColorScheme.onBusyContainer: Color
    @Composable
    get() = if (isDark) Red80 else Red40

val ColorScheme.inactive: Color
    @Composable
    get() = if (isDark) Gray50 else Gray70

val ColorScheme.inactiveContainer: Color
    @Composable
    get() = if (isDark) Gray20 else Gray90

val ColorScheme.onInactiveContainer: Color
    @Composable
    get() = if (isDark) Gray70 else Gray50

@Composable
fun Free2PartyTheme(
    themeMode: ThemeMode = ThemeMode.AUTOMATIC,
    dynamicColor: Boolean = false,
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

package com.free2party.ui.theme

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
import com.free2party.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Blue70,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue85,
    inversePrimary = Blue40,
    secondary = Purple80,
    onSecondary = Purple20,
    secondaryContainer = Purple50,
    onSecondaryContainer = Purple90,
    tertiary = Red80,
    onTertiary = Red20,
    tertiaryContainer = Red15,
    onTertiaryContainer = Red80,
    background = Color.Black,
    onBackground = Color.White,
    surface = Gray10,
    onSurface = Gray90,
    surfaceVariant = Gray20,
    onSurfaceVariant = Gray80,
    inverseSurface = Gray90,
    inverseOnSurface = Gray10,
    error = Red40,
    onError = Red5,
    errorContainer = Red35,
    onErrorContainer = Red80,
    outline = Gray60,
    outlineVariant = Gray30,
    scrim = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Blue30,
    onPrimary = Blue85,
    primaryContainer = Blue65,
    onPrimaryContainer = Blue20,
    inversePrimary = Blue80,
    secondary = Purple40,
    onSecondary = Purple85,
    secondaryContainer = Purple65,
    onSecondaryContainer = Purple20,
    tertiary = Red30,
    onTertiary = Red80,
    tertiaryContainer = Red80,
    onTertiaryContainer = Red30,
    background = Color.White,
    onBackground = Color.Black,
    surface = Gray95,
    onSurface = Gray10,
    surfaceVariant = Gray90,
    onSurfaceVariant = Gray20,
    inverseSurface = Gray10,
    inverseOnSurface = Gray95,
    error = Red45,
    onError = Red90,
    errorContainer = Red80,
    onErrorContainer = Red40,
    outline = Gray30,
    outlineVariant = Gray60,
    scrim = Color.Black
)

// Helper to detect if the resolved color scheme is dark or light
private val ColorScheme.isDark: Boolean
    get() = surface.luminance() < 0.5f

// Custom Extension properties for your app-specific logic
val ColorScheme.planContainer: Color
    @Composable
    get() = if (isDark) Blue40 else Blue70

val ColorScheme.onPlanContainer: Color
    @Composable
    get() = if (isDark) Blue85 else Blue30

val ColorScheme.eventContainer: Color
    @Composable
    get() = if (isDark) Purple40 else Purple75

val ColorScheme.onEventContainer: Color
    @Composable
    get() = if (isDark) Purple85 else Purple30

val ColorScheme.currentActivityContainer: Color
    @Composable
    get() = if (isDark) Green35 else Green70

val ColorScheme.onCurrentActivityContainer: Color
    @Composable
    get() = if (isDark) Green85 else Green30

val ColorScheme.available: Color
    @Composable
    get() = if (isDark) Green60 else Green50

val ColorScheme.availableContainer: Color
    @Composable
    get() = if (isDark) Green20 else Green75

val ColorScheme.onAvailableContainer: Color
    @Composable
    get() = if (isDark) Green80 else Green35

val ColorScheme.busy: Color
    @Composable
    get() = if (isDark) Red65 else Red50

val ColorScheme.busyContainer: Color
    @Composable
    get() = if (isDark) Red20 else Red80

val ColorScheme.onBusyContainer: Color
    @Composable
    get() = if (isDark) Red80 else Red40

val ColorScheme.pendingContainer: Color
    @Composable
    get() = if (isDark) Blue20 else Blue70

val ColorScheme.onPendingContainer: Color
    @Composable
    get() = if (isDark) Blue80 else Blue30

val ColorScheme.inactive: Color
    @Composable
    get() = if (isDark) Gray50 else Gray70

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

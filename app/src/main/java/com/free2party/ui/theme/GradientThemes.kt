package com.free2party.ui.theme

import androidx.compose.ui.graphics.Color

object GradientThemes {
    // Default Theme (Dark Mode)
    val DEFAULT =
        listOf(ThemeDefaultColor1, ThemeDefaultColor2, ThemeDefaultColor3, ThemeDefaultColor4)

    // Sunset Glow theme colors
    val SUNSET = listOf(ThemeSunsetColor1, ThemeSunsetColor2, ThemeSunsetColor3, ThemeSunsetColor4)

    // Ocean Breeze theme colors
    val OCEAN = listOf(ThemeOceanColor1, ThemeOceanColor2, ThemeOceanColor3, ThemeOceanColor4)

    // Deep Forest theme colors
    val FOREST = listOf(ThemeForestColor1, ThemeForestColor2, ThemeForestColor3, ThemeForestColor4)

    // Neon Lights theme colors
    val NEON = listOf(ThemeNeonColor1, ThemeNeonColor2, ThemeNeonColor3, ThemeNeonColor4)

    // Midnight Mystery theme colors
    val MIDNIGHT =
        listOf(ThemeMidnightColor1, ThemeMidnightColor2, ThemeMidnightColor3, ThemeMidnightColor4)

    /**
     * Returns the 4 colors to draw the background glow circles for the given theme.
     */
    fun getThemeColors(themeName: String): List<Color> {
        return when (themeName) {
            "SUNSET" -> SUNSET
            "OCEAN" -> OCEAN
            "FOREST" -> FOREST
            "NEON" -> NEON
            "MIDNIGHT" -> MIDNIGHT
            else -> DEFAULT
        }
    }
}

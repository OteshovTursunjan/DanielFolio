package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(raw: String?): ThemeMode =
            entries.firstOrNull { it.key == raw } ?: SYSTEM
    }
}

val LocalStationeryPalette = staticCompositionLocalOf { LightStationeryPalette }

private val FolioLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TerracottaOnPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TerracottaOnContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageContainer,
    onSecondaryContainer = SageOnContainer,
    tertiary = OchreTertiary,
    onTertiary = OchreOnTertiary,
    tertiaryContainer = OchreContainer,
    onTertiaryContainer = OchreOnContainer,
    background = PaperBackground,
    onBackground = InkPrimary,
    surface = PaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperSurfaceContainerLow,
    surfaceContainer = PaperSurfaceContainer,
    surfaceContainerHigh = PaperSurfaceContainerHigh,
    outline = InkOutline,
    outlineVariant = InkOutlineVariant,
    error = ErrorRedLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val FolioDarkColorScheme = darkColorScheme(
    primary = TerracottaPrimaryDark,
    onPrimary = TerracottaOnPrimaryDark,
    primaryContainer = TerracottaContainerDark,
    onPrimaryContainer = TerracottaOnContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = SageOnSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = SageOnContainerDark,
    tertiary = OchreTertiaryDark,
    onTertiary = OchreOnTertiaryDark,
    tertiaryContainer = OchreContainerDark,
    onTertiaryContainer = OchreOnContainerDark,
    background = CharcoalBackground,
    onBackground = ParchmentPrimary,
    surface = CharcoalSurface,
    onSurface = ParchmentPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = ParchmentSecondary,
    surfaceContainerLowest = CharcoalBackground,
    surfaceContainerLow = CharcoalSurfaceContainerLow,
    surfaceContainer = CharcoalSurfaceContainer,
    surfaceContainerHigh = CharcoalSurfaceContainerHigh,
    outline = CharcoalOutline,
    outlineVariant = CharcoalOutlineVariant,
    error = ErrorRedDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

val FolioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FolioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) FolioDarkColorScheme else FolioLightColorScheme
    val stationery = if (isDark) DarkStationeryPalette else LightStationeryPalette

    CompositionLocalProvider(LocalStationeryPalette provides stationery) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FolioTypography,
            shapes = FolioShapes,
            content = content,
        )
    }
}

package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Editorial Warm Paper & Terracotta Ink Palette — Light
val PaperBackground = Color(0xFFFAF7F2)
val PaperSurface = Color(0xFFFFFFFF)
val PaperSurfaceVariant = Color(0xFFF1EAE0)
val PaperSurfaceContainerLow = Color(0xFFF6F1E9)
val PaperSurfaceContainer = Color(0xFFEEE7DC)
val PaperSurfaceContainerHigh = Color(0xFFE7DFD2)
val InkPrimary = Color(0xFF1F1C18)
val InkSecondary = Color(0xFF5D564E)
val InkOutline = Color(0xFFDCD3C5)
val InkOutlineVariant = Color(0xFFE9E2D7)

val TerracottaPrimary = Color(0xFFC8553D)
val TerracottaOnPrimary = Color(0xFFFFFFFF)
val TerracottaContainer = Color(0xFFFCE4DC)
val TerracottaOnContainer = Color(0xFF5C1D11)

val SageSecondary = Color(0xFF3E6B5C)
val SageOnSecondary = Color(0xFFFFFFFF)
val SageContainer = Color(0xFFDCEEE6)
val SageOnContainer = Color(0xFF16382D)

val OchreTertiary = Color(0xFFC97A1B)
val OchreOnTertiary = Color(0xFFFFFFFF)
val OchreContainer = Color(0xFFFDEBD2)
val OchreOnContainer = Color(0xFF4D2B05)

val ErrorRedLight = Color(0xFFBA1A1A)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// Editorial Charcoal & Luminous Coral Palette — Dark
val CharcoalBackground = Color(0xFF141311)
val CharcoalSurface = Color(0xFF1C1A17)
val CharcoalSurfaceVariant = Color(0xFF282521)
val CharcoalSurfaceContainerLow = Color(0xFF1A1815)
val CharcoalSurfaceContainer = Color(0xFF221F1C)
val CharcoalSurfaceContainerHigh = Color(0xFF2E2A25)
val ParchmentPrimary = Color(0xFFF3EDE4)
val ParchmentSecondary = Color(0xFFB5ACA0)
val CharcoalOutline = Color(0xFF453F38)
val CharcoalOutlineVariant = Color(0xFF322E28)

val TerracottaPrimaryDark = Color(0xFFEE856E)
val TerracottaOnPrimaryDark = Color(0xFF491207)
val TerracottaContainerDark = Color(0xFF6B281B)
val TerracottaOnContainerDark = Color(0xFFFFDBD2)

val SageSecondaryDark = Color(0xFF7EBBA6)
val SageOnSecondaryDark = Color(0xFF0E3429)
val SageContainerDark = Color(0xFF234D40)
val SageOnContainerDark = Color(0xFFCDEEE2)

val OchreTertiaryDark = Color(0xFFEBB365)
val OchreOnTertiaryDark = Color(0xFF422603)
val OchreContainerDark = Color(0xFF5E3B0C)
val OchreOnContainerDark = Color(0xFFFDEBD2)

val ErrorRedDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Curated Stationery Note Tints (Light & Dark)
data class StationeryPalette(
    val noteDefault: Color,
    val noteSand: Color,
    val noteSage: Color,
    val noteTerracotta: Color,
    val noteSlate: Color,
    val noteOchre: Color,
    val accentTerracotta: Color,
    val accentSage: Color,
    val accentOchre: Color,
    val accentSlate: Color,
    val accentSand: Color,
)

val LightStationeryPalette = StationeryPalette(
    noteDefault = Color(0xFFFFFFFF),
    noteSand = Color(0xFFF7EFE1),
    noteSage = Color(0xFFEAF3EE),
    noteTerracotta = Color(0xFFFCECE7),
    noteSlate = Color(0xFFEBF0F6),
    noteOchre = Color(0xFFFDF3DF),
    accentTerracotta = Color(0xFFC8553D),
    accentSage = Color(0xFF3E6B5C),
    accentOchre = Color(0xFFC97A1B),
    accentSlate = Color(0xFF4A6B8A),
    accentSand = Color(0xFF9C7B53),
)

val DarkStationeryPalette = StationeryPalette(
    noteDefault = Color(0xFF211E1B),
    noteSand = Color(0xFF2C261E),
    noteSage = Color(0xFF1C2924),
    noteTerracotta = Color(0xFF30201C),
    noteSlate = Color(0xFF1D252E),
    noteOchre = Color(0xFF2E2618),
    accentTerracotta = Color(0xFFEE856E),
    accentSage = Color(0xFF7EBBA6),
    accentOchre = Color(0xFFEBB365),
    accentSlate = Color(0xFF8AB1D6),
    accentSand = Color(0xFFC9AC86),
)

package com.osint.situationroom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SituationRoomColorScheme = darkColorScheme(
    primary          = CyanPrimary,
    onPrimary        = BgDeep,
    primaryContainer = BgElevated,
    onPrimaryContainer = TextPrimary,
    secondary        = AmberWarning,
    onSecondary      = BgDeep,
    secondaryContainer = BgSurface,
    onSecondaryContainer = TextPrimary,
    tertiary         = GreenSafe,
    onTertiary       = BgDeep,
    error            = RedAlert,
    onError          = BgDeep,
    background       = BgPrimary,
    onBackground     = TextPrimary,
    surface          = BgSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = BgElevated,
    onSurfaceVariant = TextSecond,
    outline          = BgBorder,
    outlineVariant   = BgBorder
)

@Composable
fun SituationRoomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SituationRoomColorScheme,
        typography  = SituationRoomTypography,
        content     = content
    )
}

package com.florilegia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = EspressoPrimary,
    onPrimary = IvoryOnPrimary,
    primaryContainer = SoftGoldContainer,
    onPrimaryContainer = EspressoDark,
    secondary = AgedBrass,
    onSecondary = CreamPaper,
    secondaryContainer = SandSection,
    onSecondaryContainer = InkPrimary,
    background = ParchmentPage,
    onBackground = InkPrimary,
    surface = CreamPaper,
    onSurface = InkPrimary,
    surfaceVariant = SandSection,
    onSurfaceVariant = OliveMuted,
    surfaceContainer = SandSection,
    outline = BorderMain,
    outlineVariant = BorderSubtle,
    error = StatusDangerText,
    errorContainer = StatusDangerBackground,
    onErrorContainer = StatusDangerText,
)

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = BorderMain,
    onSecondary = NightOnPrimary,
    background = NightBackground,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceContainer = NightSurfaceContainer,
    onSurfaceVariant = NightMuted,
    outline = NightOutline,
    error = NightDanger,
    errorContainer = NightDangerBackground,
    onErrorContainer = NightDanger,
)

@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
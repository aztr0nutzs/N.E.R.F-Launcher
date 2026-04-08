package com.nerf.launcher.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object LauncherTheme {
    val colors: LauncherColors
        @Composable get() = LocalLauncherColors.current

    val tokens: LauncherTokens
        @Composable get() = LocalLauncherTokens.current

    val typography: LauncherTypography
        @Composable get() = LocalLauncherTypography.current

    @Composable
    operator fun invoke(
        colors: LauncherColors = IndustrialLauncherColors,
        tokens: LauncherTokens = DefaultLauncherTokens,
        typography: LauncherTypography = DefaultLauncherTypography,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalLauncherColors provides colors,
            LocalLauncherTokens provides tokens,
            LocalLauncherTypography provides typography
        ) {
            MaterialTheme(
                colorScheme = launcherColorScheme(colors),
                typography = typography.asMaterialTypography(),
                content = content
            )
        }
    }
}

private fun launcherColorScheme(colors: LauncherColors) = darkColorScheme(
    primary = colors.accentCyan,
    onPrimary = colors.backgroundBottom,
    primaryContainer = colors.panelOuter,
    onPrimaryContainer = colors.textPrimary,
    secondary = colors.accentGreen,
    onSecondary = colors.backgroundBottom,
    tertiary = colors.accentMagenta,
    onTertiary = colors.backgroundBottom,
    background = colors.backgroundBottom,
    onBackground = colors.textPrimary,
    surface = colors.panelOuter,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.metalMid,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.frameLine,
    outlineVariant = colors.disabled,
    error = colors.danger,
    onError = colors.backgroundBottom,
    scrim = colors.backgroundScrim
)

package com.nerf.launcher.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class LauncherAccent {
    Cyan,
    Green,
    Magenta,
    Yellow
}

@Immutable
data class LauncherColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val backgroundScrim: Color,
    val metalDark: Color,
    val metalMid: Color,
    val metalHighlight: Color,
    val frameBase: Color,
    val frameLine: Color,
    val panelOuter: Color,
    val panelInner: Color,
    val panelInset: Color,
    val dockBase: Color,
    val dockInner: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentCyan: Color,
    val accentGreen: Color,
    val accentMagenta: Color,
    val accentYellow: Color,
    val danger: Color,
    val disabled: Color
) {
    fun accent(accent: LauncherAccent): Color = when (accent) {
        LauncherAccent.Cyan -> accentCyan
        LauncherAccent.Green -> accentGreen
        LauncherAccent.Magenta -> accentMagenta
        LauncherAccent.Yellow -> accentYellow
    }
}

val IndustrialLauncherColors = LauncherColors(
    backgroundTop = Color(0xFF090C11),
    backgroundBottom = Color(0xFF020304),
    backgroundScrim = Color(0xD9020408),
    metalDark = Color(0xFF0B1016),
    metalMid = Color(0xFF151C24),
    metalHighlight = Color(0xFF31414D),
    frameBase = Color(0xFF0E141A),
    frameLine = Color(0xFF4A5A67),
    panelOuter = Color(0xFF0C1117),
    panelInner = Color(0xFF080B10),
    panelInset = Color(0xFF03050A),
    dockBase = Color(0xFF0E131A),
    dockInner = Color(0xFF06090D),
    textPrimary = Color(0xFFE6FBFF),
    textSecondary = Color(0xFF9AB5BC),
    textMuted = Color(0xFF667983),
    accentCyan = Color(0xFF27E7FF),
    accentGreen = Color(0xFF73FF7C),
    accentMagenta = Color(0xFFFF47D0),
    accentYellow = Color(0xFFFFD84D),
    danger = Color(0xFFFF6A6A),
    disabled = Color(0xFF39444C)
)

val LocalLauncherColors = staticCompositionLocalOf { IndustrialLauncherColors }

package com.nerf.launcher.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LauncherGlowLevel {
    Low,
    Medium,
    High,
    Reactor
}

@Immutable
data class LauncherSpacingTokens(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 40.dp,
    val screenPadding: Dp = 20.dp,
    val contentGutter: Dp = 18.dp,
    val dockGap: Dp = 14.dp,
    val panelInset: Dp = 4.dp
)

@Immutable
data class LauncherGlowTokens(
    val low: Dp = 10.dp,
    val medium: Dp = 18.dp,
    val high: Dp = 28.dp,
    val reactor: Dp = 38.dp
) {
    fun radius(level: LauncherGlowLevel): Dp = when (level) {
        LauncherGlowLevel.Low -> low
        LauncherGlowLevel.Medium -> medium
        LauncherGlowLevel.High -> high
        LauncherGlowLevel.Reactor -> reactor
    }
}

@Immutable
data class LauncherStrokeTokens(
    val hairline: Dp = 1.dp,
    val fine: Dp = 1.5.dp,
    val strong: Dp = 2.dp,
    val heavy: Dp = 3.dp
)

@Immutable
data class LauncherShapeTokens(
    val frameChamfer: Dp = 28.dp,
    val panelChamfer: Dp = 20.dp,
    val plateChamfer: Dp = 18.dp,
    val tileChamfer: Dp = 16.dp,
    val innerChamfer: Dp = 10.dp,
    val microChamfer: Dp = 6.dp
)

@Immutable
data class LauncherReactorTokens(
    val coreDiameter: Dp = 112.dp,
    val coreRingThickness: Dp = 10.dp,
    val moduleMinHeight: Dp = 160.dp,
    val headerPlateHeight: Dp = 92.dp,
    val dockRegionHeight: Dp = 116.dp,
    val dockTileSize: Dp = 88.dp,
    val dockTileExpandedWidth: Dp = 104.dp
)

@Immutable
data class LauncherTokens(
    val spacing: LauncherSpacingTokens = LauncherSpacingTokens(),
    val glow: LauncherGlowTokens = LauncherGlowTokens(),
    val strokes: LauncherStrokeTokens = LauncherStrokeTokens(),
    val shapes: LauncherShapeTokens = LauncherShapeTokens(),
    val reactor: LauncherReactorTokens = LauncherReactorTokens()
)

internal val DefaultLauncherTokens = LauncherTokens()

val LocalLauncherTokens = staticCompositionLocalOf { DefaultLauncherTokens }

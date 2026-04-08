package com.nerf.launcher.ui.reactor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.theme.LauncherTheme
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Immutable
data class ReactorSegmentModel(
    val id: String,
    val label: String,
    val accent: LauncherAccent,
    val isActive: Boolean = false
)

@Immutable
data class ReactorSupportRing(
    val radiusFraction: Float,
    val strokeWidthFraction: Float = 0.016f,
    val alpha: Float = 0.4f,
    val dashCount: Int = 0,
    val accent: LauncherAccent = LauncherAccent.Cyan
)

@Immutable
data class ReactorCoreModel(
    val title: String = "NANO",
    val subtitle: String = "CORE",
    val status: String = "STABLE",
    val accent: LauncherAccent = LauncherAccent.Cyan,
    val isOnline: Boolean = true
)

@Immutable
data class ReactorMotionSpec(
    val pulseCore: Boolean = true,
    val breatheGlow: Boolean = true,
    val rotateOuterRing: Boolean = false,
    val pulseDurationMillis: Int = 3200,
    val breathingDurationMillis: Int = 4200,
    val rotationDurationMillis: Int = 120_000
)

@Immutable
data class ReactorInteractionState(
    val pressedSegmentId: String? = null,
    val isCorePressed: Boolean = false
)

enum class ReactorHapticEvent {
    CoreTap,
    SegmentTap,
    SegmentLongPress
}

@Immutable
data class ReactorModel(
    val segments: List<ReactorSegmentModel>,
    val supportRings: List<ReactorSupportRing> = ReactorDefaults.supportRings(),
    val core: ReactorCoreModel = ReactorCoreModel(),
    val startAngle: Float = -90f,
    val segmentGapAngle: Float = 8f,
    val outerPaddingFraction: Float = 0.08f,
    val outerRingThicknessFraction: Float = 0.15f,
    val labelRadiusFraction: Float = 0.71f,
    val coreRadiusFraction: Float = 0.28f
)

@Immutable
data class ReactorLayoutMetrics(
    val center: Offset,
    val outerRadiusPx: Float,
    val ringThicknessPx: Float,
    val coreRadiusPx: Float,
    val rotationDegrees: Float = 0f
) {
    val ringInnerRadiusPx: Float
        get() = (outerRadiusPx - ringThicknessPx).coerceAtLeast(0f)

    fun pointInGeometrySpace(point: Offset): Offset {
        if (rotationDegrees == 0f) return point

        val radians = Math.toRadians((-rotationDegrees).toDouble())
        val dx = point.x - center.x
        val dy = point.y - center.y
        val rotatedX = dx * cos(radians) - dy * sin(radians)
        val rotatedY = dx * sin(radians) + dy * cos(radians)
        return Offset(
            x = center.x + rotatedX.toFloat(),
            y = center.y + rotatedY.toFloat()
        )
    }

    fun distanceFromCenter(point: Offset): Float {
        val normalized = pointInGeometrySpace(point)
        return hypot(normalized.x - center.x, normalized.y - center.y)
    }

    fun isCoreHit(point: Offset): Boolean = distanceFromCenter(point) <= coreRadiusPx
}

@Immutable
data class ReactorPalette(
    val chassisShadow: Color,
    val chassisBase: Color,
    val chassisLine: Color,
    val ringTrack: Color,
    val ringInactive: Color,
    val innerRing: Color,
    val coreShell: Color,
    val coreInner: Color,
    val coreCenter: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val cyan: Color,
    val green: Color,
    val magenta: Color,
    val yellow: Color
) {
    fun accentColor(accent: LauncherAccent): Color = when (accent) {
        LauncherAccent.Cyan -> cyan
        LauncherAccent.Green -> green
        LauncherAccent.Magenta -> magenta
        LauncherAccent.Yellow -> yellow
    }
}

object ReactorDefaults {
    fun supportRings(): List<ReactorSupportRing> = listOf(
        ReactorSupportRing(
            radiusFraction = 0.58f,
            strokeWidthFraction = 0.018f,
            alpha = 0.45f,
            dashCount = 18,
            accent = LauncherAccent.Cyan
        ),
        ReactorSupportRing(
            radiusFraction = 0.46f,
            strokeWidthFraction = 0.012f,
            alpha = 0.3f,
            dashCount = 12,
            accent = LauncherAccent.Green
        ),
        ReactorSupportRing(
            radiusFraction = 0.36f,
            strokeWidthFraction = 0.01f,
            alpha = 0.24f,
            dashCount = 0,
            accent = LauncherAccent.Magenta
        )
    )

    fun segmentedCore(
        core: ReactorCoreModel = ReactorCoreModel(),
        activeSegmentIds: Set<String> = setOf("scan", "power")
    ): ReactorModel {
        val segments = listOf(
            ReactorSegmentModel(
                id = "sync",
                label = "SYNC",
                accent = LauncherAccent.Cyan,
                isActive = "sync" in activeSegmentIds
            ),
            ReactorSegmentModel(
                id = "power",
                label = "POWER",
                accent = LauncherAccent.Green,
                isActive = "power" in activeSegmentIds
            ),
            ReactorSegmentModel(
                id = "signal",
                label = "SIGNAL",
                accent = LauncherAccent.Magenta,
                isActive = "signal" in activeSegmentIds
            ),
            ReactorSegmentModel(
                id = "scan",
                label = "SCAN",
                accent = LauncherAccent.Yellow,
                isActive = "scan" in activeSegmentIds
            )
        )
        return ReactorModel(segments = segments, core = core)
    }
}

@Composable
fun rememberIndustrialReactorPalette(): ReactorPalette {
    val colors = LauncherTheme.colors
    return remember(colors) {
        ReactorPalette(
            chassisShadow = colors.backgroundBottom.copy(alpha = 0.92f),
            chassisBase = colors.metalDark,
            chassisLine = colors.frameLine,
            ringTrack = colors.metalHighlight.copy(alpha = 0.34f),
            ringInactive = colors.frameLine.copy(alpha = 0.48f),
            innerRing = colors.metalHighlight.copy(alpha = 0.24f),
            coreShell = colors.panelOuter,
            coreInner = colors.panelInner,
            coreCenter = colors.panelInset,
            textPrimary = colors.textPrimary,
            textSecondary = colors.textSecondary,
            cyan = colors.accentCyan,
            green = colors.accentGreen,
            magenta = colors.accentMagenta,
            yellow = colors.accentYellow
        )
    }
}

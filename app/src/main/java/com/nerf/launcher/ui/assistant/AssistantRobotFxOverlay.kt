package com.nerf.launcher.ui.assistant

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.nerf.launcher.util.assistant.AssistantState

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantRobotFxOverlay
//
//  Subtle, premium overlay-based robot reaction effects anchored to the
//  hotspot regions of the active assistant theme. Effects are state-driven
//  and never goofy or exaggerated.
//
//  Supported effects:
//    • Visor/eye glow pulse (listening, speaking)
//    • Chest/core reactor pulse (thinking, responding)
//    • Soft scan sweep line (idle, wake)
//    • Controlled localized glow aura (speaking)
//    • Error flicker (error state)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantRobotFxOverlay(
    state: AssistantState,
    hotspots: AssistantHotspots,
    palette: AssistantThemePalette,
    isChestCoreActive: Boolean,
    isHandProjectionActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "robotFx")

    // ── Visor glow pulse ──────────────────────────────────────────────────
    val visorPulse by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = when (state) {
            AssistantState.LISTENING -> 0.46f
            AssistantState.SPEAKING, AssistantState.RESPONDING -> 0.38f
            AssistantState.THINKING -> 0.30f
            AssistantState.WAKE -> 0.35f
            AssistantState.ERROR -> 0.50f
            else -> 0.18f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 800
                    AssistantState.SPEAKING -> 500
                    AssistantState.THINKING -> 1200
                    AssistantState.ERROR -> 300
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "visorPulse"
    )

    // ── Core reactor pulse ────────────────────────────────────────────────
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = when (state) {
            AssistantState.THINKING -> 0.42f
            AssistantState.RESPONDING -> 0.36f
            AssistantState.SPEAKING -> 0.28f
            AssistantState.WAKE -> 0.22f
            else -> 0.12f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 900
                    AssistantState.RESPONDING -> 650
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    // ── Scan sweep (vertical line moving down the body) ───────────────────
    val scanPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING, AssistantState.WAKE -> 2800
                    AssistantState.LISTENING -> 3500
                    else -> 5000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanSweep"
    )

    // ── Error flicker ─────────────────────────────────────────────────────
    val errorFlicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "errorFlicker"
    )

    val visorColor = Color(palette.visorGlow)
    val coreColor = Color(palette.coreGlow)
    val scanColor = Color(palette.scanSweep)
    val errorColor = Color(palette.errorGlow)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Determine which effects are active based on state
            val showVisorGlow = state != AssistantState.MUTED && state != AssistantState.SHUTTING_DOWN
            val showCoreGlow = state.isActive || isChestCoreActive
            val showScanSweep = state == AssistantState.IDLE || state == AssistantState.WAKE ||
                    state == AssistantState.THINKING || state == AssistantState.LISTENING
            val showErrorFx = state == AssistantState.ERROR

            // ── Visor glow ────────────────────────────────────────────────
            if (showVisorGlow) {
                drawVisorGlow(
                    chestBounds = hotspots.chestCore,
                    width = w,
                    height = h,
                    color = if (showErrorFx) errorColor else visorColor,
                    alpha = if (showErrorFx) errorFlicker * 0.5f else visorPulse
                )
            }

            // ── Core reactor glow ─────────────────────────────────────────
            if (showCoreGlow) {
                drawCoreGlow(
                    chestBounds = hotspots.chestCore,
                    width = w,
                    height = h,
                    color = if (showErrorFx) errorColor else coreColor,
                    alpha = if (isChestCoreActive) corePulse + 0.15f else corePulse
                )
            }

            // ── Scan sweep ────────────────────────────────────────────────
            if (showScanSweep && !showErrorFx) {
                drawScanSweep(
                    position = scanPosition,
                    width = w,
                    height = h,
                    color = scanColor
                )
            }

            // ── Hand projection glow ──────────────────────────────────────
            if (isHandProjectionActive) {
                drawRegionGlow(
                    bounds = hotspots.handProjection,
                    width = w,
                    height = h,
                    color = visorColor,
                    alpha = visorPulse * 0.6f
                )
            }

            // ── Error vignette ────────────────────────────────────────────
            if (showErrorFx) {
                drawErrorVignette(
                    width = w,
                    height = h,
                    color = errorColor,
                    intensity = errorFlicker * 0.15f
                )
            }
        }
    }
}

// ── Drawing helpers ──────────────────────────────────────────────────────────

private fun DrawScope.drawVisorGlow(
    chestBounds: Rect,
    width: Float,
    height: Float,
    color: Color,
    alpha: Float
) {
    // The visor sits at the top portion of the chest zone
    val visorCenterX = width * (chestBounds.left + chestBounds.right) / 2f
    val visorCenterY = height * chestBounds.top - height * 0.02f
    val visorRadius = width * (chestBounds.right - chestBounds.left) * 0.4f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.4f),
                Color.Transparent
            ),
            center = Offset(visorCenterX, visorCenterY),
            radius = visorRadius
        ),
        radius = visorRadius,
        center = Offset(visorCenterX, visorCenterY),
        blendMode = BlendMode.Screen
    )
}

private fun DrawScope.drawCoreGlow(
    chestBounds: Rect,
    width: Float,
    height: Float,
    color: Color,
    alpha: Float
) {
    val centerX = width * (chestBounds.left + chestBounds.right) / 2f
    val centerY = height * (chestBounds.top + chestBounds.bottom) / 2f
    val radius = width * (chestBounds.right - chestBounds.left) * 0.35f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha * 0.9f),
                color.copy(alpha = alpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius
        ),
        radius = radius,
        center = Offset(centerX, centerY),
        blendMode = BlendMode.Screen
    )
}

private fun DrawScope.drawScanSweep(
    position: Float,
    width: Float,
    height: Float,
    color: Color
) {
    val y = height * (0.15f + position * 0.70f) // Sweep from 15% to 85% of height
    val sweepHeight = height * 0.008f
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            color.copy(alpha = 0.12f),
            color.copy(alpha = 0.22f),
            color.copy(alpha = 0.12f),
            Color.Transparent
        ),
        startY = y - sweepHeight * 3f,
        endY = y + sweepHeight * 3f
    )
    drawRect(
        brush = gradient,
        topLeft = Offset(0f, y - sweepHeight * 3f),
        size = Size(width, sweepHeight * 6f),
        blendMode = BlendMode.Screen
    )
}

private fun DrawScope.drawRegionGlow(
    bounds: Rect,
    width: Float,
    height: Float,
    color: Color,
    alpha: Float
) {
    val centerX = width * (bounds.left + bounds.right) / 2f
    val centerY = height * (bounds.top + bounds.bottom) / 2f
    val radiusX = width * (bounds.right - bounds.left) * 0.5f
    val radiusY = height * (bounds.bottom - bounds.top) * 0.5f
    val radius = maxOf(radiusX, radiusY)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.3f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius
        ),
        radius = radius,
        center = Offset(centerX, centerY),
        blendMode = BlendMode.Screen
    )
}

private fun DrawScope.drawErrorVignette(
    width: Float,
    height: Float,
    color: Color,
    intensity: Float
) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = intensity)
            ),
            center = Offset(width / 2f, height / 2f),
            radius = maxOf(width, height) * 0.7f
        ),
        blendMode = BlendMode.Multiply
    )
}

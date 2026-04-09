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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nerf.launcher.util.assistant.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantRobotFxOverlay
//
//  All state-driven Canvas effects anchored to the overlay map via [imageRect].
//
//  Effects:
//    A. Visor glow — responsive to assistant state (LISTENING, SPEAKING, etc.)
//    B. Reactor pulse — ambient ring glow around reactorOuter
//    C. Reactor sector highlight — coloured arc for the selected sector
//    D. Reactor core burst — radial flash on core tap
//    E. Hand node radial glow — pulses when hand projection is active
//    F. Top module scan sweeps — horizontal sweeps over energyModule / statusModule
//    G. Focused input glow — border glow on inputShell
//    H. Dock selection glow — halo under selected dock button
//    I. Dock center pulse — when the NERF center logo is active
//    J. Left action stack highlight — glow behind active left button
//    K. Listening mic pulse — ring pulse on inputMic / dockMic
//    L. Error vignette — edge flicker on ERROR state
//    M. Scan sweep — full-body vertical sweep on idle/wake/thinking/listening
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantRobotFxOverlay(
    state: AssistantState,
    imageRect: Rect,
    palette: AssistantThemePalette,
    isChestCoreActive: Boolean,
    isHandProjectionActive: Boolean,
    activeSector: ReactorSector?,
    isReactorCoreBurst: Boolean,
    isListening: Boolean,
    isInputFocused: Boolean,
    activeDockAction: DockAction?,
    isDockCenterActive: Boolean,
    activeLeftAction: LeftAction?,
    modifier: Modifier = Modifier
) {
    val map = AssistantOverlayMap

    val it = rememberInfiniteTransition(label = "robotFx")

    // ── Animation values ─────────────────────────────────────────────────────

    // Visor glow pulse — amplitude depends on state
    val visorPulse by it.animateFloat(
        initialValue = 0.10f,
        targetValue  = when (state) {
            AssistantState.LISTENING              -> 0.48f
            AssistantState.SPEAKING,
            AssistantState.RESPONDING             -> 0.40f
            AssistantState.THINKING               -> 0.32f
            AssistantState.WAKE                   -> 0.36f
            AssistantState.ERROR                  -> 0.55f
            else                                  -> 0.18f
        },
        animationSpec = infiniteRepeatable(
            animation    = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 750
                    AssistantState.SPEAKING  -> 480
                    AssistantState.THINKING  -> 1100
                    AssistantState.ERROR     -> 260
                    else                     -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "visorPulse"
    )

    // Core reactor ambient pulse
    val corePulse by it.animateFloat(
        initialValue = 0.06f,
        targetValue  = when (state) {
            AssistantState.THINKING               -> 0.44f
            AssistantState.RESPONDING             -> 0.38f
            AssistantState.SPEAKING               -> 0.30f
            AssistantState.WAKE                   -> 0.22f
            else                                  -> 0.14f
        },
        animationSpec = infiniteRepeatable(
            animation    = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING  -> 900
                    AssistantState.RESPONDING -> 650
                    else                     -> 2600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    // Sector ring rotation angle — slow spin
    val sectorRingAngle by it.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sectorRing"
    )

    // Core burst ring expansion
    val burstRing by it.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coreBurst"
    )

    // Top module scan sweep (horizontal across energyModule / statusModule)
    val scanX by it.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "moduleScan"
    )

    // Listening mic ring pulse
    val micPulse by it.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    // Full-body vertical scan sweep
    val bodyScanPos by it.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING, AssistantState.WAKE -> 2800
                    AssistantState.LISTENING                     -> 3600
                    else                                         -> 5200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "bodyScan"
    )

    // Error flicker
    val errorFlicker by it.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(210, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "errorFlicker"
    )

    // Dock center pulse ring
    val dockCenterPulse by it.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dockCenterPulse"
    )

    // ── Derived colors ───────────────────────────────────────────────────────
    val visorColor  = Color(palette.visorGlow)
    val coreColor   = Color(palette.coreGlow)
    val scanColor   = Color(palette.scanSweep)
    val errorColor  = Color(palette.errorGlow)
    val accentColor = Color(palette.controlAccent)

    val showError     = state == AssistantState.ERROR
    val showBodyScan  = state == AssistantState.IDLE || state == AssistantState.WAKE ||
                        state == AssistantState.THINKING || state == AssistantState.LISTENING
    val showVisor     = state != AssistantState.MUTED && state != AssistantState.SHUTTING_DOWN
    val showCoreGlow  = state.isActive || isChestCoreActive || activeSector != null

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // ── A. Visor glow ──────────────────────────────────────────────
            if (showVisor) {
                drawVisorGlow(
                    imageRect = imageRect,
                    color     = if (showError) errorColor else visorColor,
                    alpha     = if (showError) errorFlicker * 0.55f else visorPulse
                )
            }

            // ── B. Reactor ambient ring glow ───────────────────────────────
            if (showCoreGlow) {
                drawReactorAmbientGlow(
                    imageRect = imageRect,
                    color     = if (showError) errorColor else coreColor,
                    alpha     = if (isChestCoreActive) corePulse + 0.16f else corePulse
                )
            }

            // ── C. Reactor sector highlight arc ────────────────────────────
            activeSector?.let { sector ->
                drawSectorArc(
                    imageRect = imageRect,
                    sector    = sector,
                    color     = accentColor,
                    alpha     = 0.55f + corePulse * 0.30f,
                    ringAngle = sectorRingAngle
                )
            }

            // ── D. Reactor core burst ──────────────────────────────────────
            if (isReactorCoreBurst) {
                drawCoreBurst(
                    imageRect = imageRect,
                    color     = coreColor,
                    progress  = burstRing
                )
            }

            // ── E. Hand node radial glow ───────────────────────────────────
            if (isHandProjectionActive) {
                drawRegionRadialGlow(
                    normRect  = AssistantOverlayMap.handNode,
                    imageRect = imageRect,
                    color     = visorColor,
                    alpha     = visorPulse * 0.65f
                )
            }

            // ── F. Top module scan sweeps ──────────────────────────────────
            drawModuleScanSweep(
                normRect  = AssistantOverlayMap.energyModule,
                imageRect = imageRect,
                color     = accentColor,
                progress  = scanX
            )
            drawModuleScanSweep(
                normRect  = AssistantOverlayMap.statusModule,
                imageRect = imageRect,
                color     = accentColor,
                progress  = 1f - scanX    // offset so they don't sweep in sync
            )

            // ── G. Input focused glow ──────────────────────────────────────
            if (isInputFocused) {
                drawRegionBorderGlow(
                    normRect  = AssistantOverlayMap.inputShell,
                    imageRect = imageRect,
                    color     = accentColor,
                    alpha     = 0.55f + micPulse * 0.25f
                )
            }

            // ── H. Dock button selection glow ──────────────────────────────
            activeDockAction?.let { action ->
                val dockRect = AssistantOverlayMap.dockButtons
                    .firstOrNull { it.first == action }?.second
                dockRect?.let {
                    drawRegionRadialGlow(
                        normRect  = it,
                        imageRect = imageRect,
                        color     = accentColor,
                        alpha     = 0.40f + corePulse * 0.20f
                    )
                }
            }

            // ── I. Dock center pulse ───────────────────────────────────────
            if (isDockCenterActive) {
                drawRegionRadialGlow(
                    normRect  = AssistantOverlayMap.dockCenterCore,
                    imageRect = imageRect,
                    color     = coreColor,
                    alpha     = 0.30f + dockCenterPulse * 0.45f
                )
            }

            // ── J. Left action stack highlight ─────────────────────────────
            activeLeftAction?.let { action ->
                val leftRect = AssistantOverlayMap.leftActionStack
                    .firstOrNull { it.first == action }?.second
                leftRect?.let {
                    drawRegionRadialGlow(
                        normRect  = it,
                        imageRect = imageRect,
                        color     = accentColor,
                        alpha     = 0.35f + corePulse * 0.25f
                    )
                }
            }

            // ── K. Listening mic pulse ─────────────────────────────────────
            if (isListening) {
                drawMicListeningRing(
                    imageRect = imageRect,
                    color     = Color(0xFF73FF7C),
                    pulse     = micPulse
                )
                // Also pulse dock mic if dock mic was used
                drawRegionRadialGlow(
                    normRect  = AssistantOverlayMap.dockMic,
                    imageRect = imageRect,
                    color     = Color(0xFF73FF7C),
                    alpha     = 0.25f + micPulse * 0.40f
                )
            }

            // ── L. Error vignette ──────────────────────────────────────────
            if (showError) {
                drawErrorVignette(
                    color     = errorColor,
                    intensity = errorFlicker * 0.18f
                )
            }

            // ── M. Full-body scan sweep ────────────────────────────────────
            if (showBodyScan && !showError) {
                drawBodyScanSweep(
                    imageRect = imageRect,
                    position  = bodyScanPos,
                    color     = scanColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Drawing functions — all operating in screen-pixel space
// ─────────────────────────────────────────────────────────────────────────────

/** A. Visor glow: radial glow centred on the visorZone. */
private fun DrawScope.drawVisorGlow(imageRect: Rect, color: Color, alpha: Float) {
    val visor = AssistantOverlayMap.visorZone.toPx(imageRect)
    val cx    = (visor.left + visor.right)  / 2f
    val cy    = (visor.top  + visor.bottom) / 2f
    val r     = (visor.width + visor.height) / 2f * 0.72f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.38f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = r
        ),
        radius = r,
        center = Offset(cx, cy),
        blendMode = BlendMode.Screen
    )
}

/** B. Reactor ambient ring glow: radial centred on reactorOuter. */
private fun DrawScope.drawReactorAmbientGlow(imageRect: Rect, color: Color, alpha: Float) {
    val outer = AssistantOverlayMap.reactorOuter.toPx(imageRect)
    val cx    = (outer.left + outer.right)  / 2f
    val cy    = (outer.top  + outer.bottom) / 2f
    val r     = (outer.width + outer.height) / 2f * 0.80f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha * 0.90f),
                color.copy(alpha = alpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = r
        ),
        radius     = r,
        center     = Offset(cx, cy),
        blendMode  = BlendMode.Screen
    )
}

/** C. Reactor sector arc highlight. */
private fun DrawScope.drawSectorArc(
    imageRect: Rect,
    sector: ReactorSector,
    color: Color,
    alpha: Float,
    ringAngle: Float
) {
    val outer  = AssistantOverlayMap.reactorOuter.toPx(imageRect)
    val cx     = (outer.left  + outer.right)  / 2f
    val cy     = (outer.top   + outer.bottom) / 2f
    val radius = (outer.width + outer.height) / 2f * 0.68f

    // Sector spans 90° (315→45, 45→135, 135→225, 225→315)
    // Android drawArc uses degrees where 0° = 3 o'clock, clockwise.
    val sweepDeg = 86f   // slightly less than 90° so arcs don't touch at corners
    val startDeg = when (sector) {
        ReactorSector.STABILITY_MONITOR -> -45f + ringAngle * 0.05f   // slight rotation drift
        ReactorSector.INTERFACE_CONFIG  ->  45f + ringAngle * 0.05f
        ReactorSector.RECALIBRATION     -> 135f + ringAngle * 0.05f
        ReactorSector.SYS_NET_DIAG      -> 225f + ringAngle * 0.05f
    }

    // Stroke width = ~12% of reactor outer width
    val strokeW = outer.width * 0.12f

    drawArc(
        color      = color.copy(alpha = alpha),
        startAngle = startDeg,
        sweepAngle = sweepDeg,
        useCenter  = false,
        topLeft    = Offset(cx - radius, cy - radius),
        size       = Size(radius * 2f, radius * 2f),
        style      = Stroke(width = strokeW, cap = StrokeCap.Round),
        blendMode  = BlendMode.Screen
    )

    // Glow halo behind the arc
    drawArc(
        color      = color.copy(alpha = alpha * 0.30f),
        startAngle = startDeg,
        sweepAngle = sweepDeg,
        useCenter  = false,
        topLeft    = Offset(cx - radius - strokeW, cy - radius - strokeW),
        size       = Size((radius + strokeW) * 2f, (radius + strokeW) * 2f),
        style      = Stroke(width = strokeW * 2.8f, cap = StrokeCap.Round),
        blendMode  = BlendMode.Screen
    )
}

/** D. Reactor core burst: expanding ring on core tap. */
private fun DrawScope.drawCoreBurst(imageRect: Rect, color: Color, progress: Float) {
    val reactorCenter = AssistantOverlayMap.reactorPhysics.centerNorm
    val cx = imageRect.left + reactorCenter.x * imageRect.width
    val cy = imageRect.top  + reactorCenter.y * imageRect.height

    val maxR = AssistantOverlayMap.reactorOuter.toPx(imageRect).width * 0.85f
    val r    = maxR * progress
    val a    = (1f - progress).coerceIn(0f, 1f)

    drawCircle(
        color     = color.copy(alpha = a * 0.65f),
        radius    = r,
        center    = Offset(cx, cy),
        style     = Stroke(width = 4f * (1f - progress) + 1f),
        blendMode = BlendMode.Screen
    )
}

/** E / J / partial dock: general radial glow centred on a NormRect. */
private fun DrawScope.drawRegionRadialGlow(
    normRect: NormRect,
    imageRect: Rect,
    color: Color,
    alpha: Float
) {
    val px = normRect.toPx(imageRect)
    val cx = (px.left + px.right)  / 2f
    val cy = (px.top  + px.bottom) / 2f
    val r  = maxOf(px.width, px.height) * 0.70f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.30f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = r
        ),
        radius    = r,
        center    = Offset(cx, cy),
        blendMode = BlendMode.Screen
    )
}

/** F. Top module scan sweep: horizontal light line crossing the module. */
private fun DrawScope.drawModuleScanSweep(
    normRect: NormRect,
    imageRect: Rect,
    color: Color,
    progress: Float
) {
    val px     = normRect.toPx(imageRect)
    val x      = px.left + px.width * progress
    val height = px.height

    drawLine(
        brush = Brush.verticalGradient(
            colors  = listOf(Color.Transparent, color.copy(0.22f), Color.Transparent),
            startY  = px.top,
            endY    = px.bottom
        ),
        start     = Offset(x, px.top),
        end       = Offset(x, px.bottom),
        strokeWidth = 2.5f,
        blendMode = BlendMode.Screen
    )
    // Soft glow halo on the sweep line
    drawRect(
        brush = Brush.horizontalGradient(
            colors  = listOf(
                Color.Transparent,
                color.copy(0.08f),
                color.copy(0.15f),
                color.copy(0.08f),
                Color.Transparent
            ),
            startX  = (x - height).coerceAtLeast(px.left),
            endX    = (x + height).coerceAtMost(px.right)
        ),
        topLeft   = Offset((x - height).coerceAtLeast(px.left), px.top),
        size      = Size((height * 2f).coerceAtMost(px.width), height),
        blendMode = BlendMode.Screen
    )
}

/** G. Input shell border glow. */
private fun DrawScope.drawRegionBorderGlow(
    normRect: NormRect,
    imageRect: Rect,
    color: Color,
    alpha: Float
) {
    val px = normRect.toPx(imageRect)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = alpha * 0.30f),
                color.copy(alpha = alpha * 0.55f),
                color.copy(alpha = alpha * 0.30f),
                Color.Transparent
            ),
            startY = px.top,
            endY   = px.bottom
        ),
        topLeft = Offset(px.left, px.top),
        size    = Size(px.width, px.height),
        blendMode = BlendMode.Screen
    )
}

/** K. Listening mic ring — pulsing concentric ring around the inputMic region. */
private fun DrawScope.drawMicListeningRing(imageRect: Rect, color: Color, pulse: Float) {
    val micPx  = AssistantOverlayMap.inputMic.toPx(imageRect)
    val cx     = (micPx.left + micPx.right)  / 2f
    val cy     = (micPx.top  + micPx.bottom) / 2f
    val baseR  = maxOf(micPx.width, micPx.height) / 2f
    val r      = baseR * (1.4f + pulse * 1.6f)

    drawCircle(
        color     = color.copy(alpha = (1f - pulse) * 0.55f),
        radius    = r,
        center    = Offset(cx, cy),
        style     = Stroke(width = 2f),
        blendMode = BlendMode.Screen
    )
}

/** L. Error vignette: edge-to-centre darkening. */
private fun DrawScope.drawErrorVignette(color: Color, intensity: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = intensity)
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = maxOf(size.width, size.height) * 0.75f
        ),
        blendMode = BlendMode.Multiply
    )
}

/** M. Full-body vertical scan sweep. */
private fun DrawScope.drawBodyScanSweep(imageRect: Rect, position: Float, color: Color) {
    // Sweep within the robot body zone (15% to 85% of image height)
    val yMin = imageRect.top  + imageRect.height * 0.15f
    val yMax = imageRect.top  + imageRect.height * 0.85f
    val y    = yMin + (yMax - yMin) * position
    val sh   = imageRect.height * 0.007f

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = 0.10f),
                color.copy(alpha = 0.20f),
                color.copy(alpha = 0.10f),
                Color.Transparent
            ),
            startY = y - sh * 3f,
            endY   = y + sh * 3f
        ),
        topLeft   = Offset(imageRect.left, y - sh * 3f),
        size      = Size(imageRect.width, sh * 6f),
        blendMode = BlendMode.Screen
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantState helper — determines whether state is "active"
// ─────────────────────────────────────────────────────────────────────────────

private val AssistantState.isActive: Boolean get() = when (this) {
    AssistantState.IDLE,
    AssistantState.MUTED,
    AssistantState.SHUTTING_DOWN -> false
    else -> true
}

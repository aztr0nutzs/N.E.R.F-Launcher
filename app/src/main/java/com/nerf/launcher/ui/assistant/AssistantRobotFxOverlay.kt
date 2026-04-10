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

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantRobotFxOverlay
//
//  State-driven Canvas effects anchored to overlay map regions via [imageRect].
//
//  Effects:
//    A. Visor glow       — state-responsive, anchored to visorZone width axis
//    B. Reactor ambient  — pulsing radial around reactorOuter center
//    C. Sector arc       — FIXED-position arc at the spec sector boundaries (no drift)
//    D. Core burst       — expanding ring on ReactorCoreTapped
//    E. Hand node glow   — radial on handNode when projection is active
//    F. Module scan      — horizontal light sweep across energy/status modules
//    G. Input focus glow — border glow on inputShell
//    H. Dock button glow — radial halo on active dock button
//    I. Dock center pulse— ring pulse on dockCenterCore
//    J. Left stack glow  — radial behind active left-stack button
//    K. Mic listening    — pulsing ring on inputMic
//    L. Error vignette   — edge flicker on ERROR state
//    M. Body scan sweep  — vertical sweep within robotBody bounds
//
//  CORRECTION NOTE (sector arcs):
//    Sector arcs are anchored to STRICTLY FIXED angles derived from
//    ReactorSector.arcStartAngle. The previous ringAngle drift offset
//    (ringAngle * 0.05f) has been removed — it caused arcs to drift 18° off
//    the artwork sector boundaries over a full animation cycle.
//
//  CORRECTION NOTE (visor glow radius):
//    Radius is now derived from visorZone.width / 2f only (not averaged with
//    height). The visorZone is a horizontal eye-slot; the old average caused
//    the glow to bleed down onto the chest panel.
//
//  CORRECTION NOTE (body scan sweep):
//    Sweep bounds are now clamped to the robotBody NormRect instead of the
//    hardcoded 0.15f–0.85f image-height fraction.
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
    val t = rememberInfiniteTransition(label = "robotFx")

    // ── Animation drivers ────────────────────────────────────────────────────

    val visorPulse by t.animateFloat(
        initialValue = 0.10f,
        targetValue = when (state) {
            AssistantState.LISTENING              -> 0.48f
            AssistantState.SPEAKING,
            AssistantState.RESPONDING             -> 0.40f
            AssistantState.THINKING               -> 0.32f
            AssistantState.WAKE                   -> 0.36f
            AssistantState.ERROR                  -> 0.55f
            else                                  -> 0.18f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
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

    val corePulse by t.animateFloat(
        initialValue = 0.06f,
        targetValue = when (state) {
            AssistantState.THINKING               -> 0.44f
            AssistantState.RESPONDING             -> 0.38f
            AssistantState.SPEAKING               -> 0.30f
            AssistantState.WAKE                   -> 0.22f
            else                                  -> 0.14f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING   -> 900
                    AssistantState.RESPONDING -> 650
                    else                      -> 2600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    // Decorative slow-spinning micro-ring (NOT used to offset arc positions).
    val ringSpinAngle by t.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringSpinAngle"
    )

    val burstRing by t.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coreBurst"
    )

    val scanX by t.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "moduleScan"
    )

    val micPulse by t.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    val bodyScanPos by t.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
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

    val errorFlicker by t.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(210, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "errorFlicker"
    )

    val dockCenterPulse by t.animateFloat(
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

    val showError    = state == AssistantState.ERROR
    val showBodyScan = state == AssistantState.IDLE || state == AssistantState.WAKE ||
                       state == AssistantState.THINKING || state == AssistantState.LISTENING
    val showVisor    = state != AssistantState.MUTED && state != AssistantState.SHUTTING_DOWN
    val showCoreGlow = state.isActive || isChestCoreActive || activeSector != null

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // A. Visor glow
            if (showVisor) {
                drawVisorGlow(
                    imageRect = imageRect,
                    color     = if (showError) errorColor else visorColor,
                    alpha     = if (showError) errorFlicker * 0.55f else visorPulse
                )
            }

            // B. Reactor ambient ring glow
            if (showCoreGlow) {
                drawReactorAmbientGlow(
                    imageRect = imageRect,
                    color     = if (showError) errorColor else coreColor,
                    alpha     = if (isChestCoreActive) corePulse + 0.16f else corePulse
                )
            }

            // C. Reactor sector arc — FIXED position, no angular drift
            activeSector?.let { sector ->
                drawSectorArc(
                    imageRect    = imageRect,
                    sector       = sector,
                    color        = accentColor,
                    alpha        = 0.55f + corePulse * 0.30f,
                    spinDotAngle = ringSpinAngle   // used only for decorative dots, not the arc
                )
            }

            // D. Core burst
            if (isReactorCoreBurst) {
                drawCoreBurst(imageRect = imageRect, color = coreColor, progress = burstRing)
            }

            // E. Hand node glow
            if (isHandProjectionActive) {
                drawRegionRadialGlow(
                    normRect  = AssistantOverlayMap.handNode,
                    imageRect = imageRect,
                    color     = visorColor,
                    alpha     = visorPulse * 0.65f
                )
            }

            // F. Top module scan sweeps (counter-phase so they don't sync)
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
                progress  = (scanX + 0.5f) % 1f   // 180° phase offset
            )

            // G. Input focus glow
            if (isInputFocused) {
                drawRegionBorderGlow(
                    normRect  = AssistantOverlayMap.inputShell,
                    imageRect = imageRect,
                    color     = accentColor,
                    alpha     = 0.55f + micPulse * 0.25f
                )
            }

            // H. Dock button selection glow
            activeDockAction?.let { action ->
                AssistantOverlayMap.dockButtons
                    .firstOrNull { it.first == action }?.second
                    ?.let { nr ->
                        drawRegionRadialGlow(
                            normRect  = nr,
                            imageRect = imageRect,
                            color     = accentColor,
                            alpha     = 0.40f + corePulse * 0.20f
                        )
                    }
            }

            // I. Dock center pulse
            if (isDockCenterActive) {
                drawRegionRadialGlow(
                    normRect  = AssistantOverlayMap.dockCenterCore,
                    imageRect = imageRect,
                    color     = coreColor,
                    alpha     = 0.30f + dockCenterPulse * 0.45f
                )
            }

            // J. Left action stack glow
            activeLeftAction?.let { action ->
                AssistantOverlayMap.leftActionStack
                    .firstOrNull { it.first == action }?.second
                    ?.let { nr ->
                        drawRegionRadialGlow(
                            normRect  = nr,
                            imageRect = imageRect,
                            color     = accentColor,
                            alpha     = 0.35f + corePulse * 0.25f
                        )
                    }
            }

            // K. Listening mic ring + dock mic glow
            if (isListening) {
                drawMicListeningRing(
                    imageRect = imageRect,
                    color     = accentColor,
                    pulse     = micPulse
                )
                drawRegionRadialGlow(
                    normRect  = AssistantOverlayMap.dockMic,
                    imageRect = imageRect,
                    color     = accentColor,
                    alpha     = 0.25f + micPulse * 0.40f
                )
            }

            // L. Error vignette
            if (showError) {
                drawErrorVignette(color = errorColor, intensity = errorFlicker * 0.18f)
            }

            // M. Body scan sweep — bounds from robotBody NormRect
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
//  Drawing functions (screen-pixel space)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A. Visor glow.
 *
 * CORRECTION: radius derives from [visorZone].width / 2f only.
 * The visorZone is a wide, short horizontal slot. Using the averaged
 * (width+height)/2 made the glow too tall, bleeding onto the chest panel.
 */
private fun DrawScope.drawVisorGlow(imageRect: Rect, color: Color, alpha: Float) {
    val visor = AssistantOverlayMap.visorZone.toPx(imageRect)
    val cx    = (visor.left + visor.right)  / 2f
    val cy    = (visor.top  + visor.bottom) / 2f
    // Radius = half the slot width, capped so it stays within the face zone.
    val r     = visor.width / 2f * 0.80f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.40f),
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

/** B. Reactor ambient ring glow. */
private fun DrawScope.drawReactorAmbientGlow(imageRect: Rect, color: Color, alpha: Float) {
    val outer = AssistantOverlayMap.reactorOuter.toPx(imageRect)
    val cx    = (outer.left + outer.right)  / 2f
    val cy    = (outer.top  + outer.bottom) / 2f
    val r     = minOf(outer.width, outer.height) / 2f * 1.05f   // slight halo beyond the ring

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
        radius    = r,
        center    = Offset(cx, cy),
        blendMode = BlendMode.Screen
    )
}

/**
 * C. Reactor sector arc highlight.
 *
 * CORRECTION: arc start angle is taken directly from [ReactorSector.arcStartAngle]
 * with NO rotation offset applied. The previous `ringAngle * 0.05f` drift caused
 * the arc to slide 18° off the artwork sector boundary every 12 seconds.
 *
 * [spinDotAngle] (0°→360°, slow) is kept for future decorative use (e.g. a small
 * dot orbiting the ring) but does NOT influence the arc position.
 */
private fun DrawScope.drawSectorArc(
    imageRect: Rect,
    sector: ReactorSector,
    color: Color,
    alpha: Float,
    spinDotAngle: Float   // reserved for future decorative dots; unused by arc
) {
    val outer  = AssistantOverlayMap.reactorOuter.toPx(imageRect)
    val cx     = (outer.left  + outer.right)  / 2f
    val cy     = (outer.top   + outer.bottom) / 2f
    // Radius at the mid-point of the active ring band.
    val radius = minOf(outer.width, outer.height) / 2f * 0.82f
    val strokeW= minOf(outer.width, outer.height) * 0.09f   // ~9% of ring diameter

    // Fixed start angle from the enum — matches artwork sector boundaries exactly.
    val startDeg = sector.arcStartAngle
    val sweepDeg = sector.arcSweepAngle

    // Primary arc
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

    // Soft glow halo slightly outside the arc stroke
    val haloR = radius + strokeW * 0.5f
    drawArc(
        color      = color.copy(alpha = alpha * 0.28f),
        startAngle = startDeg,
        sweepAngle = sweepDeg,
        useCenter  = false,
        topLeft    = Offset(cx - haloR, cy - haloR),
        size       = Size(haloR * 2f, haloR * 2f),
        style      = Stroke(width = strokeW * 2.6f, cap = StrokeCap.Round),
        blendMode  = BlendMode.Screen
    )
}

/** D. Reactor core burst — expanding ring on core tap. */
private fun DrawScope.drawCoreBurst(imageRect: Rect, color: Color, progress: Float) {
    val center = AssistantOverlayMap.reactorPhysics.centerNorm
    val cx = imageRect.left + center.x * imageRect.width
    val cy = imageRect.top  + center.y * imageRect.height

    val maxR = AssistantOverlayMap.reactorOuter.toPx(imageRect).let {
        minOf(it.width, it.height)
    } * 0.90f

    val r = maxR * progress
    val a = (1f - progress).coerceIn(0f, 1f)

    drawCircle(
        color     = color.copy(alpha = a * 0.65f),
        radius    = r,
        center    = Offset(cx, cy),
        style     = Stroke(width = 3.5f * (1f - progress) + 1f),
        blendMode = BlendMode.Screen
    )
}

/** E / H / I / J: general radial glow centred on a NormRect. */
private fun DrawScope.drawRegionRadialGlow(
    normRect: NormRect,
    imageRect: Rect,
    color: Color,
    alpha: Float
) {
    val px = normRect.toPx(imageRect)
    val cx = (px.left + px.right)  / 2f
    val cy = (px.top  + px.bottom) / 2f
    val r  = maxOf(px.width, px.height) * 0.72f

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

/**
 * F. Top module scan sweep.
 * The sweep is clamped to [px.left, px.right] to prevent bleeding outside
 * the module bounds.
 */
private fun DrawScope.drawModuleScanSweep(
    normRect: NormRect,
    imageRect: Rect,
    color: Color,
    progress: Float
) {
    val px = normRect.toPx(imageRect)
    val x  = px.left + px.width * progress.coerceIn(0f, 1f)

    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, color.copy(0.24f), Color.Transparent),
            startY = px.top,
            endY   = px.bottom
        ),
        start       = Offset(x, px.top),
        end         = Offset(x, px.bottom),
        strokeWidth = 2.5f,
        blendMode   = BlendMode.Screen
    )
    // Halo glow band around the sweep line
    val haloHalf = px.height.coerceAtMost(px.width * 0.12f)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(0.08f),
                color.copy(0.16f),
                color.copy(0.08f),
                Color.Transparent
            ),
            startX = (x - haloHalf).coerceAtLeast(px.left),
            endX   = (x + haloHalf).coerceAtMost(px.right)
        ),
        topLeft   = Offset((x - haloHalf).coerceAtLeast(px.left), px.top),
        size      = Size(
            (haloHalf * 2f).coerceAtMost(px.right - px.left),
            px.height
        ),
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
        topLeft   = Offset(px.left, px.top),
        size      = Size(px.width, px.height),
        blendMode = BlendMode.Screen
    )
}

/** K. Listening mic ring — pulsing concentric ring on inputMic. */
private fun DrawScope.drawMicListeningRing(imageRect: Rect, color: Color, pulse: Float) {
    val micPx = AssistantOverlayMap.inputMic.toPx(imageRect)
    val cx    = (micPx.left + micPx.right)  / 2f
    val cy    = (micPx.top  + micPx.bottom) / 2f
    val baseR = maxOf(micPx.width, micPx.height) / 2f
    val r     = baseR * (1.4f + pulse * 1.6f)

    drawCircle(
        color     = color.copy(alpha = (1f - pulse) * 0.55f),
        radius    = r,
        center    = Offset(cx, cy),
        style     = Stroke(width = 2f),
        blendMode = BlendMode.Screen
    )
}

/** L. Error vignette — edge-to-centre darkening. */
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

/**
 * M. Full-body vertical scan sweep.
 *
 * CORRECTION: sweep bounds are now derived from the [robotBody] NormRect
 * (top → bottom of the robot) instead of fixed 0.15f–0.85f image-height
 * fractions. This ensures the sweep tracks the artwork robot body on all
 * themes and screen aspect ratios.
 */
private fun DrawScope.drawBodyScanSweep(imageRect: Rect, position: Float, color: Color) {
    val body = AssistantOverlayMap.robotBody.toPx(imageRect)
    val yMin = body.top
    val yMax = body.bottom
    val y    = yMin + (yMax - yMin) * position
    val sh   = (yMax - yMin) * 0.007f

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
        topLeft   = Offset(body.left, y - sh * 3f),
        size      = Size(body.width, sh * 6f),
        blendMode = BlendMode.Screen
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantState helper
// ─────────────────────────────────────────────────────────────────────────────

private val AssistantState.isActive: Boolean
    get() = when (this) {
        AssistantState.IDLE,
        AssistantState.MUTED,
        AssistantState.SHUTTING_DOWN -> false
        else                         -> true
    }


// ============================================================
// ReactorUI.kt — Full N-Reactor Jetpack Compose Implementation
// ============================================================
// Place this file in your app's composable package.
// Dependencies needed in build.gradle:
//   implementation("androidx.compose.animation:animation-core")
//   implementation("androidx.compose.ui:ui-graphics")
// ============================================================

package com.nerf.launcher.reactor.compose.prototype

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.math.*

// ── Zone Model ───────────────────────────────────────────────────────────────
enum class ReactorZone(
    val label: String,
    val subLabel: String,
    val startDeg: Float,
    val endDeg: Float,
    val color: Color,
    val icon: String
) {
    TOP(
        "SYSTEM / NETWORK / DIAGNOSTICS", "▲ SYSTEM / NETWORK",
        startDeg = -135f, endDeg = -45f,
        color = Color(0xFF00D4FF), icon = "⬡"
    ),
    RIGHT(
        "AI / ASSISTANT / VOICE", "► AI / VOICE ACTIVE",
        startDeg = -45f, endDeg = 45f,
        color = Color(0xFFFF9500), icon = "◈"
    ),
    BOTTOM(
        "TOOLS / WEAPONS / UTILITIES", "▼ TOOLS / WEAPONS",
        startDeg = 45f, endDeg = 135f,
        color = Color(0xFF00FF44), icon = "⬢"
    ),
    LEFT(
        "SETTINGS / CONTROLS", "◄ SETTINGS / CTRL",
        startDeg = 135f, endDeg = 225f,
        color = Color(0xFFFF00CC), icon = "◉"
    );
}

// ── Main Composable ──────────────────────────────────────────────────────────
@Composable
fun ReactorScreen() {
    var statusText  by remember { mutableStateOf("■ REACTOR ONLINE ■ ALL SYSTEMS NOMINAL ■") }
    var statusColor by remember { mutableStateOf(Color(0xFF00FF88)) }
    var zoneText    by remember { mutableStateOf("TAP A ZONE TO ACTIVATE") }
    var zoneColor   by remember { mutableStateOf(Color.White) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ReactorCanvas(
            modifier = Modifier.size(360.dp),
            onZoneTap = { zone ->
                statusText  = "■ ${zone.label} ■"
                statusColor = zone.color
                zoneText    = zone.subLabel
                zoneColor   = zone.color
            },
            onCoreTap = {
                statusText  = "■ PRIMARY ACTIVATION — MODE SWITCH ENGAGED ■"
                statusColor = Color(0xFFFF6600)
                zoneText    = "⚡ CORE ACTIVATED"
                zoneColor   = Color(0xFFFF6600)
            }
        )

        Text(
            text = zoneText,
            color = zoneColor,
            fontSize = 13.sp,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

// ── Reactor Canvas ───────────────────────────────────────────────────────────
@Composable
fun ReactorCanvas(
    modifier: Modifier = Modifier,
    onZoneTap: (ReactorZone) -> Unit,
    onCoreTap: () -> Unit
) {
    // ── Infinite animations ─────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "reactor")

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "corePulse"
    )
    val traceOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "traceOffset"
    )
    val ringSwoop by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "ringSwoop"
    )
    val outerRimPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "rimPulse"
    )

    // ── Tap-activated state ─────────────────────────────────────────────────
    var activeZone  by remember { mutableStateOf<ReactorZone?>(null) }
    var coreActive  by remember { mutableStateOf(false) }
    var rippleList  by remember { mutableStateOf(listOf<RippleState>()) }

    // Animate ripples
    LaunchedEffect(rippleList) {
        if (rippleList.isNotEmpty()) {
            kotlinx.coroutines.delay(600)
            rippleList = rippleList.drop(1)
        }
    }

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing)),
        label = "ripple"
    )

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val cx = size.width / 2f
                val cy = size.height / 2f
                val dx = offset.x - cx
                val dy = offset.y - cy
                val dist = sqrt(dx * dx + dy * dy)
                val maxR  = size.width / 2f

                if (dist < maxR * 0.35f) {
                    coreActive = true
                    rippleList = rippleList + RippleState(offset, Color(0xFFFF6600))
                    onCoreTap()
                } else {
                    val angle = (atan2(dy, dx) * 180 / PI).toFloat()
                    val inner = maxR * 0.64f
                    val outer = maxR * 0.96f
                    if (dist in inner..outer) {
                        val zone = ReactorZone.values().firstOrNull { z ->
                            angleInRange(angle, z.startDeg, z.endDeg)
                        }
                        zone?.let {
                            activeZone = it
                            rippleList = rippleList + RippleState(offset, it.color)
                            onZoneTap(it)
                        }
                    }
                }
            }
        }
    ) {
        val cx  = size.width / 2f
        val cy  = size.height / 2f
        val maxR = size.width / 2f

        drawArmor(cx, cy, maxR)
        drawCircuitTraces(cx, cy, maxR, traceOffset)
        drawSegmentRing(cx, cy, maxR, activeZone, ringSwoop)
        drawInnerBezel(cx, cy, maxR)
        drawCoreNode(cx, cy, maxR, corePulse, coreActive, outerRimPulse)
        drawGlassOverlay(cx, cy, maxR)

        // Ripple effects
        rippleList.forEachIndexed { i, rp ->
            val p = ((rippleProgress + i * 0.3f) % 1f)
            drawRipple(rp.pos, rp.color, p, maxR)
        }
    }
}

// ── Draw: Armor Background ───────────────────────────────────────────────────
private fun DrawScope.drawArmor(cx: Float, cy: Float, maxR: Float) {
    // Deep background
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF0A0C1A), Color(0xFF050608)),
            center = Offset(cx, cy), radius = maxR * 1.2f
        ),
        center = Offset(cx, cy), radius = maxR
    )
    // Armor segments
    for (i in 0 until 8) {
        val a = Math.toRadians((i * 45).toDouble())
        drawLine(
            color = Color(0x333C5078),
            start = Offset(cx + cos(a).toFloat() * maxR * 0.64f, cy + sin(a).toFloat() * maxR * 0.64f),
            end   = Offset(cx + cos(a).toFloat() * maxR * 0.99f, cy + sin(a).toFloat() * maxR * 0.99f),
            strokeWidth = 1.5f
        )
    }
    // Armor arc ticks
    for (i in 0 until 8) {
        val a1 = Math.toRadians((i * 45 + 3).toDouble()).toFloat()
        val a2 = Math.toRadians((i * 45 + 42).toDouble()).toFloat()
        drawArc(
            color = Color(0x2828507A),
            startAngle = a1 * 180f / PI.toFloat(),
            sweepAngle = (a2 - a1) * 180f / PI.toFloat(),
            useCenter = false,
            topLeft = Offset(cx - maxR * 0.81f, cy - maxR * 0.81f),
            size    = Size(maxR * 1.62f, maxR * 1.62f),
            style   = Stroke(2f)
        )
    }
}

// ── Draw: Circuit Traces ─────────────────────────────────────────────────────
data class TraceSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val color: Color)

private fun DrawScope.drawCircuitTraces(cx: Float, cy: Float, maxR: Float, offset: Float) {
    val s = maxR / 300f  // scale factor

    val traces = listOf(
        TraceSegment(cx, cy - 150*s, cx, cy - 105*s, Color(0xFF00FF44)),
        TraceSegment(cx - 40*s, cy - 140*s, cx - 40*s, cy - 105*s, Color(0xFF00AAFF)),
        TraceSegment(cx + 40*s, cy - 140*s, cx + 40*s, cy - 105*s, Color(0xFFFF9500)),
        TraceSegment(cx + 150*s, cy, cx + 105*s, cy, Color(0xFFFF9500)),
        TraceSegment(cx + 140*s, cy - 40*s, cx + 105*s, cy - 40*s, Color(0xFF00FF44)),
        TraceSegment(cx + 140*s, cy + 40*s, cx + 105*s, cy + 40*s, Color(0xFFFF00CC)),
        TraceSegment(cx, cy + 150*s, cx, cy + 105*s, Color(0xFF00FF44)),
        TraceSegment(cx - 40*s, cy + 140*s, cx - 40*s, cy + 105*s, Color(0xFFFF00CC)),
        TraceSegment(cx + 40*s, cy + 140*s, cx + 40*s, cy + 105*s, Color(0xFF00AAFF)),
        TraceSegment(cx - 150*s, cy, cx - 105*s, cy, Color(0xFF00AAFF)),
        TraceSegment(cx - 140*s, cy - 40*s, cx - 105*s, cy - 40*s, Color(0xFFFF9500)),
        TraceSegment(cx - 140*s, cy + 40*s, cx - 105*s, cy + 40*s, Color(0xFFFF00CC)),
    )
    traces.forEach { tr ->
        // Dim base
        drawLine(tr.color.copy(alpha = 0.15f), Offset(tr.x1,tr.y1), Offset(tr.x2,tr.y2), 1.2f)
        // Animated bright dash
        val dx = tr.x2-tr.x1; val dy = tr.y2-tr.y1
        val len = sqrt(dx*dx+dy*dy)
        if (len > 0) {
            val frac = (offset % len) / len
            val sx = tr.x1 + dx * frac; val sy = tr.y1 + dy * frac
            val ex = (sx + dx * 0.3f).coerceIn(
                minOf(tr.x1,tr.x2).toDouble().toFloat(),
                maxOf(tr.x1,tr.x2).toDouble().toFloat()
            )
            val ey = (sy + dy * 0.3f).coerceIn(
                minOf(tr.y1,tr.y2).toDouble().toFloat(),
                maxOf(tr.y1,tr.y2).toDouble().toFloat()
            )
            drawLine(tr.color, Offset(sx,sy), Offset(ex,ey), 2.5f, cap = StrokeCap.Round)
        }
    }
    // PCB dots
    val dotColors = listOf(Color(0xFF00FF44),Color(0xFF00AAFF),Color(0xFFFF9500),Color(0xFFFF00CC))
    listOf(
        Offset(cx, cy-105*s), Offset(cx-40*s,cy-105*s), Offset(cx+40*s,cy-105*s),
        Offset(cx+105*s,cy),  Offset(cx+105*s,cy-40*s), Offset(cx+105*s,cy+40*s),
        Offset(cx,cy+105*s),  Offset(cx-40*s,cy+105*s), Offset(cx+40*s,cy+105*s),
        Offset(cx-105*s,cy),  Offset(cx-105*s,cy-40*s), Offset(cx-105*s,cy+40*s),
    ).forEachIndexed { i, pt ->
        drawCircle(dotColors[i%4], 3f*s, pt)
    }
}

// ── Draw: Interaction Segment Ring ───────────────────────────────────────────
private fun DrawScope.drawSegmentRing(
    cx: Float, cy: Float, maxR: Float,
    activeZone: ReactorZone?, sweepAngle: Float
) {
    val IR = maxR * 0.645f
    val OR = maxR * 0.960f
    val ringW = OR - IR
    val GAP   = 10f

    ReactorZone.values().forEach { zone ->
        val isActive = activeZone?.name == zone.name
        val sa = zone.startDeg + GAP / 2f
        val sweep = (zone.endDeg - zone.startDeg) - GAP
        val midR = (IR + OR) / 2f

        // Fill arc
        drawArc(
            brush = Brush.radialGradient(
                listOf(zone.color.copy(alpha = if (isActive) 0.55f else 0.18f), Color.Transparent),
                center = Offset(cx, cy), radius = OR
            ),
            startAngle = sa, sweepAngle = sweep, useCenter = false,
            topLeft = Offset(cx - OR, cy - OR), size = Size(OR * 2, OR * 2),
            style   = Stroke(ringW)
        )
        // Border stroke
        val strokeAlpha = if (isActive) 1f else 0.6f
        drawArc(
            color = zone.color.copy(alpha = strokeAlpha),
            startAngle = sa, sweepAngle = sweep, useCenter = false,
            topLeft = Offset(cx - OR, cy - OR), size = Size(OR * 2, OR * 2),
            style   = Stroke(if (isActive) 3f else 2f)
        )
        drawArc(
            color = zone.color.copy(alpha = strokeAlpha),
            startAngle = sa, sweepAngle = sweep, useCenter = false,
            topLeft = Offset(cx - IR, cy - IR), size = Size(IR * 2, IR * 2),
            style   = Stroke(if (isActive) 3f else 2f)
        )

        // Flowing light sweep
        val sweepPos = (sweepAngle + ReactorZone.values().indexOf(zone) * 90f) % 360f
        if (sweepPos > sa && sweepPos < sa + sweep) {
            drawArc(
                color = zone.color,
                startAngle = sweepPos - 5f, sweepAngle = 10f, useCenter = false,
                topLeft = Offset(cx - OR, cy - OR), size = Size(OR * 2, OR * 2),
                style   = Stroke(5f)
            )
        }

        // Zone icon (text drawn via custom approach)
        val midAngle = Math.toRadians((sa + sweep / 2).toDouble())
        val iconX    = cx + cos(midAngle).toFloat() * midR
        val iconY    = cy + sin(midAngle).toFloat() * midR
        drawCircle(
            color = zone.color.copy(alpha = if (isActive) 0.4f else 0.2f),
            radius = maxR * 0.06f,
            center = Offset(iconX, iconY)
        )
    }
}

// ── Draw: Inner Bezel ────────────────────────────────────────────────────────
private fun DrawScope.drawInnerBezel(cx: Float, cy: Float, maxR: Float) {
    val IR = maxR * 0.635f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF05080F), Color(0xFF080C16), Color(0xFF0F1423)),
            center = Offset(cx, cy), radius = IR
        ),
        center = Offset(cx, cy), radius = IR
    )
    drawCircle(
        color = Color(0x5528406E),
        center = Offset(cx, cy), radius = IR,
        style  = Stroke(2f)
    )
    for (i in 0 until 36) {
        val a    = Math.toRadians((i * 10).toDouble())
        val r1   = if (i % 3 == 0) IR * 0.950f else IR * 0.970f
        val alpha = if (i % 3 == 0) 0.5f else 0.3f
        val col  = if (i % 3 == 0) Color(0xFF00D4FF).copy(alpha = alpha)
                   else Color(0xFF283C50).copy(alpha = alpha)
        drawLine(
            color  = col,
            start  = Offset(cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1),
            end    = Offset(cx + cos(a).toFloat() * IR, cy + sin(a).toFloat() * IR),
            strokeWidth = if (i % 3 == 0) 1.5f else 1f
        )
    }
}

// ── Draw: Core Node ──────────────────────────────────────────────────────────
private fun DrawScope.drawCoreNode(
    cx: Float, cy: Float, maxR: Float,
    pulse: Float, coreActive: Boolean, rimPulse: Float
) {
    val p    = (sin(pulse) * 0.5f + 0.5f)
    val bpm  = (sin(pulse * 1.3f) * 0.5f + 0.5f)
    val baseR = maxR * (if (coreActive) 0.30f else 0.27f)
    val R    = baseR + p * maxR * 0.015f

    // Deep glow
    drawCircle(
        brush  = Brush.radialGradient(
            listOf(
                Color(0xFFFF7800).copy(alpha = 0.25f + p * 0.15f),
                Color(0xFFFF5000).copy(alpha = 0.10f + p * 0.08f),
                Color.Transparent
            ),
            center = Offset(cx, cy), radius = maxR * 0.58f
        ),
        center = Offset(cx, cy), radius = maxR * 0.58f
    )

    // Concentric pulse rings
    listOf(
        Triple(1.8f, Color(0xFF00AAFF), 0.35f),
        Triple(1.4f, Color(0xFF00FF44), 0.25f),
        Triple(1.1f, Color(0xFFFF9500), 0.20f),
    ).forEach { (scale, col, baseA) ->
        drawCircle(
            color  = col.copy(alpha = (baseA - p * 0.1f).coerceAtLeast(0.05f)),
            center = Offset(cx, cy),
            radius = R * scale,
            style  = Stroke(1.5f)
        )
    }

    // Inset cavity
    drawCircle(
        brush  = Brush.radialGradient(
            listOf(Color(0xFF0A0C14), Color(0xFF080A10), Color(0xFF050608)),
            center = Offset(cx, cy), radius = R
        ),
        center = Offset(cx, cy), radius = R
    )
    // Core glow border
    drawCircle(
        color  = Color(0xFFFF6600).copy(alpha = 0.6f + bpm * 0.3f),
        center = Offset(cx, cy),
        radius = R,
        style  = Stroke(3f)
    )

    // N-Chip housing
    val chipS = R * 0.64f
    val chipTL = Offset(cx - chipS / 2f, cy - chipS / 2f)
    // Chip shadow glow
    drawRoundRect(
        color     = Color(0xFFFF4400).copy(alpha = 0.35f + p * 0.2f),
        topLeft   = chipTL - Offset(4f, 4f),
        size      = Size(chipS + 8f, chipS + 8f),
        cornerRadius = CornerRadius(8f)
    )
    // Chip body
    drawRoundRect(
        brush    = Brush.linearGradient(
            listOf(Color(0xFF2A0A00), Color(0xFF3D1200), Color(0xFF1F0800)),
            start = chipTL, end = Offset(chipTL.x + chipS, chipTL.y + chipS)
        ),
        topLeft  = chipTL,
        size     = Size(chipS, chipS),
        cornerRadius = CornerRadius(6f)
    )
    drawRoundRect(
        color    = Color(0xFFFF6600).copy(alpha = 0.8f + p * 0.2f),
        topLeft  = chipTL,
        size     = Size(chipS, chipS),
        cornerRadius = CornerRadius(6f),
        style    = Stroke(2.5f)
    )

    // PCB mini lines inside chip
    val lineColors = listOf(Color(0xFF00FF44),Color(0xFF00AAFF),Color(0xFFFF9500),Color(0xFFFF00CC))
    listOf(
        Pair(Offset(cx-chipS*0.45f, cy-chipS*0.15f), Offset(cx-chipS*0.10f, cy-chipS*0.15f)),
        Pair(Offset(cx+chipS*0.10f, cy-chipS*0.15f), Offset(cx+chipS*0.45f, cy-chipS*0.15f)),
        Pair(Offset(cx-chipS*0.45f, cy+chipS*0.15f), Offset(cx-chipS*0.10f, cy+chipS*0.15f)),
        Pair(Offset(cx+chipS*0.10f, cy+chipS*0.15f), Offset(cx+chipS*0.45f, cy+chipS*0.15f)),
    ).forEachIndexed { i, (s, e) ->
        drawLine(lineColors[i].copy(alpha = 0.5f), s, e, 1.2f)
    }

    // Chip pins
    for (i in 0..3) {
        val px = chipTL.x + chipS * (0.15f + i * 0.22f)
        drawLine(lineColors[i].copy(alpha=0.7f), Offset(px,chipTL.y-3f), Offset(px,chipTL.y-9f), 1.5f)
        drawLine(lineColors[i].copy(alpha=0.7f), Offset(px,chipTL.y+chipS+3f), Offset(px,chipTL.y+chipS+9f), 1.5f)
    }
}

// ── Draw: Glass Overlay ───────────────────────────────────────────────────────
private fun DrawScope.drawGlassOverlay(cx: Float, cy: Float, maxR: Float) {
    // Convex sheen
    drawCircle(
        brush  = Brush.linearGradient(
            listOf(Color.White.copy(0.07f), Color.White.copy(0.03f), Color.Transparent),
            start = Offset(cx - maxR * 0.66f, cy - maxR * 0.73f),
            end   = Offset(cx + maxR * 0.33f, cy + maxR * 0.27f)
        ),
        center = Offset(cx, cy), radius = maxR
    )
    // Smudge arc
    drawArc(
        color = Color.White.copy(0.04f),
        startAngle = -155f, sweepAngle = 55f, useCenter = false,
        topLeft = Offset(cx - maxR * 0.6f, cy - maxR * 0.6f),
        size = Size(maxR * 1.2f, maxR * 1.2f),
        style = Stroke(18f, cap = StrokeCap.Round)
    )
    // Rim vignette
    drawCircle(
        brush  = Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(0.5f)),
            center = Offset(cx, cy), radius = maxR
        ),
        center = Offset(cx, cy), radius = maxR
    )
}

// ── Draw: Ripple ─────────────────────────────────────────────────────────────
private fun DrawScope.drawRipple(pos: Offset, color: Color, progress: Float, maxR: Float) {
    val r = progress * maxR * 0.28f + 10f
    drawCircle(
        color  = color.copy(alpha = (1f - progress) * 0.85f),
        center = pos, radius = r,
        style  = Stroke(2.5f)
    )
}

// ── Ripple State ─────────────────────────────────────────────────────────────
data class RippleState(val pos: Offset, val color: Color)

// ── Helper ────────────────────────────────────────────────────────────────────
private fun angleInRange(angle: Float, start: Float, end: Float): Boolean {
    val norm = { d: Float -> ((d % 360f) + 360f) % 360f }
    val a = norm(angle); val s = norm(start); val e = norm(end)
    return if (s < e) a >= s && a < e else a >= s || a < e
}

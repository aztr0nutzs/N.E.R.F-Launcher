// ReactorDrawing.kt
package com.nerf.launcher.reactor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.*

fun DrawScope.drawOuterRing(
    center: Offset,
    radius: Float,
    phase: Float,
    colors: List<Color>,
    mode: ReactorMode,
    alertPulseAlpha: Float
) {
    val strokeWidth = radius * 0.10f
    val baseAlpha = when (mode) {
        ReactorMode.Idle -> 0.65f
        ReactorMode.Active -> 0.9f
        ReactorMode.Alert -> 0.9f * alertPulseAlpha
        ReactorMode.Overdrive -> 1f
    }

    val gradient = SweepGradientShader(
        center = center,
        colors = colors.map { it.copy(alpha = baseAlpha) },
        colorStops = null
    )

    val ringBrush = Brush.shader(gradient)

    val rect = Rect(
        center = center,
        radius = radius - strokeWidth / 2f
    )

    rotate(phase, center) {
        // base ring
        drawArc(
            brush = ringBrush,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // mechanical segments (gaps)
        val segCount = 8
        val segSweep = 18f
        val gapSweep = 360f / segCount - segSweep
        val segmentStroke = strokeWidth * 1.4f
        val segmentRadius = radius - strokeWidth * 0.25f

        repeat(segCount) { index ->
            val start = index * (segSweep + gapSweep)
            drawArc(
                color = Color(0xFF11131A),
                startAngle = start,
                sweepAngle = segSweep,
                useCenter = false,
                topLeft = Offset(
                    center.x - segmentRadius,
                    center.y - segmentRadius
                ),
                size = Size(segmentRadius * 2, segmentRadius * 2),
                style = Stroke(width = segmentStroke, cap = StrokeCap.Round)
            )
        }
    }
}

fun DrawScope.drawMidEnergyRing(
    center: Offset,
    radius: Float,
    phase: Float,
    colors: List<Color>,
    flowPhase: Float
) {
    val strokeWidth = radius * 0.16f
    val rect = Rect(center, radius - strokeWidth / 2f)

    val channelCount = colors.size
    val sweepPerChannel = 360f / channelCount

    repeat(channelCount) { i ->
        val c = colors[i]
        val startAngle = i * sweepPerChannel + phase * 0.4f
        // Create flowing gradient: bright head, faded tail
        val headPos = (flowPhase + i * 0.25f) % 1f
        val brightSweep = sweepPerChannel * 0.55f
        val dimSweep = sweepPerChannel - brightSweep

        // bright leading arc
        drawArc(
            color = c.copy(alpha = 0.95f),
            startAngle = startAngle + headPos * sweepPerChannel,
            sweepAngle = brightSweep,
            useCenter = false,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // trailing dim arc
        drawArc(
            color = c.copy(alpha = 0.35f),
            startAngle = startAngle + headPos * sweepPerChannel + brightSweep,
            sweepAngle = dimSweep,
            useCenter = false,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
        )
    }

    // subtle radial grid
    repeat(6) { i ->
        val angle = i * 60f + phase * 0.3f
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val inner = Offset(
            x = center.x + cos(rad) * (radius - strokeWidth * 0.8f),
            y = center.y + sin(rad) * (radius - strokeWidth * 0.8f)
        )
        val outer = Offset(
            x = center.x + cos(rad) * (radius + strokeWidth * 0.8f),
            y = center.y + sin(rad) * (radius + strokeWidth * 0.8f)
        )
        drawLine(
            color = Color(0x55FFFFFF),
            start = inner,
            end = outer,
            strokeWidth = strokeWidth * 0.05f
        )
    }
}

fun DrawScope.drawAura(
    center: Offset,
    radius: Float,
    mode: ReactorMode,
    coreGlowAlpha: Float,
    alertPulseAlpha: Float
) {
    val auraColor = when (mode) {
        ReactorMode.Idle -> Color(0xFF00D1FF)
        ReactorMode.Active -> Color(0xFF00FFE3)
        ReactorMode.Alert -> Color(0xFFFF5A2C)
        ReactorMode.Overdrive -> Color(0xFF00F7FF)
    }

    val alpha = when (mode) {
        ReactorMode.Idle -> 0.18f * coreGlowAlpha
        ReactorMode.Active -> 0.26f * coreGlowAlpha
        ReactorMode.Alert -> 0.35f * alertPulseAlpha
        ReactorMode.Overdrive -> 0.42f
    }

    val radialBrush = Brush.radialGradient(
        colors = listOf(
            auraColor.copy(alpha = alpha),
            auraColor.copy(alpha = 0f)
        ),
        center = center,
        radius = radius
    )

    drawCircle(
        brush = radialBrush,
        radius = radius
    )

    // Arc‑like plasma streaks
    val streakCount = 4
    val streakRadius = radius * 0.92f
    val stroke = radius * 0.08f
    repeat(streakCount) { idx ->
        val baseAngle = 45f + idx * 90f
        drawArc(
            color = auraColor.copy(alpha = alpha * 1.5f),
            startAngle = baseAngle - 18f,
            sweepAngle = 36f,
            useCenter = false,
            topLeft = Offset(
                center.x - streakRadius,
                center.y - streakRadius
            ),
            size = Size(streakRadius * 2, streakRadius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

fun DrawScope.drawCore(
    center: Offset,
    radius: Float,
    rotation: Float,
    glowAlpha: Float,
    mode: ReactorMode,
    textMeasurer: TextMeasurer
) {
    // Inner glow discs
    val innerBrush = Brush.radialGradient(
        colors = listOf(
            when (mode) {
                ReactorMode.Idle -> Color(0xFFFFD93B)
                ReactorMode.Active -> Color(0xFFFFFF6B)
                ReactorMode.Alert -> Color(0xFFFF8C42)
                ReactorMode.Overdrive -> Color(0xFFFFFF9C)
            }.copy(alpha = glowAlpha),
            Color.Black
        ),
        center = center,
        radius = radius * 1.1f
    )

    drawCircle(
        brush = innerBrush,
        radius = radius
    )

    // Inner rotating ring
    val ringRadius = radius * 0.86f
    val rect = Rect(center, ringRadius)
    rotate(rotation, center) {
        drawArc(
            color = Color(0x55FFFFFF),
            startAngle = 0f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(
                width = radius * 0.18f,
                cap = StrokeCap.Round
            )
        )
    }

    // Central logo "N"
    val logoText = "N"
    val logoMeasure = textMeasurer.measure(
        text = logoText,
        style = androidx.compose.ui.text.TextStyle(
            color = Color(0xFF222222),
            fontSize = (radius * 0.95f).sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black
        )
    )

    val logoOffset = Offset(
        center.x - logoMeasure.size.width / 2f,
        center.y - logoMeasure.size.height / 1.75f
    )

    drawText(
        textLayoutResult = logoMeasure,
        topLeft = logoOffset
    )

    // "Core" label
    val coreMeasure = textMeasurer.measure(
        text = "Core",
        style = androidx.compose.ui.text.TextStyle(
            color = Color.White.copy(alpha = 0.85f),
            fontSize = (radius * 0.26f).sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    )
    drawText(
        textLayoutResult = coreMeasure,
        topLeft = Offset(
            center.x - coreMeasure.size.width / 2f,
            center.y + radius * 0.35f
        )
    )
}

fun DrawScope.drawHudLabels(
    center: Offset,
    radius: Float,
    mode: ReactorMode,
    textMeasurer: TextMeasurer
) {
    val labelStyle = androidx.compose.ui.text.TextStyle(
        fontSize = (radius * 0.16f).sp,
        color = Color(0xFFB6F4FF),
        textAlign = TextAlign.Center
    )

    // bottom CPU
    val cpu = textMeasurer.measure(
        text = "CPU",
        style = labelStyle
    )
    drawText(
        cpu,
        topLeft = Offset(
            center.x - cpu.size.width / 2f,
            center.y + radius * 0.32f
        )
    )

    // ping text along bottom arc
    val pingText = when (mode) {
        ReactorMode.Idle -> "PING: 12 ms"
        ReactorMode.Active -> "PING: 9 ms"
        ReactorMode.Alert -> "PING: 45 ms"
        ReactorMode.Overdrive -> "PING: 3 ms"
    }
    val ping = textMeasurer.measure(
        text = pingText,
        style = labelStyle.copy(fontSize = (radius * 0.14f).sp)
    )
    drawText(
        ping,
        topLeft = Offset(
            center.x - ping.size.width / 2f,
            center.y + radius * 0.12f
        )
    )

    // top bandwidth label
    val bwText = "DOWNLOAD • SSU Nitro"
    val bw = textMeasurer.measure(
        text = bwText,
        style = labelStyle.copy(fontSize = (radius * 0.14f).sp)
    )
    drawText(
        bw,
        topLeft = Offset(
            center.x - bw.size.width / 2f,
            center.y - radius * 0.52f
        )
    )
}

private fun Rect(center: Offset, radius: Float): Rect =
    Rect(
        center.x - radius,
        center.y - radius,
        center.x + radius,
        center.y + radius
    )
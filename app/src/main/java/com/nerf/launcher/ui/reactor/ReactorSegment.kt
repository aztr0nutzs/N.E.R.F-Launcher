package com.nerf.launcher.ui.reactor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal data class ReactorSegmentLayout(
    val model: ReactorSegmentModel,
    val startAngle: Float,
    val sweepAngle: Float,
    val innerRadiusPx: Float,
    val outerRadiusPx: Float,
    val labelOffset: Offset
) {
    fun contains(point: Offset, center: Offset): Boolean {
        val dx = point.x - center.x
        val dy = point.y - center.y
        val radius = hypot(dx, dy)
        if (radius < innerRadiusPx || radius > outerRadiusPx) {
            return false
        }
        val angle = normalizeAngle(Math.toDegrees(atan2(dy, dx).toDouble()).toFloat())
        return isAngleInSweep(angle = angle, startAngle = startAngle, sweepAngle = sweepAngle)
    }
}

internal fun calculateSegmentLayouts(
    reactor: ReactorModel,
    outerRadiusPx: Float,
    ringThicknessPx: Float,
    labelRadiusPx: Float
): List<ReactorSegmentLayout> {
    val segments = reactor.segments
    if (segments.isEmpty()) return emptyList()

    val gap = reactor.segmentGapAngle.coerceIn(0f, 24f)
    val availableSweep = (360f - gap * segments.size).coerceAtLeast(40f)
    val segmentSweep = availableSweep / segments.size
    val innerRadius = (outerRadiusPx - ringThicknessPx).coerceAtLeast(0f)

    return segments.mapIndexed { index, segment ->
        val start = reactor.startAngle + index * (segmentSweep + gap)
        val midAngle = Math.toRadians((start + segmentSweep / 2f).toDouble())
        ReactorSegmentLayout(
            model = segment,
            startAngle = start,
            sweepAngle = segmentSweep,
            innerRadiusPx = innerRadius,
            outerRadiusPx = outerRadiusPx,
            labelOffset = Offset(
                x = (cos(midAngle) * labelRadiusPx).toFloat(),
                y = (sin(midAngle) * labelRadiusPx).toFloat()
            )
        )
    }
}

internal fun DrawScope.drawReactorSegment(
    layout: ReactorSegmentLayout,
    palette: ReactorPalette,
    glowBreathScale: Float,
    isPressed: Boolean = false
) {
    val diameter = layout.outerRadiusPx * 2f
    val topLeft = Offset(center.x - layout.outerRadiusPx, center.y - layout.outerRadiusPx)
    val size = Size(diameter, diameter)
    val strokeWidth = layout.outerRadiusPx - layout.innerRadiusPx
    val accentColor = palette.accentColor(layout.model.accent)
    val trackColor = palette.ringTrack.copy(
        alpha = when {
            isPressed -> 0.44f
            layout.model.isActive -> 0.36f
            else -> 0.22f
        }
    )
    val glowAlpha = when {
        isPressed -> 0.36f * glowBreathScale
        layout.model.isActive -> 0.24f * glowBreathScale
        else -> 0.08f
    }
    val mainColor = when {
        isPressed -> accentColor.copy(alpha = 0.98f)
        layout.model.isActive -> accentColor
        else -> palette.ringInactive
    }

    drawArc(
        color = trackColor,
        startAngle = layout.startAngle,
        sweepAngle = layout.sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth)
    )
    drawArc(
        color = accentColor.copy(alpha = glowAlpha),
        startAngle = layout.startAngle,
        sweepAngle = layout.sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth * 1.42f)
    )
    drawArc(
        color = mainColor,
        startAngle = layout.startAngle,
        sweepAngle = layout.sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth * if (isPressed) 0.92f else 0.84f)
    )
    drawArc(
        color = Color.White.copy(
            alpha = when {
                isPressed -> 0.34f
                layout.model.isActive -> 0.24f
                else -> 0.08f
            }
        ),
        startAngle = layout.startAngle + 1.5f,
        sweepAngle = (layout.sweepAngle - 3f).coerceAtLeast(1f),
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth * 0.12f)
    )
}

@Composable
internal fun ReactorSegmentLabel(
    layout: ReactorSegmentLayout,
    palette: ReactorPalette,
    isPressed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val background = if (isPressed) {
        palette.accentColor(layout.model.accent).copy(alpha = 0.26f)
    } else if (layout.model.isActive) {
        palette.accentColor(layout.model.accent).copy(alpha = 0.18f)
    } else {
        palette.chassisBase.copy(alpha = 0.78f)
    }
    val border = if (isPressed) {
        palette.accentColor(layout.model.accent)
    } else if (layout.model.isActive) {
        palette.accentColor(layout.model.accent).copy(alpha = 0.72f)
    } else {
        palette.chassisLine.copy(alpha = 0.52f)
    }
    val textColor = if (isPressed || layout.model.isActive) palette.textPrimary else palette.textSecondary

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = layout.labelOffset.x
                translationY = layout.labelOffset.y
                scaleX = if (isPressed) 1.04f else 1f
                scaleY = if (isPressed) 1.04f else 1f
            }
            .background(background, CutCornerShape(8.dp))
            .border(1.dp, border, CutCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = layout.model.label,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

private fun normalizeAngle(angle: Float): Float {
    val normalized = angle % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

private fun isAngleInSweep(
    angle: Float,
    startAngle: Float,
    sweepAngle: Float
): Boolean {
    val start = normalizeAngle(startAngle)
    val end = normalizeAngle(startAngle + sweepAngle)
    return if (start <= end) {
        angle in start..end
    } else {
        angle >= start || angle <= end
    }
}

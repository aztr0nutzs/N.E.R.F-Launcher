package com.nerf.launcher.ui.reactor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReactorCore(
    model: ReactorCoreModel,
    modifier: Modifier = Modifier,
    palette: ReactorPalette = rememberIndustrialReactorPalette(),
    glowBreathScale: Float = 1f,
    pulseScale: Float = 1f,
    isPressed: Boolean = false
) {
    val accentColor = palette.accentColor(model.accent)
    val glowAlpha = when {
        isPressed -> 0.34f * glowBreathScale
        model.isOnline -> 0.22f * glowBreathScale
        else -> 0.08f
    }
    val coreScale = if (isPressed) pulseScale * 0.97f else pulseScale
    val borderAlpha = if (isPressed) 0.82f else 0.58f
    val ringAlpha = if (isPressed) 0.84f else 0.65f

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = coreScale
                scaleY = coreScale
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(10.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = glowAlpha))
                .blur(22.dp)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.coreShell,
                            palette.chassisShadow
                        )
                    )
                )
                .border(2.dp, accentColor.copy(alpha = borderAlpha), CircleShape)
        )

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .padding(12.dp)
        ) {
            drawCircle(
                color = palette.coreInner,
                radius = size.minDimension * 0.5f
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.16f * glowBreathScale),
                radius = size.minDimension * 0.42f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.64f),
                        palette.coreCenter,
                        Color.Transparent
                    )
                ),
                radius = size.minDimension * 0.32f
            )
            drawCircle(
                color = accentColor.copy(alpha = ringAlpha),
                radius = size.minDimension * 0.44f,
                style = Stroke(width = size.minDimension * 0.038f)
            )
            repeat(6) { index ->
                drawArc(
                    color = accentColor.copy(alpha = 0.34f),
                    startAngle = -90f + index * 60f + 8f,
                    sweepAngle = 24f,
                    useCenter = false,
                    style = Stroke(width = size.minDimension * 0.02f)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(CutCornerShape(10.dp))
                .background(palette.coreCenter.copy(alpha = 0.76f))
                .border(1.dp, accentColor.copy(alpha = 0.44f), CutCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = model.title,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = model.subtitle,
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = model.status,
                color = if (model.isOnline) palette.textSecondary else accentColor.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
        }
    }
}

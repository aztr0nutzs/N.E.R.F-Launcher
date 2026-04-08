package com.nerf.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.theme.LauncherTheme

@Composable
fun MetalBackdrop(
    modifier: Modifier = Modifier,
    accent: LauncherAccent = LauncherAccent.Cyan,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val colors = LauncherTheme.colors
    val tokens = LauncherTheme.tokens
    val accentColor = colors.accent(accent)
    val density = LocalDensity.current
    val verticalSpacingPx = with(density) { 18.dp.toPx() }
    val horizontalSpacingPx = with(density) { 72.dp.toPx() }
    val strokePx = with(density) { tokens.strokes.hairline.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBottom)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.backgroundTop, colors.backgroundBottom)
                )
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.metalHighlight.copy(alpha = 0.18f),
                        Color.Transparent,
                        colors.metalDark.copy(alpha = 0.68f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )

            var x = 0f
            var stripeIndex = 0
            while (x <= size.width) {
                val alpha = if (stripeIndex % 5 == 0) 0.12f else 0.05f
                drawLine(
                    color = colors.frameLine.copy(alpha = alpha),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokePx
                )
                x += verticalSpacingPx
                stripeIndex++
            }

            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = colors.metalHighlight.copy(alpha = 0.045f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokePx
                )
                y += horizontalSpacingPx
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.22f),
                    radius = size.minDimension * 0.58f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.accentYellow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.78f),
                    radius = size.minDimension * 0.44f
                )
            )
            drawRect(
                color = colors.backgroundScrim.copy(alpha = 0.38f),
                style = Stroke(width = strokePx * 2.5f)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        colors.backgroundBottom.copy(alpha = 0.34f),
                        colors.backgroundBottom.copy(alpha = 0.78f)
                    ),
                    startY = size.height * 0.42f,
                    endY = size.height
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
        }

        content()
    }
}

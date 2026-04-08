package com.nerf.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.theme.LauncherGlowLevel
import com.nerf.launcher.theme.LauncherTheme

@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    accent: LauncherAccent = LauncherAccent.Cyan,
    glowLevel: LauncherGlowLevel = LauncherGlowLevel.Low,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LauncherTheme.colors
    val tokens = LauncherTheme.tokens
    val accentColor = colors.accent(accent)
    val outerShape = CutCornerShape(tokens.shapes.panelChamfer)
    val innerShape = CutCornerShape(tokens.shapes.innerChamfer)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(tokens.spacing.sm)
                .clip(outerShape)
                .background(accentColor.copy(alpha = 0.10f))
                .blur(tokens.glow.radius(glowLevel))
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(outerShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.panelOuter, colors.metalDark)
                    )
                )
                .border(tokens.strokes.strong, accentColor.copy(alpha = 0.58f), outerShape)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(tokens.spacing.panelInset)
                .clip(innerShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.panelInner, colors.panelInset)
                    )
                )
                .border(tokens.strokes.hairline, colors.frameLine.copy(alpha = 0.42f), innerShape)
        )

        Box(
            modifier = Modifier
                .padding(start = tokens.spacing.lg, top = tokens.spacing.sm)
                .fillMaxWidth(0.26f)
                .clip(CutCornerShape(tokens.shapes.microChamfer))
                .background(accentColor.copy(alpha = 0.92f))
                .padding(vertical = tokens.strokes.hairline)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = content
        )
    }
}

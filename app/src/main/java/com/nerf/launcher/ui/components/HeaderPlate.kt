package com.nerf.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.theme.LauncherTheme

@Composable
fun HeaderPlate(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    accent: LauncherAccent = LauncherAccent.Cyan,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val colors = LauncherTheme.colors
    val tokens = LauncherTheme.tokens
    val typography = LauncherTheme.typography
    val accentColor = colors.accent(accent)
    val outerShape = CutCornerShape(tokens.shapes.plateChamfer)
    val innerShape = CutCornerShape(tokens.shapes.innerChamfer)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(tokens.spacing.xs)
                .clip(outerShape)
                .background(accentColor.copy(alpha = 0.12f))
                .blur(tokens.glow.medium)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(outerShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(colors.metalDark, colors.frameBase, colors.metalMid)
                    )
                )
                .border(tokens.strokes.strong, accentColor.copy(alpha = 0.62f), outerShape)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(tokens.spacing.panelInset)
                .clip(innerShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.panelOuter, colors.panelInner)
                    )
                )
                .border(tokens.strokes.hairline, colors.frameLine.copy(alpha = 0.46f), innerShape)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(tokens.strokes.heavy * 3)
                    .fillMaxHeight(0.78f)
                    .clip(CutCornerShape(tokens.shapes.microChamfer))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.15f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = tokens.spacing.md, end = tokens.spacing.md)
            ) {
                eyebrow?.let {
                    Text(
                        text = it,
                        style = typography.micro,
                        color = accentColor.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = typography.header,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = typography.body,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = trailingContent
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = tokens.spacing.sm, end = tokens.spacing.lg)
                .size(width = tokens.reactor.coreDiameter * 0.34f, height = tokens.strokes.strong)
                .clip(CutCornerShape(tokens.shapes.microChamfer))
                .background(accentColor.copy(alpha = 0.88f))
        )
    }
}

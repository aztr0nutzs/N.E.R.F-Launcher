package com.nerf.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.theme.LauncherGlowLevel
import com.nerf.launcher.theme.LauncherTheme

@Composable
fun ChromeFrame(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    accent: LauncherAccent = LauncherAccent.Cyan,
    topBarContent: @Composable RowScope.() -> Unit = {},
    dockContent: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LauncherTheme.colors
    val tokens = LauncherTheme.tokens
    val accentColor = colors.accent(accent)
    val shellShape = CutCornerShape(tokens.shapes.frameChamfer)

    MetalBackdrop(
        modifier = modifier.fillMaxSize(),
        accent = accent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(tokens.spacing.screenPadding)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(tokens.spacing.sm)
                    .clip(shellShape)
                    .background(accentColor.copy(alpha = 0.10f))
                    .blur(tokens.glow.high)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shellShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(colors.frameBase, colors.panelInset)
                        )
                    )
                    .border(tokens.strokes.strong, accentColor.copy(alpha = 0.54f), shellShape)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(tokens.spacing.xs)
                    .clip(CutCornerShape(tokens.shapes.panelChamfer))
                    .border(
                        tokens.strokes.hairline,
                        colors.frameLine.copy(alpha = 0.55f),
                        CutCornerShape(tokens.shapes.panelChamfer)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md)
            ) {
                HeaderPlate(
                    title = title,
                    subtitle = subtitle,
                    eyebrow = eyebrow,
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = tokens.reactor.headerPlateHeight),
                    trailingContent = topBarContent
                )

                Spacer(Modifier.height(tokens.spacing.lg))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CutCornerShape(tokens.shapes.panelChamfer))
                            .background(colors.backgroundScrim.copy(alpha = 0.38f))
                            .border(
                                tokens.strokes.fine,
                                colors.frameLine.copy(alpha = 0.28f),
                                CutCornerShape(tokens.shapes.panelChamfer)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(tokens.spacing.sm),
                        contentAlignment = Alignment.Center,
                        content = content
                    )
                }

                Spacer(Modifier.height(tokens.spacing.lg))

                DockRegion(
                    accent = accent,
                    modifier = Modifier.fillMaxWidth(),
                    content = dockContent
                )
            }
        }
    }
}

@Composable
private fun DockRegion(
    accent: LauncherAccent,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LauncherTheme.colors
    val tokens = LauncherTheme.tokens
    val accentColor = colors.accent(accent)
    val dockShape = CutCornerShape(tokens.shapes.panelChamfer)

    Box(modifier = modifier.defaultMinSize(minHeight = tokens.reactor.dockRegionHeight)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.xs)
                .clip(dockShape)
                .background(accentColor.copy(alpha = 0.10f))
                .blur(tokens.glow.radius(LauncherGlowLevel.Medium))
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(dockShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.dockBase, colors.dockInner)
                    )
                )
                .border(tokens.strokes.strong, accentColor.copy(alpha = 0.52f), dockShape)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(tokens.spacing.panelInset)
                .clip(CutCornerShape(tokens.shapes.innerChamfer))
                .background(colors.panelInset.copy(alpha = 0.72f))
                .border(
                    tokens.strokes.hairline,
                    colors.frameLine.copy(alpha = 0.48f),
                    CutCornerShape(tokens.shapes.innerChamfer)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

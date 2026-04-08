package com.nerf.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.theme.LauncherGlowLevel
import com.nerf.launcher.theme.LauncherTheme

@Composable
fun DockTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    accent: LauncherAccent = LauncherAccent.Cyan,
    supportingText: String? = null,
    icon: @Composable BoxScope.() -> Unit
) {
    val colors = LauncherTheme.colors
    val tokens = LauncherTheme.tokens
    val typography = LauncherTheme.typography
    val accentColor = colors.accent(accent)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val glowLevel = when {
        pressed -> LauncherGlowLevel.High
        selected -> LauncherGlowLevel.Medium
        else -> LauncherGlowLevel.Low
    }
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabled
            pressed -> accentColor
            selected -> accentColor.copy(alpha = 0.92f)
            else -> colors.frameLine.copy(alpha = 0.92f)
        },
        label = "dockTileBorder"
    )
    val glowColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabled.copy(alpha = 0.10f)
            pressed -> accentColor.copy(alpha = 0.26f)
            selected -> accentColor.copy(alpha = 0.18f)
            else -> accentColor.copy(alpha = 0.08f)
        },
        label = "dockTileGlow"
    )
    val surfaceBrush = when {
        pressed -> Brush.verticalGradient(listOf(colors.metalMid, colors.panelInner))
        selected -> Brush.verticalGradient(listOf(colors.panelOuter, colors.panelInset))
        else -> Brush.verticalGradient(listOf(colors.dockBase, colors.dockInner))
    }
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.97f
            selected -> 1.02f
            else -> 1f
        },
        label = "dockTileScale"
    )
    val glowRadius by animateDpAsState(
        targetValue = tokens.glow.radius(glowLevel),
        label = "dockTileGlowRadius"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.textMuted
            selected || pressed -> colors.textPrimary
            else -> colors.textSecondary
        },
        label = "dockTileText"
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            !enabled -> colors.textMuted
            selected || pressed -> accentColor
            else -> colors.textPrimary
        },
        label = "dockTileIconTint"
    )
    val shape = CutCornerShape(tokens.shapes.tileChamfer)
    val innerShape = CutCornerShape(tokens.shapes.innerChamfer)

    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = tokens.reactor.dockTileSize,
                minHeight = tokens.reactor.dockTileSize
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(6.dp)
                .clip(shape)
                .background(glowColor)
                .blur(glowRadius)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(surfaceBrush)
                .border(tokens.strokes.strong, borderColor, shape)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(tokens.spacing.panelInset)
                .clip(innerShape)
                .background(colors.panelInset.copy(alpha = if (selected) 0.92f else 0.82f))
                .border(tokens.strokes.hairline, colors.frameLine.copy(alpha = 0.46f), innerShape)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalContentColor provides iconTint) {
                Box(
                    modifier = Modifier.size(tokens.reactor.coreRingThickness * 3),
                    contentAlignment = Alignment.Center,
                    content = icon
                )
            }

            Spacer(Modifier.height(tokens.spacing.sm))

            Text(
                text = label,
                style = typography.label,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            supportingText?.let {
                Spacer(Modifier.height(tokens.spacing.xs))
                Text(
                    text = it,
                    style = typography.micro,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

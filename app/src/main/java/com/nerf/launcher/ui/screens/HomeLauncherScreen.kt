package com.nerf.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nerf.launcher.state.LauncherDockItem
import com.nerf.launcher.state.LauncherMode
import com.nerf.launcher.state.LauncherStatusModule
import com.nerf.launcher.state.LauncherUiState
import com.nerf.launcher.state.LauncherUtilityAction
import com.nerf.launcher.state.LauncherViewModel
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.theme.LauncherGlowLevel
import com.nerf.launcher.ui.components.ChromeFrame
import com.nerf.launcher.ui.components.DockTile
import com.nerf.launcher.ui.components.PanelCard
import com.nerf.launcher.ui.reactor.ReactorHapticEvent
import com.nerf.launcher.ui.reactor.ReactorInteractionState
import com.nerf.launcher.ui.reactor.NanoCoreReactor

@Composable
fun HomeLauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    HomeLauncherScreen(
        uiState = viewModel.uiState,
        onModeSelected = viewModel::selectMode,
        onDockItemTap = viewModel::onDockItemTap,
        onUtilityActionTap = viewModel::onUtilityActionTap,
        onReactorInteractionStateChange = viewModel::onReactorInteractionStateChange,
        onReactorCoreTap = viewModel::onReactorCoreTap,
        onReactorSegmentTap = viewModel::onReactorSegmentTap,
        onReactorSegmentLongPress = viewModel::onReactorSegmentLongPress,
        onReactorHapticEvent = { event ->
            when (event) {
                ReactorHapticEvent.CoreTap,
                ReactorHapticEvent.SegmentTap -> {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                ReactorHapticEvent.SegmentLongPress -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun HomeLauncherScreen(
    uiState: LauncherUiState,
    onModeSelected: (LauncherMode) -> Unit,
    onDockItemTap: (LauncherMode) -> Unit,
    onUtilityActionTap: (LauncherUtilityAction) -> Unit,
    onReactorInteractionStateChange: (ReactorInteractionState) -> Unit,
    onReactorCoreTap: () -> Unit,
    onReactorSegmentTap: (String) -> Unit,
    onReactorSegmentLongPress: (String) -> Unit,
    onReactorHapticEvent: (ReactorHapticEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ChromeFrame(
        title = uiState.headerTitle,
        subtitle = uiState.headerSubtitle,
        eyebrow = uiState.headerEyebrow,
        accent = uiState.selectedMode.accent,
        modifier = modifier,
        topBarContent = {
            ModeBadge(mode = uiState.selectedMode)
            Spacer(Modifier.width(12.dp))
            uiState.utilityActions.forEachIndexed { index, action ->
                UtilityPill(
                    action = action,
                    onClick = { onUtilityActionTap(action) }
                )
                if (index != uiState.utilityActions.lastIndex) {
                    Spacer(Modifier.width(10.dp))
                }
            }
        },
        dockContent = {
            uiState.dockItems.forEachIndexed { index, item ->
                DockTile(
                    label = item.label,
                    supportingText = item.supportingText,
                    accent = item.accent,
                    selected = item.mode == uiState.selectedMode,
                    onClick = { onDockItemTap(item.mode) },
                    modifier = Modifier.weight(1f)
                ) {
                    ReactorGlyph(
                        glyph = item.glyph,
                        accent = item.accent
                    )
                }
                if (index != uiState.dockItems.lastIndex) {
                    Spacer(Modifier.width(14.dp))
                }
            }
        }
    ) {
        HomeCommandDeck(
            uiState = uiState,
            onModeSelected = onModeSelected,
            onReactorInteractionStateChange = onReactorInteractionStateChange,
            onReactorCoreTap = onReactorCoreTap,
            onReactorSegmentTap = onReactorSegmentTap,
            onReactorSegmentLongPress = onReactorSegmentLongPress,
            onReactorHapticEvent = onReactorHapticEvent
        )
    }
}

@Composable
private fun BoxScope.HomeCommandDeck(
    uiState: LauncherUiState,
    onModeSelected: (LauncherMode) -> Unit,
    onReactorInteractionStateChange: (ReactorInteractionState) -> Unit,
    onReactorCoreTap: () -> Unit,
    onReactorSegmentTap: (String) -> Unit,
    onReactorSegmentLongPress: (String) -> Unit,
    onReactorHapticEvent: (ReactorHapticEvent) -> Unit
) {
    val tokens = LauncherTheme.tokens

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val wideLayout = maxWidth >= 980.dp

        if (wideLayout) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.lg)
            ) {
                Column(
                    modifier = Modifier.weight(0.28f),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
                ) {
                    StatusModuleCard(
                        module = uiState.statusModules[0],
                        selectedMode = uiState.selectedMode,
                        onClick = { onModeSelected(uiState.statusModules[0].mode) },
                        modifier = Modifier.weight(1f)
                    )
                    StatusModuleCard(
                        module = uiState.statusModules[1],
                        selectedMode = uiState.selectedMode,
                        onClick = { onModeSelected(uiState.statusModules[1].mode) },
                        modifier = Modifier.weight(1f)
                    )
                }

                ReactorHubCard(
                    uiState = uiState,
                    onReactorInteractionStateChange = onReactorInteractionStateChange,
                    onReactorCoreTap = onReactorCoreTap,
                    onReactorSegmentTap = onReactorSegmentTap,
                    onReactorSegmentLongPress = onReactorSegmentLongPress,
                    onReactorHapticEvent = onReactorHapticEvent,
                    modifier = Modifier.weight(0.44f)
                )

                Column(
                    modifier = Modifier.weight(0.28f),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
                ) {
                    StatusModuleCard(
                        module = uiState.statusModules[2],
                        selectedMode = uiState.selectedMode,
                        onClick = { onModeSelected(uiState.statusModules[2].mode) },
                        modifier = Modifier.weight(1f)
                    )
                    StatusModuleCard(
                        module = uiState.statusModules[3],
                        selectedMode = uiState.selectedMode,
                        onClick = { onModeSelected(uiState.statusModules[3].mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.38f),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)
                ) {
                    StatusModuleCard(
                        module = uiState.statusModules[0],
                        selectedMode = uiState.selectedMode,
                        onClick = { onModeSelected(uiState.statusModules[0].mode) },
                        modifier = Modifier.weight(1f)
                    )
                    StatusModuleCard(
                        module = uiState.statusModules[1],
                        selectedMode = uiState.selectedMode,
                        onClick = { onModeSelected(uiState.statusModules[1].mode) },
                        modifier = Modifier.weight(1f)
                    )
                }

                ReactorHubCard(
                    uiState = uiState,
                    onReactorInteractionStateChange = onReactorInteractionStateChange,
                    onReactorCoreTap = onReactorCoreTap,
                    onReactorSegmentTap = onReactorSegmentTap,
                    onReactorSegmentLongPress = onReactorSegmentLongPress,
                    onReactorHapticEvent = onReactorHapticEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.38f),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)
                ) {
                    StatusModuleCard(
                        module = uiState.statusModules[2],
                        selectedMode = uiState.selectedMode,
                        onClick = { onModeSelected(uiState.statusModules[2].mode) },
                        modifier = Modifier.weight(1f)
                    )
                    StatusModuleCard(
                        module = uiState.statusModules[3],
                        selectedMode = uiState.selectedMode,
                        onClick = { onModeSelected(uiState.statusModules[3].mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactorHubCard(
    uiState: LauncherUiState,
    onReactorInteractionStateChange: (ReactorInteractionState) -> Unit,
    onReactorCoreTap: () -> Unit,
    onReactorSegmentTap: (String) -> Unit,
    onReactorSegmentLongPress: (String) -> Unit,
    onReactorHapticEvent: (ReactorHapticEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val tokens = LauncherTheme.tokens
    val typography = LauncherTheme.typography

    PanelCard(
        modifier = modifier.fillMaxSize(),
        accent = uiState.selectedMode.accent,
        glowLevel = LauncherGlowLevel.High
    ) {
        Text(
            text = "REACTOR COMMAND CORE",
            style = typography.label,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(
            text = uiState.statusHeadline,
            style = typography.header,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(tokens.spacing.xs))
        Text(
            text = uiState.statusMessage,
            style = typography.body,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(tokens.spacing.md))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val reactorSize = when {
                maxWidth < 360.dp -> maxWidth * 0.92f
                maxWidth < 520.dp -> maxWidth * 0.82f
                else -> 420.dp
            }

            NanoCoreReactor(
                reactor = uiState.reactor,
                interactionState = uiState.reactorInteractionState,
                onInteractionStateChange = onReactorInteractionStateChange,
                onCoreTap = onReactorCoreTap,
                onSegmentTap = onReactorSegmentTap,
                onSegmentLongPress = onReactorSegmentLongPress,
                onHapticEvent = onReactorHapticEvent,
                modifier = Modifier
                    .size(reactorSize)
                    .aspectRatio(1f)
            )
        }

        Spacer(Modifier.height(tokens.spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
        ) {
            TelemetryStrip(
                label = "MODE",
                value = uiState.selectedMode.dockLabel.uppercase(),
                accent = uiState.selectedMode.accent,
                modifier = Modifier.weight(1f)
            )
            TelemetryStrip(
                label = "STATE",
                value = if (uiState.reactorInteractionState.isCorePressed) "CORE PRESS" else "ONLINE",
                accent = LauncherAccent.Green,
                modifier = Modifier.weight(1f)
            )
            TelemetryStrip(
                label = "LOCK",
                value = uiState.reactorInteractionState.pressedSegmentId?.uppercase() ?: "OPEN",
                accent = LauncherAccent.Magenta,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusModuleCard(
    module: LauncherStatusModule,
    selectedMode: LauncherMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val isSelected = module.mode == selectedMode

    Box(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        PanelCard(
            modifier = Modifier.fillMaxSize(),
            accent = module.accent,
            glowLevel = if (isSelected) LauncherGlowLevel.Medium else LauncherGlowLevel.Low
        ) {
            Text(
                text = module.title,
                style = typography.label,
                color = if (isSelected) colors.textPrimary else colors.textSecondary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = module.value,
                style = typography.display,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = module.detail,
                style = typography.body,
                color = colors.textSecondary
            )
            Spacer(Modifier.weight(1f))
            TelemetryStrip(
                label = "AUX",
                value = module.footer,
                accent = module.accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TelemetryStrip(
    label: String,
    value: String,
    accent: LauncherAccent,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val tokens = LauncherTheme.tokens
    val typography = LauncherTheme.typography
    val accentColor = colors.accent(accent)

    Row(
        modifier = modifier
            .clip(CutCornerShape(tokens.shapes.innerChamfer))
            .background(colors.panelInset.copy(alpha = 0.8f))
            .border(1.dp, accentColor.copy(alpha = 0.44f), CutCornerShape(tokens.shapes.innerChamfer))
            .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = typography.micro,
            color = accentColor
        )
        Spacer(Modifier.width(tokens.spacing.sm))
        Text(
            text = value,
            style = typography.numeric,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModeBadge(mode: LauncherMode) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val accentColor = colors.accent(mode.accent)

    Box(
        modifier = Modifier
            .clip(CutCornerShape(10.dp))
            .background(colors.panelInset.copy(alpha = 0.92f))
            .border(1.dp, accentColor.copy(alpha = 0.56f), CutCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.dockLabel.uppercase(),
            style = typography.label,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun UtilityPill(
    action: LauncherUtilityAction,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val accentColor = colors.accent(action.accent)

    Box(
        modifier = Modifier
            .clip(CutCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colors.panelOuter,
                        colors.panelInset
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.52f), CutCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = action.label,
            style = typography.label,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun ReactorGlyph(
    glyph: String,
    accent: LauncherAccent
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CutCornerShape(10.dp))
            .background(colors.panelInset.copy(alpha = 0.82f))
            .border(1.dp, colors.accent(accent).copy(alpha = 0.58f), CutCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            style = typography.label,
            color = colors.accent(accent),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

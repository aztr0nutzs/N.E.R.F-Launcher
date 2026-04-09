package com.nerf.launcher.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantScreen
//
//  Main composable for the themed hybrid assistant interface.
//  Architecture:
//    1. Full-screen artwork backplate (per-theme drawable)
//    2. Robot FX overlay (state-driven Canvas effects anchored to hotspots)
//    3. Hotspot gesture layer (detects taps in chest/hand/side zones)
//    4. Chat pane overlay (clipped to the themed chat region)
//    5. Controls overlay (input, mic, dock — aligned to artwork control area)
//    6. Theme switcher strip
//    7. Dismiss button
//
//  This is NOT a generic chat UI. The artwork is the visual backbone.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    AssistantScreen(
        uiState = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onInputTextChanged = viewModel::onInputTextChanged,
        modifier = modifier
    )
}

@Composable
fun AssistantScreen(
    uiState: AssistantUiState,
    onEvent: (AssistantEvent) -> Unit,
    onInputTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = uiState.activeTheme
    val hotspots = theme.hotspots
    val palette = theme.palette

    AnimatedVisibility(
        visible = uiState.isVisible,
        enter = fadeIn(tween(280, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            // ── Layer 1: Artwork Backplate ─────────────────────────────────
            Image(
                painter = painterResource(id = theme.backplateRes),
                contentDescription = "Assistant theme: ${theme.displayName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // ── Layer 2: Vignette scrim (enhances readability) ────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            ),
                            startY = 0f,
                            endY = with(LocalDensity.current) { screenHeight.toPx() }
                        )
                    )
            )

            // ── Layer 3: Robot State FX ───────────────────────────────────
            AssistantRobotFxOverlay(
                state = uiState.robotState,
                hotspots = hotspots,
                palette = palette,
                isChestCoreActive = uiState.isChestCoreActive,
                isHandProjectionActive = uiState.isHandProjectionActive,
                modifier = Modifier.fillMaxSize()
            )

            // ── Layer 4: Hotspot gesture detection ────────────────────────
            HotspotGestureLayer(
                hotspots = hotspots,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                onEvent = onEvent,
                modifier = Modifier.fillMaxSize()
            )

            // ── Layer 5: Side telemetry panels ────────────────────────────
            hotspots.sideModuleLeft?.let { bounds ->
                SideTelemetryPanel(
                    bounds = bounds,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    isOpen = uiState.isSidePanelLeftOpen,
                    palette = palette,
                    label = "TELEMETRY-L",
                    data = listOf(
                        "STATE" to stateShortLabel(uiState.robotState),
                        "MOOD" to uiState.mood.label.uppercase(),
                        "BANK" to uiState.bankStatusLabel.take(12),
                        "INTX" to "#${uiState.interactionCount}"
                    )
                )
            }

            hotspots.sideModuleRight?.let { bounds ->
                SideTelemetryPanel(
                    bounds = bounds,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    isOpen = uiState.isSidePanelRightOpen,
                    palette = palette,
                    label = "TELEMETRY-R",
                    data = listOf(
                        "THEME" to theme.displayName.take(10),
                        "VOICE" to if (uiState.isVoiceAvailable) "ONLINE" else "OFFLINE",
                        "CHAT" to if (uiState.isChatPaneOpen) "OPEN" else "CLOSED",
                        "MSGS" to "${uiState.transcript.size}"
                    )
                )
            }

            // ── Layer 6: Chest core overlay indicator ─────────────────────
            ChestCoreIndicator(
                isActive = uiState.isChestCoreActive,
                bounds = hotspots.chestCore,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                palette = palette
            )

            // ── Layer 7: Hand projection overlay ──────────────────────────
            HandProjectionIndicator(
                isActive = uiState.isHandProjectionActive,
                bounds = hotspots.handProjection,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                palette = palette
            )

            // ── Layer 8: Chat pane ────────────────────────────────────────
            val chatBounds = hotspots.chatPane
            AssistantChatOverlay(
                transcript = uiState.transcript,
                latestResponse = uiState.latestResponse,
                palette = palette,
                isVisible = uiState.isChatPaneOpen,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (screenWidth.toPx() * chatBounds.left).toInt(),
                            y = (screenHeight.toPx() * chatBounds.top).toInt()
                        )
                    }
                    .size(
                        width = screenWidth * (chatBounds.right - chatBounds.left),
                        height = screenHeight * (chatBounds.bottom - chatBounds.top)
                    )
            )

            // ── Layer 9: Controls ─────────────────────────────────────────
            AssistantControlsOverlay(
                uiState = uiState,
                onEvent = onEvent,
                onInputTextChanged = onInputTextChanged,
                modifier = Modifier.fillMaxSize()
            )

            // ── Layer 10: Top bar (dismiss + theme switcher) ──────────────
            TopBar(
                themeName = theme.displayName,
                onDismiss = { onEvent(AssistantEvent.Dismiss) },
                onCycleTheme = { onEvent(AssistantEvent.CycleTheme) },
                onToggleChat = { onEvent(AssistantEvent.ToggleChatPane) },
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            )

            // ── Layer 11: Theme switcher strip ────────────────────────────
            ThemeSwitcherStrip(
                activeThemeId = theme.id,
                onSwitchTheme = { id -> onEvent(AssistantEvent.SwitchTheme(id)) },
                palette = palette,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 12.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hotspot gesture layer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HotspotGestureLayer(
    hotspots: AssistantHotspots,
    screenWidth: Dp,
    screenHeight: Dp,
    onEvent: (AssistantEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val widthPx = with(density) { screenWidth.toPx() }
    val heightPx = with(density) { screenHeight.toPx() }

    val chestRect = remember(hotspots, widthPx, heightPx) {
        hotspots.chestCore.toPixelRect(widthPx, heightPx)
    }
    val handRect = remember(hotspots, widthPx, heightPx) {
        hotspots.handProjection.toPixelRect(widthPx, heightPx)
    }
    val sideLeftRect = remember(hotspots, widthPx, heightPx) {
        hotspots.sideModuleLeft?.toPixelRect(widthPx, heightPx)
    }
    val sideRightRect = remember(hotspots, widthPx, heightPx) {
        hotspots.sideModuleRight?.toPixelRect(widthPx, heightPx)
    }

    Box(
        modifier = modifier.pointerInput(hotspots) {
            detectTapGestures { offset ->
                when {
                    chestRect.contains(offset) ->
                        onEvent(AssistantEvent.ChestCoreTapped)
                    handRect.contains(offset) ->
                        onEvent(AssistantEvent.HandProjectionTapped)
                    sideLeftRect?.contains(offset) == true ->
                        onEvent(AssistantEvent.SideModuleTapped(AssistantEvent.SideModuleTapped.Side.LEFT))
                    sideRightRect?.contains(offset) == true ->
                        onEvent(AssistantEvent.SideModuleTapped(AssistantEvent.SideModuleTapped.Side.RIGHT))
                }
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    themeName: String,
    onDismiss: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleChat: () -> Unit,
    palette: AssistantThemePalette,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(palette.controlAccent)
    val surfaceColor = Color(palette.controlSurface)
    val textPrimary = Color(palette.textPrimary)
    val textSecondary = Color(palette.textSecondary)

    Row(
        modifier = modifier
            .clip(CutCornerShape(8.dp))
            .background(surfaceColor)
            .border(0.5.dp, accentColor.copy(alpha = 0.2f), CutCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accent dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "ASSISTANT LINK",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
            color = accentColor
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = themeName,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            color = textSecondary
        )
        Spacer(Modifier.weight(1f))

        // Chat toggle
        Box(
            modifier = Modifier
                .clip(CutCornerShape(4.dp))
                .clickable { onToggleChat() }
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = "LOG",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                color = accentColor.copy(alpha = 0.8f)
            )
        }

        Spacer(Modifier.width(6.dp))

        // Theme cycle
        Box(
            modifier = Modifier
                .clip(CutCornerShape(4.dp))
                .clickable { onCycleTheme() }
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = "SKIN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                color = accentColor.copy(alpha = 0.8f)
            )
        }

        Spacer(Modifier.width(6.dp))

        // Dismiss
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CutCornerShape(6.dp))
                .background(Color(0x33FF4444))
                .border(0.5.dp, Color(0x66FF4444), CutCornerShape(6.dp))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFFF6A6A)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Theme Switcher Strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeSwitcherStrip(
    activeThemeId: AssistantThemeId,
    onSwitchTheme: (AssistantThemeId) -> Unit,
    palette: AssistantThemePalette,
    modifier: Modifier = Modifier
) {
    val surfaceColor = Color(palette.controlSurface)

    Column(
        modifier = modifier
            .clip(CutCornerShape(6.dp))
            .background(surfaceColor)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        AssistantThemeId.values().forEach { themeId ->
            val isActive = themeId == activeThemeId
            val themeConfig = AssistantThemeRegistry.get(themeId)
            val accent = Color(themeConfig.palette.controlAccent)

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) accent.copy(alpha = 0.3f) else Color.Transparent)
                    .border(
                        width = if (isActive) 1.5.dp else 0.5.dp,
                        color = accent.copy(alpha = if (isActive) 0.9f else 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSwitchTheme(themeId) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent.copy(alpha = if (isActive) 0.9f else 0.4f))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Chest core indicator overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChestCoreIndicator(
    isActive: Boolean,
    bounds: Rect,
    screenWidth: Dp,
    screenHeight: Dp,
    palette: AssistantThemePalette
) {
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200))
    ) {
        val accentColor = Color(palette.coreGlow)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (screenWidth.toPx() * bounds.left).toInt(),
                        y = (screenHeight.toPx() * bounds.top).toInt()
                    )
                }
                .size(
                    width = screenWidth * (bounds.right - bounds.left),
                    height = screenHeight * (bounds.bottom - bounds.top)
                )
                .clip(CutCornerShape(8.dp))
                .border(1.dp, accentColor.copy(alpha = 0.4f), CutCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "● CORE ACTIVE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = accentColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "SCANNING REACTOR LINK",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 8.sp,
                    color = accentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hand projection indicator overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HandProjectionIndicator(
    isActive: Boolean,
    bounds: Rect,
    screenWidth: Dp,
    screenHeight: Dp,
    palette: AssistantThemePalette
) {
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200))
    ) {
        val accentColor = Color(palette.visorGlow)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (screenWidth.toPx() * bounds.left).toInt(),
                        y = (screenHeight.toPx() * bounds.top).toInt()
                    )
                }
                .size(
                    width = screenWidth * (bounds.right - bounds.left),
                    height = screenHeight * (bounds.bottom - bounds.top)
                )
                .clip(CutCornerShape(8.dp))
                .border(1.dp, accentColor.copy(alpha = 0.35f), CutCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "◈ PROJECTION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = accentColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "STATUS REPORT ACTIVE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 8.sp,
                    color = accentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Side telemetry panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SideTelemetryPanel(
    bounds: Rect,
    screenWidth: Dp,
    screenHeight: Dp,
    isOpen: Boolean,
    palette: AssistantThemePalette,
    label: String,
    data: List<Pair<String, String>>
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(180))
    ) {
        val accentColor = Color(palette.controlAccent)
        val surfaceColor = Color(palette.controlSurface)
        val textPrimary = Color(palette.textPrimary)
        val textSecondary = Color(palette.textSecondary)

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (screenWidth.toPx() * bounds.left).toInt(),
                        y = (screenHeight.toPx() * bounds.top).toInt()
                    )
                }
                .size(
                    width = screenWidth * (bounds.right - bounds.left),
                    height = screenHeight * (bounds.bottom - bounds.top)
                )
                .clip(CutCornerShape(6.dp))
                .background(surfaceColor)
                .border(0.5.dp, accentColor.copy(alpha = 0.25f), CutCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    letterSpacing = 1.2.sp,
                    color = accentColor
                )
                data.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 7.sp,
                            letterSpacing = 0.8.sp,
                            color = textSecondary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = value,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 7.sp,
                            letterSpacing = 0.8.sp,
                            color = textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Extensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts a normalized (0–1) Rect to pixel coordinates.
 */
private fun Rect.toPixelRect(width: Float, height: Float): Rect = Rect(
    left = left * width,
    top = top * height,
    right = right * width,
    bottom = bottom * height
)

private fun Rect.contains(offset: Offset): Boolean =
    offset.x in left..right && offset.y in top..bottom

private fun stateShortLabel(state: com.nerf.launcher.util.assistant.AssistantState): String = when (state) {
    com.nerf.launcher.util.assistant.AssistantState.IDLE -> "IDLE"
    com.nerf.launcher.util.assistant.AssistantState.WAKE -> "WAKE"
    com.nerf.launcher.util.assistant.AssistantState.LISTENING -> "LSTN"
    com.nerf.launcher.util.assistant.AssistantState.THINKING -> "PROC"
    com.nerf.launcher.util.assistant.AssistantState.RESPONDING -> "RESP"
    com.nerf.launcher.util.assistant.AssistantState.SPEAKING -> "SPKN"
    com.nerf.launcher.util.assistant.AssistantState.MUTED -> "MUTE"
    com.nerf.launcher.util.assistant.AssistantState.ERROR -> "ERR"
    com.nerf.launcher.util.assistant.AssistantState.REBOOTING -> "BOOT"
    com.nerf.launcher.util.assistant.AssistantState.SHUTTING_DOWN -> "DOWN"
}

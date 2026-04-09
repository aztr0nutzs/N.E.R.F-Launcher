package com.nerf.launcher.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantScreen
//
//  Layer stack (bottom → top, i.e. index 0 is drawn first):
//
//    0  Artwork backplate      ContentScale.Fit, no interaction
//    1  Robot FX Canvas        Canvas layer, anchored to imageRect
//    2  Robot gesture capture  pointerInput — robot-body zones only
//    3  Chat pane              LazyColumn clipped to transcriptRegion
//    4  Controls overlay       Input row · dock · left stack (own clickable)
//    5  Top bar                Theme picker · dismiss
//
//  INTERACTION SPLIT:
//  ─ Layer 2 handles ONLY reactor core/sector and hand-node taps.
//    It does NOT claim input for any control-widget region — those are fully
//    owned by Layer 4's per-widget clickable modifiers.
//  ─ In Compose Box, later children (higher index) are on top and receive
//    input first. So Layer 4 consumes its taps before they reach Layer 2.
//  ─ For any tap in the upper robot-body area (not covered by Layer 4 widgets),
//    Layer 2 catches and dispatches it.
//
//  Source image size: 1177 × 2048 px (portrait).
// ─────────────────────────────────────────────────────────────────────────────

private const val IMAGE_W = 1177f
private const val IMAGE_H = 2048f

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    AssistantScreen(
        uiState            = viewModel.uiState,
        onEvent            = viewModel::onEvent,
        onInputTextChanged = viewModel::onInputTextChanged,
        modifier           = modifier
    )
}

@Composable
fun AssistantScreen(
    uiState: AssistantUiState,
    onEvent: (AssistantEvent) -> Unit,
    onInputTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme   = uiState.activeTheme
    val palette = theme.palette

    AnimatedVisibility(
        visible  = uiState.isVisible,
        enter    = fadeIn(tween(280, easing = FastOutSlowInEasing)),
        exit     = fadeOut(tween(200)),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density    = LocalDensity.current
            val containerW = with(density) { maxWidth.toPx() }
            val containerH = with(density) { maxHeight.toPx() }

            // Compute the displayed image rect once and share it with every layer.
            // ContentScale.Fit → uniform scale, centred. The image may be
            // pillarboxed (portrait phone) or letterboxed (landscape / tablet).
            val imageRect = remember(containerW, containerH) {
                AssistantOverlayHitTest.computeImageRect(containerW, containerH, IMAGE_W, IMAGE_H)
            }

            // ── Layer 0: Artwork backplate ─────────────────────────────────
            Image(
                painter            = painterResource(id = theme.backplateRes),
                contentDescription = "Assistant theme: ${theme.displayName}",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize()
            )

            // ── Layer 1: Robot FX (Canvas, no input) ─────────────────────
            AssistantRobotFxOverlay(
                state                  = uiState.robotState,
                imageRect              = imageRect,
                palette                = palette,
                isChestCoreActive      = uiState.isChestCoreActive,
                isHandProjectionActive = uiState.isHandProjectionActive,
                activeSector           = uiState.activeSector,
                isReactorCoreBurst     = uiState.isReactorCoreBurst,
                isListening            = uiState.isListening,
                isInputFocused         = uiState.isInputFocused,
                activeDockAction       = uiState.activeDockAction,
                isDockCenterActive     = uiState.isDockCenterActive,
                activeLeftAction       = uiState.activeLeftAction,
                modifier               = Modifier.fillMaxSize()
            )

            // ── Layer 2: Robot-body gesture capture ───────────────────────
            // Handles ONLY reactor and hand-node taps. All control-area
            // taps are consumed by Layer 4 before they reach here.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageRect) {
                        detectTapGestures { offset -> handleRobotTap(offset, imageRect, onEvent) }
                    }
            )

            // ── Layer 3: Chat pane ────────────────────────────────────────
            AssistantChatOverlayMapped(
                transcript     = uiState.transcript,
                latestResponse = uiState.latestResponse,
                palette        = palette,
                isVisible      = uiState.isChatPaneOpen,
                imageRect      = imageRect,
                modifier       = Modifier.fillMaxSize()
            )

            // ── Layer 4: Controls overlay ─────────────────────────────────
            // All interactive controls for input, dock, and left stack live
            // here. Each widget uses its own clickable modifier so Compose
            // handles focus, ripple, and event ownership correctly.
            AssistantControlsOverlayMapped(
                uiState            = uiState,
                imageRect          = imageRect,
                onEvent            = onEvent,
                onInputTextChanged = onInputTextChanged,
                modifier           = Modifier.fillMaxSize()
            )

            // ── Layer 5: Top bar ──────────────────────────────────────────
            AssistantTopBar(
                palette       = palette,
                themeName     = theme.displayName,
                onDismiss     = { onEvent(AssistantEvent.Dismiss) },
                onCycleTheme  = { onEvent(AssistantEvent.CycleTheme) },
                onToggleChat  = { onEvent(AssistantEvent.ToggleChatPane) },
                activeThemeId = theme.id,
                onSwitchTheme = { id -> onEvent(AssistantEvent.SwitchTheme(id)) },
                modifier      = Modifier.fillMaxSize()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Robot-body tap dispatch (reactor + hand node only)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dispatch a tap in the robot-body zone to the appropriate [AssistantEvent].
 *
 * Control-widget events (input, dock, left stack) are NOT handled here —
 * they fire from the controls overlay's own clickable modifiers.
 */
private fun handleRobotTap(
    tapOffset: Offset,
    imageRect: Rect,
    onEvent: (AssistantEvent) -> Unit
) {
    when (val hit = AssistantOverlayHitTest.classify(tapOffset, imageRect)) {
        HitRegion.ReactorCore ->
            onEvent(AssistantEvent.ReactorCoreTapped)

        is HitRegion.ReactorSector ->
            onEvent(AssistantEvent.ReactorSectorTapped(hit.sector))

        HitRegion.HandNode ->
            onEvent(AssistantEvent.HandProjectionTapped)

        null -> Unit   // tap outside all robot-body zones — ignore
    }
}

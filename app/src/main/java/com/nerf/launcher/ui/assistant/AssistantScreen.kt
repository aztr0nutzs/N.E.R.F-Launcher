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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.gestures.detectTapGestures

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantScreen
//
//  Top-level composable. Architecture:
//
//    ┌──────────────────────────────────────────────┐
//    │  Layer 0 : Full-screen backplate image       │  ContentScale.Fit
//    │  Layer 1 : Robot state FX (Canvas)           │  mapped to imageRect
//    │  Layer 2 : Global gesture capture            │  imageRect hit-test
//    │  Layer 3 : Chat pane (clipped to region)     │
//    │  Layer 4 : Controls overlay (input/dock)     │
//    │  Layer 5 : Top bar & theme switcher          │
//    └──────────────────────────────────────────────┘
//
//  KEY DESIGN DECISION:
//    The backplate is rendered with ContentScale.Fit so its aspect ratio is
//    preserved. All overlay coordinates are mapped through [imageRect] which
//    is computed once from the actual on-screen size. This means every region
//    and hotspot is pixel-accurate regardless of screen size or density.
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
    val theme   = uiState.activeTheme
    val palette = theme.palette

    AnimatedVisibility(
        visible = uiState.isVisible,
        enter   = fadeIn(tween(280, easing = FastOutSlowInEasing)),
        exit    = fadeOut(tween(200)),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val containerW = with(density) { maxWidth.toPx() }
            val containerH = with(density) { maxHeight.toPx() }

            // ── Compute the displayed image rect once ──────────────────────
            // ContentScale.Fit preserves aspect ratio; the image may be
            // letterboxed or pillarboxed inside the container.
            val imageRect = remember(containerW, containerH) {
                AssistantOverlayHitTest.computeImageRect(containerW, containerH, IMAGE_W, IMAGE_H)
            }

            // ── Layer 0: Artwork backplate ─────────────────────────────────
            Image(
                painter          = painterResource(id = theme.backplateRes),
                contentDescription = "Assistant theme: ${theme.displayName}",
                contentScale     = ContentScale.Fit,
                modifier         = Modifier.fillMaxSize()
            )

            // ── Layer 1: Robot state FX (Canvas) ──────────────────────────
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

            // ── Layer 2: Global gesture capture ───────────────────────────
            // Intercepts ALL taps and classifies them via the hit-test engine.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageRect) {
                        detectTapGestures { tapOffset ->
                            handleTap(tapOffset, imageRect, onEvent)
                        }
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
            AssistantControlsOverlayMapped(
                uiState            = uiState,
                imageRect          = imageRect,
                onEvent            = onEvent,
                onInputTextChanged = onInputTextChanged,
                modifier           = Modifier.fillMaxSize()
            )

            // ── Layer 5: Top bar (theme + dismiss) ────────────────────────
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
//  Tap dispatch
// ─────────────────────────────────────────────────────────────────────────────

private fun handleTap(
    tapOffset: Offset,
    imageRect: Rect,
    onEvent: (AssistantEvent) -> Unit
) {
    val hit = AssistantOverlayHitTest.classify(tapOffset, imageRect) ?: return

    when (hit) {
        // Reactor
        HitRegion.ReactorCore ->
            onEvent(AssistantEvent.ReactorCoreTapped)

        is HitRegion.ReactorSector ->
            onEvent(AssistantEvent.ReactorSectorTapped(hit.sector))

        // Robot body
        HitRegion.HandNode ->
            onEvent(AssistantEvent.HandProjectionTapped)

        // Input accessories — individual logical buttons in the input shell
        HitRegion.InputMic ->
            onEvent(AssistantEvent.MicTapped)

        HitRegion.InputEmoji ->
            Unit    // emoji picker — no-op for now; extend as needed

        HitRegion.InputText ->
            onEvent(AssistantEvent.InputFocused)   // inform ViewModel; actual focus handled by TextField

        HitRegion.Send ->
            onEvent(AssistantEvent.SubmitText(""))  // ViewModel reads from uiState.inputText

        HitRegion.ToggleModule ->
            onEvent(AssistantEvent.ToggleModuleTapped)

        HitRegion.Transcript, HitRegion.ChatPanel ->
            Unit    // scroll is handled internally by LazyColumn

        // Left action stack
        is HitRegion.LeftAction ->
            onEvent(AssistantEvent.LeftActionTapped(hit.action))

        // Dock
        HitRegion.DockCenter ->
            onEvent(AssistantEvent.DockCenterTapped)

        is HitRegion.Dock ->
            onEvent(AssistantEvent.DockActionTapped(hit.action))
    }
}

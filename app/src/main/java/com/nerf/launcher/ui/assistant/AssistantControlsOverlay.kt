package com.nerf.launcher.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantControlsOverlayMapped
//
//  Precision-positioned interactive controls placed at exact normalized
//  coordinates via [imageRect]. Every widget owns its own clickable modifier —
//  no global gesture interception required for control-area events.
//
//  CORRECTIONS vs. previous implementation:
//
//  1. roundToInt() — all IntOffset conversions now use roundToInt() instead of
//     toInt(). The old truncation caused up to 0.99 px of systematic offset
//     which, on a 1080-wide screen at 3× density, shifted widgets by ~0.33 dp.
//
//  2. SmallIconButton shape — mic and emoji buttons now use CutCornerShape
//     instead of RoundedCornerShape(50). The visual region is NOT square
//     (inputMic w=0.0357, h=0.0244), so clipping to a circle distorted the
//     shape and shrunk the visible hit area. A slight cut-corner rectangle
//     is consistent with the design language and preserves the full rect.
//
//  3. Expanded touch targets — SmallIconButton uses an OUTER clickable wrapper
//     sized to the expanded hit-target rect (from AssistantOverlayMap.*Hit)
//     while the INNER visual is sized to the exact spec rect. This ensures
//     touch targets meet ~44 dp guidelines without moving any visuals.
//
//  4. Left-stack and dock buttons also use expanded outer wrappers.
//
//  Regions covered:
//    inputShell / inputTextRegion   — BasicTextField
//    inputMic / inputEmoji          — icon buttons with expanded hit targets
//    sendButton                     — SEND action button
//    dartCount                      — interaction count display
//    toggleModule                   — ON/OFF toggle
//    leftPower/Network/Alerts/Settings — left action stack
//    dockSettings/Map/Modules/Mic/Profile — bottom dock buttons
//    dockCenterCore                 — animated NERF logo button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantControlsOverlayMapped(
    uiState: AssistantUiState,
    imageRect: Rect,
    onEvent: (AssistantEvent) -> Unit,
    onInputTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = uiState.activeTheme.palette
    val accent  = Color(palette.controlAccent)
    val surface = Color(palette.controlSurface)
    val textPri = Color(palette.textPrimary)
    val textSec = Color(palette.textSecondary)
    val dock    = Color(palette.dockSurface)
    val density = LocalDensity.current

    AnimatedVisibility(
        visible  = uiState.isVisible,
        enter    = fadeIn(tween(300)),
        exit     = fadeOut(tween(200)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Input shell ────────────────────────────────────────────────
            // The shell is the full text-entry box (not including mic/emoji/send).
            val shellPx = AssistantOverlayMap.inputShell.toPx(imageRect)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            shellPx.left.roundToInt(),
                            shellPx.top.roundToInt()
                        )
                    }
                    .size(
                        width  = with(density) { shellPx.width.toDp() },
                        height = with(density) { shellPx.height.toDp() }
                    )
                    .clip(CutCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(surface)
                    .border(
                        width = if (uiState.isInputFocused) 1.dp else 0.5.dp,
                        color = accent.copy(alpha = if (uiState.isInputFocused) 0.70f else 0.25f),
                        shape = CutCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value         = uiState.inputText,
                    onValueChange = onInputTextChanged,
                    textStyle     = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 11.sp,
                        color      = textPri
                    ),
                    cursorBrush     = SolidColor(accent),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onEvent(AssistantEvent.SubmitText(uiState.inputText)) }
                    ),
                    decorationBox = { inner ->
                        Box(
                            modifier         = Modifier.padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.inputText.isEmpty()) {
                                Text(
                                    text       = "Type command…",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize   = 10.sp,
                                    color      = textSec.copy(alpha = 0.45f)
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ── Mic button — expanded touch target, exact visual ───────────
            ExpandedIconButton(
                visualNormRect   = AssistantOverlayMap.inputMic,
                hitTargetNormRect = AssistantOverlayMap.inputMicHit,
                imageRect        = imageRect,
                onClick          = { onEvent(AssistantEvent.MicTapped) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CutCornerShape(3.dp))
                        .background(surface)
                        .border(
                            0.5.dp,
                            if (uiState.isListening) accent.copy(0.70f)
                            else accent.copy(0.30f),
                            CutCornerShape(3.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = if (uiState.isListening) "●" else "🎙",
                        fontSize  = 9.sp,
                        color     = if (uiState.isListening) accent else accent,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Repeat-last button — re-speaks the assistant's last response ─
            ExpandedIconButton(
                visualNormRect    = AssistantOverlayMap.inputEmoji,
                hitTargetNormRect = AssistantOverlayMap.inputEmojiHit,
                imageRect         = imageRect,
                onClick           = { onEvent(AssistantEvent.RepeatLast) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CutCornerShape(3.dp))
                        .background(surface)
                        .border(0.5.dp, accent.copy(0.25f), CutCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = "↩",
                        fontSize  = 9.sp,
                        color     = accent.copy(0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── SEND button ────────────────────────────────────────────────
            val sendPx = AssistantOverlayMap.sendButton.toPx(imageRect)
            MappedButton(
                label   = "SEND",
                pxRect  = sendPx,
                color   = accent,
                surface = surface,
                onClick = { onEvent(AssistantEvent.SubmitText(uiState.inputText)) }
            )

            // ── Dart count display ─────────────────────────────────────────
            val dartPx = AssistantOverlayMap.dartCount.toPx(imageRect)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(dartPx.left.roundToInt(), dartPx.top.roundToInt())
                    }
                    .size(
                        width  = with(density) { dartPx.width.toDp() },
                        height = with(density) { dartPx.height.toDp() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text          = "DARTS: ${uiState.interactionCount}",
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 9.sp,
                    letterSpacing = 1.2.sp,
                    color         = accent.copy(alpha = 0.85f)
                )
            }

            // ── Toggle module ──────────────────────────────────────────────
            val togglePx = AssistantOverlayMap.toggleModule.toPx(imageRect)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(togglePx.left.roundToInt(), togglePx.top.roundToInt())
                    }
                    .size(
                        width  = with(density) { togglePx.width.toDp() },
                        height = with(density) { togglePx.height.toDp() }
                    )
                    .clip(RoundedCornerShape(50))
                    .background(if (uiState.isToggleModuleOn) accent.copy(0.20f) else surface)
                    .border(
                        width = 1.dp,
                        color = accent.copy(if (uiState.isToggleModuleOn) 0.80f else 0.30f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { onEvent(AssistantEvent.ToggleModuleTapped) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text          = if (uiState.isToggleModuleOn) "ON" else "OFF",
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 8.sp,
                    letterSpacing = 1.0.sp,
                    color         = if (uiState.isToggleModuleOn) accent else textSec
                )
            }

            // ── Left action stack — expanded touch targets ─────────────────
            AssistantOverlayMap.leftActionStack.forEachIndexed { idx, (action, visualNorm) ->
                val hitNorm  = AssistantOverlayMap.leftActionStackHit[idx].second
                val isActive = uiState.activeLeftAction == action

                ExpandedButton(
                    label             = action.label,
                    visualNormRect    = visualNorm,
                    hitTargetNormRect = hitNorm,
                    imageRect         = imageRect,
                    color             = if (isActive) accent else accent.copy(0.60f),
                    surface           = if (isActive) accent.copy(0.14f) else surface,
                    onClick           = { onEvent(AssistantEvent.LeftActionTapped(action)) }
                )
            }

            // ── Bottom dock buttons — expanded touch targets ───────────────
            AssistantOverlayMap.dockButtons.forEachIndexed { idx, (action, visualNorm) ->
                val hitNorm  = AssistantOverlayMap.dockButtonsHit[idx].second
                val isActive = uiState.activeDockAction == action

                ExpandedButton(
                    label             = action.label,
                    visualNormRect    = visualNorm,
                    hitTargetNormRect = hitNorm,
                    imageRect         = imageRect,
                    color             = if (isActive) accent else accent.copy(0.65f),
                    surface           = if (isActive) accent.copy(0.14f) else dock,
                    onClick           = { onEvent(AssistantEvent.DockActionTapped(action)) }
                )
            }

            // ── Dock center core ───────────────────────────────────────────
            val dockCenterPx = AssistantOverlayMap.dockCenterCore.toPx(imageRect)
            DockCenterButton(
                pxRect   = dockCenterPx,
                isActive = uiState.isDockCenterActive,
                accent   = accent,
                surface  = dock,
                onClick  = { onEvent(AssistantEvent.DockCenterTapped) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A labelled, rectangular button placed at the exact [pxRect] position.
 * All offsets use [roundToInt] to eliminate sub-pixel truncation.
 */
@Composable
private fun MappedButton(
    label: String,
    pxRect: Rect,
    color: Color,
    surface: Color,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(pxRect.left.roundToInt(), pxRect.top.roundToInt()) }
            .size(
                width  = with(density) { pxRect.width.toDp() },
                height = with(density) { pxRect.height.toDp() }
            )
            .clip(CutCornerShape(4.dp))
            .background(surface)
            .border(0.5.dp, color.copy(alpha = 0.40f), CutCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = label,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            fontSize      = 8.sp,
            letterSpacing = 1.0.sp,
            color         = color,
            textAlign     = TextAlign.Center
        )
    }
}

/**
 * A button with a LARGER invisible touch target ([hitTargetNormRect]) wrapping
 * the exact visual rect ([visualNormRect]).
 *
 * The outer Box is clickable and sized to the expanded hit target.
 * The inner Box is sized to the exact visual rect and centred inside the outer.
 * This meets ~44dp touch target guidelines without shifting any visual elements.
 */
@Composable
private fun ExpandedButton(
    label: String,
    visualNormRect: NormRect,
    hitTargetNormRect: NormRect,
    imageRect: Rect,
    color: Color,
    surface: Color,
    onClick: () -> Unit
) {
    val density   = LocalDensity.current
    val hitPx     = hitTargetNormRect.toPx(imageRect)
    val visualPx  = visualNormRect.toPx(imageRect)

    Box(
        modifier = Modifier
            .offset { IntOffset(hitPx.left.roundToInt(), hitPx.top.roundToInt()) }
            .size(
                width  = with(density) { hitPx.width.toDp() },
                height = with(density) { hitPx.height.toDp() }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Visual is centred inside the hit target — no offset needed
        Box(
            modifier = Modifier
                .size(
                    width  = with(density) { visualPx.width.toDp() },
                    height = with(density) { visualPx.height.toDp() }
                )
                .clip(CutCornerShape(4.dp))
                .background(surface)
                .border(0.5.dp, color.copy(0.40f), CutCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text          = label,
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.Bold,
                fontSize      = 8.sp,
                letterSpacing = 1.0.sp,
                color         = color,
                textAlign     = TextAlign.Center
            )
        }
    }
}

/**
 * Icon button with expanded touch target — visual is at exact [visualNormRect],
 * touch target is [hitTargetNormRect]. [content] renders the visual widget.
 */
@Composable
private fun ExpandedIconButton(
    visualNormRect: NormRect,
    hitTargetNormRect: NormRect,
    imageRect: Rect,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val density  = LocalDensity.current
    val hitPx    = hitTargetNormRect.toPx(imageRect)
    val visualPx = visualNormRect.toPx(imageRect)

    Box(
        modifier = Modifier
            .offset { IntOffset(hitPx.left.roundToInt(), hitPx.top.roundToInt()) }
            .size(
                width  = with(density) { hitPx.width.toDp() },
                height = with(density) { hitPx.height.toDp() }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(
                width  = with(density) { visualPx.width.toDp() },
                height = with(density) { visualPx.height.toDp() }
            )
        ) {
            content()
        }
    }
}

/** Animated dock center (NERF logo) button. */
@Composable
private fun DockCenterButton(
    pxRect: Rect,
    isActive: Boolean,
    accent: Color,
    surface: Color,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val t       = rememberInfiniteTransition(label = "dockCenterBtn")
    val pulse by t.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dockBtnPulse"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(pxRect.left.roundToInt(), pxRect.top.roundToInt()) }
            .size(
                width  = with(density) { pxRect.width.toDp() },
                height = with(density) { pxRect.height.toDp() }
            )
            .clip(RoundedCornerShape(50))
            .background(if (isActive) accent.copy(0.20f) else surface.copy(0.50f))
            .border(
                width = if (isActive) (1.5f * pulse).dp else 1.dp,
                color = accent.copy(if (isActive) pulse * 0.80f else 0.35f),
                shape = RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = "N",
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            fontSize      = 14.sp,
            letterSpacing = 2.sp,
            color         = accent.copy(if (isActive) 1f else 0.65f)
        )
    }
}

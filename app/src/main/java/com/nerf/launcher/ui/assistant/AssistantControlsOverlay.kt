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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantControlsOverlayMapped
//
//  Precise-positioned interactive controls mapped to the backplate regions
//  via [imageRect]. Every widget is placed at exact normalized coordinates.
//
//  Regions covered:
//    • inputShell / inputTextRegion — text input
//    • inputMic — mic button
//    • inputEmoji — emoji (stub)
//    • sendButton — SEND
//    • dartCount — dart count display
//    • toggleModule — toggle switch
//    • dockHousing + dockSettings/Map/Modules/dockCenterCore/dockMic/dockProfile —
//        bottom dock buttons
//    • leftPower / leftNetwork / leftAlerts / leftSettings — left action stack
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

            // ── Input shell + text region ──────────────────────────────────
            val inputShellPx = AssistantOverlayMap.inputShell.toPx(imageRect)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(inputShellPx.left.toInt(), inputShellPx.top.toInt())
                    }
                    .size(
                        width  = with(density) { inputShellPx.width.toDp() },
                        height = with(density) { inputShellPx.height.toDp() }
                    )
                    .clip(CutCornerShape(4.dp))
                    .background(surface)
                    .border(
                        width = if (uiState.isInputFocused) 1.dp else 0.5.dp,
                        color = accent.copy(alpha = if (uiState.isInputFocused) 0.70f else 0.25f),
                        shape = CutCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value           = uiState.inputText,
                    onValueChange   = onInputTextChanged,
                    textStyle       = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 11.sp,
                        color      = textPri
                    ),
                    cursorBrush   = SolidColor(accent),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onEvent(AssistantEvent.SubmitText(uiState.inputText)) }
                    ),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
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
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
                )
            }

            // ── Mic button ────────────────────────────────────────────────
            SmallIconButton(
                label      = if (uiState.isListening) "●" else "🎙",
                imageRect  = imageRect,
                normRect   = AssistantOverlayMap.inputMic,
                color      = if (uiState.isListening) Color(0xFF73FF7C) else accent,
                surface    = surface,
                onClick    = { onEvent(AssistantEvent.MicTapped) }
            )

            // ── Emoji button (stub) ────────────────────────────────────────
            SmallIconButton(
                label     = "☺",
                imageRect = imageRect,
                normRect  = AssistantOverlayMap.inputEmoji,
                color     = accent.copy(alpha = 0.60f),
                surface   = surface,
                onClick   = { /* emoji picker stub */ }
            )

            // ── SEND button ────────────────────────────────────────────────
            val sendPx = AssistantOverlayMap.sendButton.toPx(imageRect)
            MappedButton(
                label     = "SEND",
                pxRect    = sendPx,
                color     = accent,
                surface   = surface,
                onClick   = { onEvent(AssistantEvent.SubmitText(uiState.inputText)) }
            )

            // ── Dart count display ─────────────────────────────────────────
            val dartPx = AssistantOverlayMap.dartCount.toPx(imageRect)
            Box(
                modifier = Modifier
                    .offset { IntOffset(dartPx.left.toInt(), dartPx.top.toInt()) }
                    .size(
                        width  = with(density) { dartPx.width.toDp() },
                        height = with(density) { dartPx.height.toDp() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "DARTS: ${uiState.interactionCount}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 9.sp,
                    letterSpacing = 1.2.sp,
                    color      = accent.copy(alpha = 0.85f)
                )
            }

            // ── Toggle module ──────────────────────────────────────────────
            val togglePx = AssistantOverlayMap.toggleModule.toPx(imageRect)
            Box(
                modifier = Modifier
                    .offset { IntOffset(togglePx.left.toInt(), togglePx.top.toInt()) }
                    .size(
                        width  = with(density) { togglePx.width.toDp() },
                        height = with(density) { togglePx.height.toDp() }
                    )
                    .clip(RoundedCornerShape(50))
                    .background(if (uiState.isToggleModuleOn) accent.copy(0.20f) else surface)
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = if (uiState.isToggleModuleOn) 0.80f else 0.30f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { onEvent(AssistantEvent.ToggleModuleTapped) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = if (uiState.isToggleModuleOn) "ON" else "OFF",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 8.sp,
                    letterSpacing = 1.0.sp,
                    color      = if (uiState.isToggleModuleOn) accent else textSec
                )
            }

            // ── Left action stack ──────────────────────────────────────────
            AssistantOverlayMap.leftActionStack.forEach { (action, normRect) ->
                val px = normRect.toPx(imageRect)
                val isActive = uiState.activeLeftAction == action
                MappedButton(
                    label   = action.label,
                    pxRect  = px,
                    color   = if (isActive) accent else accent.copy(0.60f),
                    surface = if (isActive) accent.copy(0.14f) else surface,
                    onClick = { onEvent(AssistantEvent.LeftActionTapped(action)) }
                )
            }

            // ── Bottom dock buttons ────────────────────────────────────────
            AssistantOverlayMap.dockButtons.forEach { (action, normRect) ->
                val px       = normRect.toPx(imageRect)
                val isActive = uiState.activeDockAction == action
                MappedButton(
                    label   = action.label,
                    pxRect  = px,
                    color   = if (isActive) accent else accent.copy(0.65f),
                    surface = if (isActive) accent.copy(0.14f) else dock,
                    onClick = { onEvent(AssistantEvent.DockActionTapped(action)) }
                )
            }

            // ── Dock center core (NERF logo button) ────────────────────────
            val dockCenterPx = AssistantOverlayMap.dockCenterCore.toPx(imageRect)
            DockCenterButton(
                pxRect    = dockCenterPx,
                isActive  = uiState.isDockCenterActive,
                accent    = accent,
                surface   = dock,
                onClick   = { onEvent(AssistantEvent.DockCenterTapped) }
            )
        }
    }
}

// ── Shared button composables ─────────────────────────────────────────────────

@Composable
private fun MappedButton(
    label: String,
    pxRect: androidx.compose.ui.geometry.Rect,
    color: Color,
    surface: Color,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(pxRect.left.toInt(), pxRect.top.toInt()) }
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

@Composable
private fun SmallIconButton(
    label: String,
    imageRect: Rect,
    normRect: NormRect,
    color: Color,
    surface: Color,
    onClick: () -> Unit
) {
    val px      = normRect.toPx(imageRect)
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(px.left.toInt(), px.top.toInt()) }
            .size(
                width  = with(density) { px.width.toDp() },
                height = with(density) { px.height.toDp() }
            )
            .clip(RoundedCornerShape(50))
            .background(surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 9.sp,
            color      = color,
            textAlign  = TextAlign.Center
        )
    }
}

@Composable
private fun DockCenterButton(
    pxRect: androidx.compose.ui.geometry.Rect,
    isActive: Boolean,
    accent: Color,
    surface: Color,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val it      = rememberInfiniteTransition(label = "dockCenterBtn")
    val pulse by it.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dockBtnPulse"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(pxRect.left.toInt(), pxRect.top.toInt()) }
            .size(
                width  = with(density) { pxRect.width.toDp() },
                height = with(density) { pxRect.height.toDp() }
            )
            .clip(RoundedCornerShape(50))
            .background(if (isActive) accent.copy(0.20f) else surface.copy(0.50f))
            .border(
                width = if (isActive) (1.5 * pulse).dp else 1.dp,
                color = accent.copy(alpha = if (isActive) pulse * 0.80f else 0.35f),
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
            color         = accent.copy(alpha = if (isActive) 1f else 0.65f)
        )
    }
}

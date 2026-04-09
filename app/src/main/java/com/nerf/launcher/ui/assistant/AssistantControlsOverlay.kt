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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.launcher.util.assistant.AssistantAction
import com.nerf.launcher.util.assistant.AssistantState
import com.nerf.launcher.util.assistant.PersonalityMood

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantControlsOverlay
//
//  The input/send/mic controls, quick action dock, and status indicators
//  rendered as an overlay aligned to the artwork's control regions.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantControlsOverlay(
    uiState: AssistantUiState,
    onEvent: (AssistantEvent) -> Unit,
    onInputTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = uiState.activeTheme.palette
    val accentColor = Color(palette.controlAccent)
    val surfaceColor = Color(palette.controlSurface)
    val textPrimary = Color(palette.textPrimary)
    val textSecondary = Color(palette.textSecondary)
    val dockSurface = Color(palette.dockSurface)

    AnimatedVisibility(
        visible = uiState.isVisible,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            // ── Status bar ────────────────────────────────────────────────
            StatusBar(
                state = uiState.robotState,
                mood = uiState.mood,
                bankStatus = uiState.bankStatusLabel,
                interactionCount = uiState.interactionCount,
                accentColor = accentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surfaceColor = surfaceColor,
                onMoodTap = { onEvent(AssistantEvent.CycleMood) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(6.dp))

            // ── Input row ─────────────────────────────────────────────────
            InputRow(
                inputText = uiState.inputText,
                onInputTextChanged = onInputTextChanged,
                onSubmit = { onEvent(AssistantEvent.SubmitText(uiState.inputText)) },
                accentColor = accentColor,
                surfaceColor = surfaceColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(6.dp))

            // ── Voice controls row ────────────────────────────────────────
            VoiceControlsRow(
                isVoiceAvailable = uiState.isVoiceAvailable,
                isListening = uiState.isListening,
                state = uiState.robotState,
                accentColor = accentColor,
                surfaceColor = surfaceColor,
                textPrimary = textPrimary,
                onMicTap = { onEvent(AssistantEvent.MicTapped) },
                onRepeatTap = { onEvent(AssistantEvent.RepeatLast) },
                onInterruptTap = { onEvent(AssistantEvent.InterruptSpeaking) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Quick action dock ─────────────────────────────────────────
            QuickActionDock(
                accentColor = accentColor,
                dockSurface = dockSurface,
                textPrimary = textPrimary,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Status Bar ───────────────────────────────────────────────────────────────

@Composable
private fun StatusBar(
    state: AssistantState,
    mood: PersonalityMood,
    bankStatus: String,
    interactionCount: Int,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    onMoodTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = stateDisplayColor(state, accentColor)
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val statusAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusDotPulse"
    )

    Row(
        modifier = modifier
            .clip(CutCornerShape(6.dp))
            .background(surfaceColor)
            .border(0.5.dp, accentColor.copy(alpha = 0.2f), CutCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor.copy(alpha = statusAlpha))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stateDisplayLabel(state),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = statusColor
        )
        Spacer(Modifier.weight(1f))
        // Mood indicator (tappable)
        Text(
            text = "PROFILE ${mood.label.uppercase()}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 1.2.sp,
            color = accentColor.copy(alpha = 0.8f),
            modifier = Modifier
                .clip(CutCornerShape(4.dp))
                .clickable { onMoodTap() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "#$interactionCount",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 8.sp,
            color = textSecondary
        )
    }
}

// ── Input Row ────────────────────────────────────────────────────────────────

@Composable
private fun InputRow(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    accentColor: Color,
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text input field
        BasicTextField(
            value = inputText,
            onValueChange = onInputTextChanged,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = textPrimary
            ),
            cursorBrush = SolidColor(accentColor),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(CutCornerShape(6.dp))
                        .background(surfaceColor)
                        .border(0.5.dp, accentColor.copy(alpha = 0.25f), CutCornerShape(6.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Type assistant command",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = textSecondary.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(6.dp))

        // Send button
        Box(
            modifier = Modifier
                .height(38.dp)
                .width(60.dp)
                .clip(CutCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.5f), CutCornerShape(6.dp))
                .clickable { onSubmit() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SEND",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = accentColor
            )
        }
    }
}

// ── Voice Controls Row ───────────────────────────────────────────────────────

@Composable
private fun VoiceControlsRow(
    isVoiceAvailable: Boolean,
    isListening: Boolean,
    state: AssistantState,
    accentColor: Color,
    surfaceColor: Color,
    textPrimary: Color,
    onMicTap: () -> Unit,
    onRepeatTap: () -> Unit,
    onInterruptTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Mic button
        ControlPill(
            label = if (!isVoiceAvailable) "NO MIC" else if (isListening) "● LIVE" else "MIC",
            color = if (isListening) Color(0xFF73FF7C) else accentColor,
            surfaceColor = surfaceColor,
            enabled = isVoiceAvailable,
            onClick = onMicTap,
            modifier = Modifier.weight(1f)
        )

        // Repeat last
        ControlPill(
            label = "REPEAT",
            color = accentColor,
            surfaceColor = surfaceColor,
            enabled = true,
            onClick = onRepeatTap,
            modifier = Modifier.weight(1f)
        )

        // Interrupt / stop voice
        ControlPill(
            label = "STOP",
            color = Color(0xFFFF9F43),
            surfaceColor = surfaceColor,
            enabled = state.isSpeaking || isListening,
            onClick = onInterruptTap,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Quick Action Dock ────────────────────────────────────────────────────────

@Composable
private fun QuickActionDock(
    accentColor: Color,
    dockSurface: Color,
    textPrimary: Color,
    onEvent: (AssistantEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CutCornerShape(8.dp))
            .background(dockSurface)
            .border(0.5.dp, accentColor.copy(alpha = 0.15f), CutCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "QUICK ACTIONS",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.6.sp,
            color = accentColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DockActionButton(
                label = "SETTINGS",
                accentColor = Color(0xFF27E7FF),
                surfaceColor = dockSurface,
                onClick = {
                    onEvent(AssistantEvent.LauncherCommandTapped(
                        AssistantAction.LauncherCommand.OPEN_SETTINGS
                    ))
                },
                modifier = Modifier.weight(1f)
            )
            DockActionButton(
                label = "DIAG",
                accentColor = Color(0xFFFF9F43),
                surfaceColor = dockSurface,
                onClick = {
                    onEvent(AssistantEvent.LauncherCommandTapped(
                        AssistantAction.LauncherCommand.OPEN_DIAGNOSTICS
                    ))
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DockActionButton(
                label = "NODES",
                accentColor = Color(0xFF73FF7C),
                surfaceColor = dockSurface,
                onClick = {
                    onEvent(AssistantEvent.LauncherCommandTapped(
                        AssistantAction.LauncherCommand.OPEN_NODE_HUNTER
                    ))
                },
                modifier = Modifier.weight(1f)
            )
            DockActionButton(
                label = "LOCK",
                accentColor = Color(0xFFFF47D0),
                surfaceColor = dockSurface,
                onClick = {
                    onEvent(AssistantEvent.LauncherCommandTapped(
                        AssistantAction.LauncherCommand.SHOW_LOCK_SURFACE
                    ))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Shared Pill / Button Components ──────────────────────────────────────────

@Composable
private fun ControlPill(
    label: String,
    color: Color,
    surfaceColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.35f
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(CutCornerShape(6.dp))
            .background(surfaceColor)
            .border(0.5.dp, color.copy(alpha = 0.35f * alpha), CutCornerShape(6.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            color = color.copy(alpha = alpha),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DockActionButton(
    label: String,
    accentColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(CutCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.06f))
            .border(0.5.dp, accentColor.copy(alpha = 0.3f), CutCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = accentColor,
            textAlign = TextAlign.Center
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun stateDisplayLabel(state: AssistantState): String = when (state) {
    AssistantState.IDLE -> "STANDBY"
    AssistantState.WAKE -> "WAKING"
    AssistantState.LISTENING -> "LISTENING"
    AssistantState.THINKING -> "RESOLVING"
    AssistantState.RESPONDING -> "RESPONDING"
    AssistantState.SPEAKING -> "SPEAKING"
    AssistantState.MUTED -> "MUTED"
    AssistantState.ERROR -> "FAULT"
    AssistantState.REBOOTING -> "REBOOTING"
    AssistantState.SHUTTING_DOWN -> "SHUTDOWN"
}

private fun stateDisplayColor(state: AssistantState, accent: Color): Color = when (state) {
    AssistantState.IDLE -> accent.copy(alpha = 0.6f)
    AssistantState.WAKE -> Color(0xFFFFD84D)
    AssistantState.LISTENING -> Color(0xFF73FF7C)
    AssistantState.THINKING -> Color(0xFFFFD84D)
    AssistantState.RESPONDING -> accent
    AssistantState.SPEAKING -> accent
    AssistantState.MUTED -> Color(0xFF667983)
    AssistantState.ERROR -> Color(0xFFFF6A6A)
    AssistantState.REBOOTING -> Color(0xFFFFD84D)
    AssistantState.SHUTTING_DOWN -> Color(0xFFFF6A6A)
}

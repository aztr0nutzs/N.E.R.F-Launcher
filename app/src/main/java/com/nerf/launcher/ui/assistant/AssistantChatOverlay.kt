package com.nerf.launcher.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantChatOverlay
//
//  A real, readable chat transcript overlay rendered inside the chat pane
//  hotspot bounds. Features:
//    • Distinct user / assistant message bubbles
//    • Proper clipping to the pane region
//    • Auto-scroll to latest message
//    • Good readability with monospace tactical styling
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantChatOverlay(
    transcript: List<TranscriptMessage>,
    latestResponse: String,
    palette: AssistantThemePalette,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val panelBg = Color(palette.chatPanelBg)
    val borderColor = Color(palette.chatPanelBorder)
    val textPrimary = Color(palette.textPrimary)
    val textSecondary = Color(palette.textSecondary)
    val userBubble = Color(palette.chatUserBubble)
    val assistantBubble = Color(palette.chatAssistantBubble)
    val accentColor = Color(palette.controlAccent)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(initialAlpha = 0.3f) + slideInVertically { it / 4 },
        exit = fadeOut() + slideOutVertically { it / 4 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CutCornerShape(8.dp))
                .background(panelBg)
                .border(1.dp, borderColor, CutCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "TRANSCRIPT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp,
                        color = accentColor
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${transcript.size} MSG",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        color = textSecondary
                    )
                }

                Spacer(Modifier.height(2.dp))

                // ── Transcript list ───────────────────────────────────────
                if (transcript.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO TRANSCRIPT YET",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.4.sp,
                                color = textSecondary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Send a command to start logging",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.sp,
                                color = textSecondary.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    val listState = rememberLazyListState()

                    LaunchedEffect(transcript.size) {
                        if (transcript.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        reverseLayout = false
                    ) {
                        items(
                            items = transcript,
                            key = { it.timestampMs }
                        ) { message ->
                            ChatMessageBubble(
                                message = message,
                                userBubbleColor = userBubble,
                                assistantBubbleColor = assistantBubble,
                                accentColor = accentColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }
                    }
                }

                // ── Latest response strip ─────────────────────────────────
                if (latestResponse.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CutCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.08f))
                            .border(0.5.dp, accentColor.copy(alpha = 0.25f), CutCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = latestResponse,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: TranscriptMessage,
    userBubbleColor: Color,
    assistantBubbleColor: Color,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val isUser = message.speaker == TranscriptMessage.Speaker.USER
    val bubbleColor = if (isUser) userBubbleColor else assistantBubbleColor
    val speakerLabel = if (isUser) "YOU" else "N.E.R.F."
    val speakerColor = if (isUser) textSecondary else accentColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(6.dp))
            .background(bubbleColor)
            .border(0.5.dp, accentColor.copy(alpha = 0.15f), CutCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = speakerLabel,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 1.4.sp,
            color = speakerColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = message.text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.3.sp,
            color = textPrimary
        )
    }
}

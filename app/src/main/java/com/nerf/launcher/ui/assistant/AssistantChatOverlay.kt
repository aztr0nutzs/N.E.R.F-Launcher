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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantChatOverlayMapped
//
//  Chat transcript overlay clipped to the exact panelOuter / transcriptRegion
//  bounds from AssistantOverlayMap, mapped through [imageRect].
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantChatOverlayMapped(
    transcript: List<TranscriptMessage>,
    latestResponse: String,
    palette: AssistantThemePalette,
    isVisible: Boolean,
    imageRect: Rect,
    modifier: Modifier = Modifier
) {
    val panelBg        = Color(palette.chatPanelBg)
    val borderColor    = Color(palette.chatPanelBorder)
    val textPrimary    = Color(palette.textPrimary)
    val textSecondary  = Color(palette.textSecondary)
    val userBubble     = Color(palette.chatUserBubble)
    val assistantBubble = Color(palette.chatAssistantBubble)
    val accentColor    = Color(palette.controlAccent)

    val density  = LocalDensity.current

    // Map panelOuter to screen pixels
    val panelPx  = AssistantOverlayMap.panelOuter.toPx(imageRect)
    val transcriptPx = AssistantOverlayMap.transcriptRegion.toPx(imageRect)

    AnimatedVisibility(
        visible  = isVisible,
        enter    = fadeIn(initialAlpha = 0.3f) + slideInVertically { it / 4 },
        exit     = fadeOut() + slideOutVertically { it / 4 },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Panel outer shell ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .offset { IntOffset(panelPx.left.toInt(), panelPx.top.toInt()) }
                    .size(
                        width  = with(density) { panelPx.width.toDp() },
                        height = with(density) { panelPx.height.toDp() }
                    )
                    .clip(CutCornerShape(8.dp))
                    .background(panelBg)
                    .border(1.dp, borderColor, CutCornerShape(8.dp))
            )

            // ── Transcript region ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .offset { IntOffset(transcriptPx.left.toInt(), transcriptPx.top.toInt()) }
                    .size(
                        width  = with(density) { transcriptPx.width.toDp() },
                        height = with(density) { transcriptPx.height.toDp() }
                    )
                    .clip(CutCornerShape(6.dp))
                    .background(panelBg.copy(alpha = 0.92f))
                    .border(0.5.dp, borderColor, CutCornerShape(6.dp))
                    .padding(4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentColor)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text          = "TRANSCRIPT",
                            fontFamily    = FontFamily.Monospace,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 8.sp,
                            letterSpacing = 1.4.sp,
                            color         = accentColor
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text          = "${transcript.size} MSG",
                            fontFamily    = FontFamily.Monospace,
                            fontWeight    = FontWeight.Medium,
                            fontSize      = 8.sp,
                            color         = textSecondary
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    if (transcript.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text          = "AWAITING INPUT",
                                fontFamily    = FontFamily.Monospace,
                                fontWeight    = FontWeight.Bold,
                                fontSize      = 9.sp,
                                letterSpacing = 1.4.sp,
                                color         = textSecondary.copy(alpha = 0.55f)
                            )
                        }
                    } else {
                        val listState = rememberLazyListState()

                        LaunchedEffect(transcript.size) {
                            if (transcript.isNotEmpty()) {
                                listState.animateScrollToItem(transcript.lastIndex)
                            }
                        }

                        LazyColumn(
                            state          = listState,
                            modifier       = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(items = transcript, key = { it.timestampMs }) { msg ->
                                ChatBubble(
                                    message         = msg,
                                    userColor       = userBubble,
                                    assistantColor  = assistantBubble,
                                    accentColor     = accentColor,
                                    textPrimary     = textPrimary,
                                    textSecondary   = textSecondary
                                )
                            }
                        }
                    }

                    // Latest response strip
                    if (latestResponse.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.07f))
                                .border(0.5.dp, accentColor.copy(alpha = 0.22f), CutCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text       = latestResponse,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 9.sp,
                                lineHeight = 13.sp,
                                color      = textPrimary,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Chat bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(
    message: TranscriptMessage,
    userColor: Color,
    assistantColor: Color,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val isUser  = message.speaker == TranscriptMessage.Speaker.USER
    val bg      = if (isUser) userColor else assistantColor
    val speaker = if (isUser) "YOU" else "N.E.R.F."
    val spColor = if (isUser) textSecondary else accentColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(5.dp))
            .background(bg)
            .border(0.5.dp, accentColor.copy(alpha = 0.13f), CutCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text       = speaker,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize   = 7.sp,
            letterSpacing = 1.2.sp,
            color      = spColor
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text       = message.text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize   = 10.sp,
            lineHeight = 14.sp,
            color      = textPrimary
        )
    }
}

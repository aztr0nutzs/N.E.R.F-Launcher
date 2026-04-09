package com.nerf.launcher.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantTopBar
//
//  Top bar: dismiss button, theme name, LOG toggle, SKIN cycle, plus the
//  theme-switcher strip docked at the top-right corner.
//  Lives in layer 5 above all other overlays.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AssistantTopBar(
    palette: AssistantThemePalette,
    themeName: String,
    onDismiss: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleChat: () -> Unit,
    activeThemeId: AssistantThemeId,
    onSwitchTheme: (AssistantThemeId) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent  = Color(palette.controlAccent)
    val surface = Color(palette.controlSurface)
    val textSec = Color(palette.textSecondary)

    Box(modifier = modifier) {
        // ── Main top bar ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(CutCornerShape(8.dp))
                .background(surface)
                .border(0.5.dp, accent.copy(alpha = 0.18f), CutCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text          = "ASSISTANT LINK",
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.Bold,
                fontSize      = 10.sp,
                letterSpacing = 1.6.sp,
                color         = accent
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text       = themeName,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize   = 8.sp,
                color      = textSec
            )
            Spacer(Modifier.weight(1f))

            // LOG toggle
            TopBarChip(label = "LOG", accent = accent, onClick = onToggleChat)
            Spacer(Modifier.width(4.dp))

            // Theme cycle
            TopBarChip(label = "SKIN", accent = accent, onClick = onCycleTheme)
            Spacer(Modifier.width(4.dp))

            // Dismiss
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CutCornerShape(6.dp))
                    .background(Color(0x33FF4444))
                    .border(0.5.dp, Color(0x66FF4444), CutCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "✕",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 11.sp,
                    color      = Color(0xFFFF6A6A)
                )
            }
        }

        // ── Theme switcher strip (top-right) ──────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 14.dp)
                .clip(CutCornerShape(6.dp))
                .background(surface)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            AssistantThemeId.values().forEach { themeId ->
                val isActive    = themeId == activeThemeId
                val themeConfig = AssistantThemeRegistry.get(themeId)
                val dot         = Color(themeConfig.palette.controlAccent)

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isActive) dot.copy(alpha = 0.28f) else Color.Transparent)
                        .border(
                            width  = if (isActive) 1.5.dp else 0.5.dp,
                            color  = dot.copy(alpha = if (isActive) 0.90f else 0.28f),
                            shape  = RoundedCornerShape(4.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onSwitchTheme(themeId) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(dot.copy(alpha = if (isActive) 0.90f else 0.38f))
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarChip(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CutCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text          = label,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            fontSize      = 8.sp,
            letterSpacing = 1.2.sp,
            color         = accent.copy(alpha = 0.80f)
        )
    }
}

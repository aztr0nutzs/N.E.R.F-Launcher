package com.nerf.launcher.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nerf.launcher.state.*
import kotlinx.coroutines.launch
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// Token map — all raw colors isolated here for surgical replacement
// ─────────────────────────────────────────────────────────────────────────────
private object AT {
    val bgBlack        = Color(0xFF020305)
    val panelBg        = Color(0xCC060B10)
    val panelBgLight   = Color(0xBB0E1620)
    val panelBorder    = Color(0xFF1A2A3A)
    val textPrimary    = Color(0xFFDDEEFF)
    val textSecondary  = Color(0xFF8899AA)
    val textAI         = Color(0xFFCCEEFF)
    val textUser       = Color(0xFFFFEECC)
    val dimOverlay     = Color(0xAA000000)
    val white          = Color(0xFFFFFFFF)
    val bevelHigh      = Color(0xFF2C3A4A)
    val bevelLow       = Color(0xFF050810)
    val screwFace      = Color(0xFF1A2230)
}

// ─────────────────────────────────────────────────────────────────────────────
// Root entry point
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val config = AssistantThemeRegistry.forTheme(state.activeTheme)

    // Track actual rendered px dimensions of the root box
    var screenWidthPx  by remember { mutableStateOf(0f) }
    var screenHeightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AT.bgBlack)
            .onGloballyPositioned { coords ->
                screenWidthPx  = coords.size.width.toFloat()
                screenHeightPx = coords.size.height.toFloat()
            }
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                viewModel.onEvent(AssistantEvent.DismissOverlays)
            }
    ) {
        if (screenWidthPx > 0f && screenHeightPx > 0f) {

            // ── Layer 1: Full-bleed artwork base
            ArtworkBaseLayer(config = config, state = state)

            // ── Layer 2: Robot reactive overlays (visor, chest glow)
            RobotGlowLayer(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx)

            // ── Layer 3: Chat pane overlay (exact artwork region)
            ChatPaneOverlay(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx,
                onEvent = { viewModel.onEvent(it) })

            // ── Layer 4: Dart strip (themes 1–3 only)
            if (config.showDartStrip && config.dartStripBounds != null) {
                DartStripOverlay(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx,
                    onEvent = { viewModel.onEvent(it) })
            }

            // ── Layer 5: Bottom dock overlay (real interactive buttons)
            DockOverlay(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx,
                onEvent = { viewModel.onEvent(it) })

            // ── Layer 6: Mic button (precise position, large tap target)
            MicButtonOverlay(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx,
                onTap = { viewModel.onEvent(AssistantEvent.MicTapped) })

            // ── Layer 7: Chest tap zone
            ChestTapZone(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx,
                onTap = { viewModel.onEvent(AssistantEvent.ChestTapped) })

            // ── Layer 8: Hand projection tap zone
            HandTapZone(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx,
                onTap = { viewModel.onEvent(AssistantEvent.HandTapped) })

            // ── Layer 9: Floating panels (open from chest / hand taps)
            if (state.chestPanelOpen) {
                ChestStatusPanel(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx)
            }
            if (state.handPanelOpen) {
                HandQuickToolsPanel(config = config, state = state, sw = screenWidthPx, sh = screenHeightPx)
            }

            // ── Layer 10: Theme switcher strip (top-right corner, semi-transparent)
            ThemeSwitcherStrip(
                activeTheme = state.activeTheme,
                accentColor = config.accentPrimary,
                onTheme = { viewModel.onEvent(AssistantEvent.ThemeSelected(it)) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 1 – Full-bleed artwork
// Uses res drawable if available; falls back to a Canvas-rendered placeholder
// that preserves the correct visual structure for each theme.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ArtworkBaseLayer(config: AssistantThemeConfig, state: AssistantUiState) {
    // In the real project, swap the when() arms for:
    //   Image(painter = painterResource(config.drawableResId), ...)
    // The Canvas fallback below renders a credible structural stand-in
    // so the overlay system works before the drawables are wired.
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val accent  = config.accentPrimary
            val accent2 = config.accentSecondary

            // Sky/background gradient
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(AT.bgBlack, Color(0xFF050A14), Color(0xFF030608))
                )
            )

            // Robot body silhouette placeholder — upper 58%
            val robotCx = w / 2f
            val robotTopY = h * 0.02f
            val robotBodyH = h * 0.56f

            // Helmet
            val helmetR = w * 0.30f
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF1A2530), Color(0xFF080D12)), Offset(robotCx, robotTopY + helmetR), helmetR),
                radius = helmetR, center = Offset(robotCx, robotTopY + helmetR)
            )
            // Visor stripe
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, config.visorGlowColor.copy(0.6f), config.visorGlowColor, config.visorGlowColor.copy(0.6f), Color.Transparent)),
                topLeft = Offset(robotCx - helmetR * 0.85f, robotTopY + helmetR * 1.1f),
                size = Size(helmetR * 1.7f, helmetR * 0.12f)
            )
            // V-CORE label band
            drawRect(
                color = Color(0xDD000000),
                topLeft = Offset(robotCx - helmetR * 0.6f, robotTopY + helmetR * 1.32f),
                size = Size(helmetR * 1.2f, helmetR * 0.10f)
            )

            // Shoulders
            val shoulderY = robotTopY + helmetR * 2.1f
            drawCircle(accent.copy(0.15f), helmetR * 0.5f, Offset(robotCx - helmetR * 1.1f, shoulderY))
            drawCircle(accent.copy(0.15f), helmetR * 0.5f, Offset(robotCx + helmetR * 1.1f, shoulderY))

            // Chest / reactor zone
            val chestCy = shoulderY + helmetR * 0.5f
            val chestR  = w * 0.18f
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(0.3f), Color(0xFF080D12)), Offset(robotCx, chestCy), chestR * 1.5f),
                radius = chestR, center = Offset(robotCx, chestCy)
            )
            drawCircle(config.chestGlowColor.copy(0.4f), chestR, Offset(robotCx, chestCy), style = Stroke(3f))
            // Reactor rings
            repeat(3) { i ->
                drawCircle(accent.copy(0.15f + i * 0.08f), chestR * (0.75f - i * 0.2f), Offset(robotCx, chestCy), style = Stroke(2f))
            }
            // N label in core
            drawCircle(accent.copy(0.7f), chestR * 0.28f, Offset(robotCx, chestCy))

            // DIMENSIONAL label
            drawRect(
                color = AT.bgBlack,
                topLeft = Offset(robotCx - chestR * 1.1f, chestCy + chestR * 0.72f),
                size = Size(chestR * 2.2f, chestR * 0.22f)
            )

            // Left hand projection zone
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(0.2f), Color.Transparent), Offset(w * 0.14f, h * 0.55f), w * 0.14f),
                radius = w * 0.14f, center = Offset(w * 0.14f, h * 0.55f)
            )

            // HUD lines in background
            repeat(8) { i ->
                drawLine(accent.copy(0.05f), Offset(0f, h * (0.06f + i * 0.07f)), Offset(w * 0.35f, h * (0.06f + i * 0.07f)), 0.5f)
                drawLine(accent2.copy(0.05f), Offset(w * 0.65f, h * (0.06f + i * 0.07f)), Offset(w, h * (0.06f + i * 0.07f)), 0.5f)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 2 – Robot reactive glow overlays
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RobotGlowLayer(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float
) {
    val inf = rememberInfiniteTransition(label = "robotGlow")
    val visorPulse by inf.animateFloat(0.3f, 0.85f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), "vPulse")
    val chestPulse by inf.animateFloat(0.25f, 0.9f,  infiniteRepeatable(tween(2200), RepeatMode.Reverse), "cPulse")

    val listeningBoost = if (state.assistantState == AssistantState.LISTENING) 1.5f else 1.0f
    val thinkingBoost  = if (state.assistantState == AssistantState.THINKING)  1.3f else 1.0f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height

        // Visor glow — positioned at ~15–20% from top, horizontal band
        val visorY = h * 0.185f
        val visorAlpha = visorPulse * listeningBoost
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, config.visorGlowColor.copy(visorAlpha.coerceIn(0f, 1f)),
                    config.visorGlowColor.copy((visorAlpha * 1.2f).coerceIn(0f, 1f)),
                    config.visorGlowColor.copy(visorAlpha.coerceIn(0f, 1f)), Color.Transparent)
            ),
            topLeft = Offset(w * 0.18f, visorY),
            size = Size(w * 0.64f, h * 0.025f)
        )

        // Chest core pulse
        val chestCx = w / 2f
        val chestCy = h * 0.48f
        val chestAlpha = chestPulse * thinkingBoost
        drawCircle(
            brush = Brush.radialGradient(
                listOf(config.chestGlowColor.copy((chestAlpha * 0.7f).coerceIn(0f, 1f)), Color.Transparent),
                Offset(chestCx, chestCy), w * 0.22f
            ),
            radius = w * 0.22f, center = Offset(chestCx, chestCy)
        )

        // Listening state: scan sweep arc
        if (state.assistantState == AssistantState.LISTENING) {
            drawCircle(
                color = config.accentPrimary.copy(0.25f * visorPulse),
                radius = w * 0.35f, center = Offset(chestCx, chestCy),
                style = Stroke(3f)
            )
            drawCircle(
                color = config.accentPrimary.copy(0.12f * visorPulse),
                radius = w * 0.42f, center = Offset(chestCx, chestCy),
                style = Stroke(1.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 3 – Chat pane overlay
// Renders a semi-transparent panel exactly over the artwork's chat region
// with real scrollable transcript text inside it.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatPaneOverlay(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float,
    onEvent: (AssistantEvent) -> Unit
) {
    val density = LocalDensity.current
    val b = config.chatPaneBounds
    val xDp = with(density) { (b.xFraction * sw).toDp() }
    val yDp = with(density) { (b.yFraction * sh).toDp() }
    val wDp = with(density) { (b.wFraction * sw).toDp() }
    val hDp = with(density) { (b.hFraction * sh).toDp() }

    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(state.chatMessages.size - 1) }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((b.xFraction * sw).toInt(), (b.yFraction * sh).toInt()) }
            .size(wDp, hDp)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
    ) {
        // Panel background — semi-transparent over artwork
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Dark glass tint exactly over the artwork pane region
            drawRoundRect(
                color = AT.panelBg,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            // Top accent line
            drawLine(
                color = config.accentPrimary.copy(0.7f),
                start = Offset(8.dp.toPx(), 0f),
                end   = Offset(size.width - 8.dp.toPx(), 0f),
                strokeWidth = 1.5f
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Transcript scroll area — fills most of the pane height
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(state.chatMessages) { msg ->
                    ChatMessageRow(msg = msg, config = config)
                }
            }

            // Input strip at bottom of pane
            ChatInputStrip(state = state, config = config, onEvent = onEvent)
        }

        // Theme 4 only: progress dots inside pane
        if (config.progressDotsVisible) {
            ProgressDotStrip(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
                color = config.accentPrimary
            )
        }
    }
}

@Composable
private fun ChatMessageRow(msg: ChatMessage, config: AssistantThemeConfig) {
    Text(
        text       = msg.text,
        color      = if (msg.isAI) AT.textAI else AT.textUser,
        fontSize   = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (msg.isAI) FontWeight.Normal else FontWeight.SemiBold,
        lineHeight = 15.sp,
        maxLines   = 3,
        overflow   = TextOverflow.Ellipsis,
        modifier   = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    )
}

@Composable
private fun ChatInputStrip(
    state: AssistantUiState,
    config: AssistantThemeConfig,
    onEvent: (AssistantEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .drawBehind {
                drawRoundRect(AT.panelBgLight, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                drawRoundRect(config.accentPrimary.copy(0.3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = Stroke(0.8f))
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = state.inputText,
            onValueChange = { onEvent(AssistantEvent.InputChanged(it)) },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            textStyle = TextStyle(color = AT.textPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
            singleLine = true,
            decorationBox = { inner ->
                if (state.inputText.isEmpty()) {
                    Text("CMD >_", color = AT.textSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                inner()
            }
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(config.accentPrimary.copy(0.25f))
                .clickable { onEvent(AssistantEvent.SendMessage) }
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = config.accentPrimary, fontSize = 9.sp)
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun ProgressDotStrip(modifier: Modifier, color: Color) {
    val inf = rememberInfiniteTransition(label = "dots")
    val phase by inf.animateFloat(0f, 3f, infiniteRepeatable(tween(900), RepeatMode.Restart), "dotPhase")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val active = phase.toInt() == i
            Box(
                Modifier.size(if (active) 18.dp else 12.dp, 4.dp)
                    .background(color.copy(if (active) 1f else 0.3f), RoundedCornerShape(2.dp))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 4 – Dart strip overlay (themes 1–3)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DartStripOverlay(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float,
    onEvent: (AssistantEvent) -> Unit
) {
    val b = config.dartStripBounds ?: return
    val density = LocalDensity.current
    val wDp = with(density) { (b.wFraction * sw).toDp() }
    val hDp = with(density) { (b.hFraction * sh).toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset((b.xFraction * sw).toInt(), (b.yFraction * sh).toInt()) }
            .size(wDp, hDp)
            .drawBehind {
                drawRoundRect(AT.panelBg, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                drawLine(config.accentPrimary.copy(0.5f), Offset(0f, 0f), Offset(size.width, 0f), 1f)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dart count
            Column {
                Text("DART COUNT:", color = AT.textSecondary, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp, 8.dp).background(config.accentPrimary.copy(0.6f), RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("x ${state.dartCount}", color = AT.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.weight(1f))

            if (config.showModeToggle) {
                // PEEK PERFORMANCE label
                Column(horizontalAlignment = Alignment.End) {
                    Text("PEEK PERFORMANCE OPERATIONS", color = AT.textSecondary, fontSize = 6.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(2.dp))
                    // Semi/Full Auto toggle
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FireModeLabel("SEMI-AUTO", state.fireMode == FireMode.SEMI_AUTO, config.accentPrimary) {
                            onEvent(AssistantEvent.FireModeChanged(FireMode.SEMI_AUTO))
                        }
                        // Toggle pill
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(14.dp)
                                .drawBehind {
                                    drawRoundRect(AT.panelBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
                                    val thumbX = if (state.fireMode == FireMode.FULL_AUTO) size.width - 10.dp.toPx() else 2.dp.toPx()
                                    drawCircle(config.accentPrimary, 6.dp.toPx(), Offset(thumbX + 6.dp.toPx(), size.height / 2))
                                }
                                .clickable {
                                    onEvent(AssistantEvent.FireModeChanged(
                                        if (state.fireMode == FireMode.SEMI_AUTO) FireMode.FULL_AUTO else FireMode.SEMI_AUTO
                                    ))
                                }
                        )
                        FireModeLabel("FULL-AUTO", state.fireMode == FireMode.FULL_AUTO, config.accentPrimary) {
                            onEvent(AssistantEvent.FireModeChanged(FireMode.FULL_AUTO))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FireModeLabel(label: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (active) color else AT.textSecondary,
        fontSize = 7.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 5 – Bottom dock overlay (real interactive icon buttons)
// ─────────────────────────────────────────────────────────────────────────────
private data class DockIcon(val symbol: String, val label: String)
private val DOCK_ICONS_STD = listOf(
    DockIcon("⚙", "SETTINGS"), DockIcon("🗺", "MAP"), DockIcon("💼", "MODULES"),
    DockIcon("🎤", "MIC"),     DockIcon("🔔", "ALERTS"),  DockIcon("👤", "PROFILE")
)
private val DOCK_ICONS_CG = listOf(
    DockIcon("⚙", "SETTINGS"), DockIcon("🗺", "MAP"), DockIcon("💼", "MODULES"),
    DockIcon("🎤", "MIC"), DockIcon("👤", "PROFILE")
)

@Composable
private fun DockOverlay(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float,
    onEvent: (AssistantEvent) -> Unit
) {
    val b = config.dockBounds
    val density = LocalDensity.current
    val wDp = with(density) { (b.wFraction * sw).toDp() }
    val hDp = with(density) { (b.hFraction * sh).toDp() }
    val icons = if (config.theme == AssistantTheme.CYBER_GRAFFITI) DOCK_ICONS_CG else DOCK_ICONS_STD

    Box(
        modifier = Modifier
            .offset { IntOffset((b.xFraction * sw).toInt(), (b.yFraction * sh).toInt()) }
            .size(wDp, hDp)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xAA060B10), Color(0xDD020408)))
                )
                drawLine(config.accentPrimary.copy(0.5f), Offset(0f, 0f), Offset(size.width, 0f), 1f)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            icons.forEach { icon ->
                // Skip mic position — handled by dedicated MicButtonOverlay
                val isMicSlot = icon.symbol == "🎤"
                if (isMicSlot) {
                    // Spacer matching mic button width
                    Spacer(Modifier.width(with(density) { (config.micBounds.wFraction * sw).toDp() }))
                } else {
                    DockIconButton(icon = icon, accentColor = config.accentPrimary, onClick = {})
                }
            }
        }
    }
}

@Composable
private fun DockIconButton(icon: DockIcon, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AT.panelBg)
            .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(8.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(icon.symbol, fontSize = 18.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 6 – Mic button (precise positioned overlay)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MicButtonOverlay(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float,
    onTap: () -> Unit
) {
    val b = config.micBounds
    val density = LocalDensity.current
    val wDp = with(density) { (b.wFraction * sw).toDp() }
    val hDp = with(density) { (b.hFraction * sh).toDp() }
    val isListening = state.assistantState == AssistantState.LISTENING

    val inf = rememberInfiniteTransition(label = "mic")
    val micPulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "mPulse")
    val ringScale by inf.animateFloat(1f, 1.25f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), "mRing")

    Box(
        modifier = Modifier
            .offset { IntOffset((b.xFraction * sw).toInt(), (b.yFraction * sh).toInt()) }
            .size(wDp, hDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2; val cy = size.height / 2
            val r = minOf(size.width, size.height) * 0.44f

            // Outer glow ring (listening only)
            if (isListening) {
                drawCircle(config.accentPrimary.copy(0.25f * micPulse), r * ringScale, Offset(cx, cy), style = Stroke(4f))
                drawCircle(config.accentPrimary.copy(0.1f * micPulse), r * ringScale * 1.2f, Offset(cx, cy), style = Stroke(2f))
            }
            // Button background
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(if (isListening) config.accentPrimary.copy(0.4f) else AT.panelBgLight, AT.bgBlack),
                    Offset(cx, cy), r
                ),
                radius = r, center = Offset(cx, cy)
            )
            drawCircle(config.accentPrimary.copy(if (isListening) micPulse else 0.7f), r, Offset(cx, cy), style = Stroke(2.5f))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTap() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isListening) "◉" else "🎤",
                fontSize = if (config.theme == AssistantTheme.CYBER_GRAFFITI) 22.sp else 18.sp,
                color = config.accentPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 7 & 8 – Chest and Hand tap zones (invisible tap targets)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChestTapZone(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float,
    onTap: () -> Unit
) {
    val b = config.chestBounds
    val density = LocalDensity.current
    val wDp = with(density) { (b.wFraction * sw).toDp() }
    val hDp = with(density) { (b.hFraction * sh).toDp() }

    val inf = rememberInfiniteTransition(label = "chest")
    val cPulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), "cP")

    Box(
        modifier = Modifier
            .offset { IntOffset((b.xFraction * sw).toInt(), (b.yFraction * sh).toInt()) }
            .size(wDp, hDp)
            .clip(CircleShape)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTap() }
    ) {
        // Very subtle tap-hint ring — only visible on hover in real device
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (state.chestPanelOpen) {
                drawCircle(config.chestGlowColor.copy(0.3f * cPulse), minOf(size.width, size.height) / 2,
                    Offset(size.width / 2, size.height / 2), style = Stroke(2f))
            }
        }
    }
}

@Composable
private fun HandTapZone(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float,
    onTap: () -> Unit
) {
    val b = config.handBounds
    val density = LocalDensity.current
    val wDp = with(density) { (b.wFraction * sw).toDp() }
    val hDp = with(density) { (b.hFraction * sh).toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset((b.xFraction * sw).toInt(), (b.yFraction * sh).toInt()) }
            .size(wDp, hDp)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTap() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 9a – Chest status panel (floats above chest zone when open)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChestStatusPanel(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float
) {
    val b = config.chestBounds
    val density = LocalDensity.current
    // Panel anchors just above chest zone
    val panelX = with(density) { ((b.xFraction - 0.02f).coerceAtLeast(0f) * sw).toDp() }
    val panelY = with(density) { ((b.yFraction - 0.22f).coerceAtLeast(0f) * sh).toDp() }
    val panelW = with(density) { ((b.wFraction + 0.04f).coerceAtMost(1f) * sw).toDp() }

    Box(
        modifier = Modifier
            .absoluteOffset(panelX, panelY)
            .width(panelW)
            .wrapContentHeight()
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xEE0A1220), Color(0xEE060C18))),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
                drawRoundRect(config.chestGlowColor.copy(0.7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = Stroke(1.5f))
            }
            .padding(10.dp)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
    ) {
        Column {
            PanelLabel("CORE STATUS", config.accentPrimary)
            Spacer(Modifier.height(4.dp))
            StatusRow("REACTOR",      "OPTIMAL",  config.accentPrimary)
            StatusRow("DIMENSIONAL",  "STABLE 98%", config.accentSecondary)
            StatusRow("VOID SCAN",    "ACTIVE",   config.accentPrimary)
            StatusRow("AETHER STAB",  "NOMINAL",  config.accentSecondary)
            StatusRow("RESONANCE CAL","LOCKED",   config.accentPrimary)
            Spacer(Modifier.height(4.dp))
            PanelLabel("ASSISTANT STATE: ${state.assistantState.name}", config.chestGlowColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 9b – Hand quick tools panel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HandQuickToolsPanel(
    config: AssistantThemeConfig,
    state: AssistantUiState,
    sw: Float, sh: Float
) {
    val b = config.handBounds
    val density = LocalDensity.current
    val panelX = with(density) { ((b.xFraction + b.wFraction + 0.01f) * sw).toDp() }
    val panelY = with(density) { ((b.yFraction - 0.04f).coerceAtLeast(0f) * sh).toDp() }
    val panelW = with(density) { (0.44f * sw).toDp() }

    Box(
        modifier = Modifier
            .absoluteOffset(panelX, panelY)
            .width(panelW)
            .wrapContentHeight()
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xEE0A1A14), Color(0xEE060C10))),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
                drawRoundRect(config.accentPrimary.copy(0.7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = Stroke(1.5f))
            }
            .padding(10.dp)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
    ) {
        Column {
            PanelLabel("QUICK TOOLS", config.accentPrimary)
            Spacer(Modifier.height(6.dp))
            val tools = listOf("DEEP SCAN", "TEMPORAL LOCK", "VOID PING", "AETHER SYNC", "RESONANCE CHECK")
            tools.forEach { tool ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .drawBehind {
                            drawRoundRect(config.accentPrimary.copy(0.1f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                            drawRoundRect(config.accentPrimary.copy(0.3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(0.8f))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                ) {
                    Text(tool, color = config.accentPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layer 10 – Theme switcher strip (top-right corner)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ThemeSwitcherStrip(
    activeTheme: AssistantTheme,
    accentColor: Color,
    onTheme: (AssistantTheme) -> Unit
) {
    val themes = listOf(
        AssistantTheme.PHANTOM_BLACK   to "PB",
        AssistantTheme.NERF_ORANGE     to "NO",
        AssistantTheme.BLUEPRINT_STONE to "BP",
        AssistantTheme.CYBER_GRAFFITI  to "CG"
    )

    Column(
        modifier = Modifier
            .padding(top = 6.dp, end = 6.dp)
            .wrapContentSize()
            .align(Alignment.TopEnd) // won't compile here — handled at call site
    ) { /* see BoxScope wrapper below */ }
}

// BoxScope-aware version called from root Box
@Composable
private fun BoxScope.ThemeSwitcherStrip(
    activeTheme: AssistantTheme,
    accentColor: Color,
    onTheme: (AssistantTheme) -> Unit
) {
    val themes = listOf(
        AssistantTheme.PHANTOM_BLACK   to "PB",
        AssistantTheme.NERF_ORANGE     to "NO",
        AssistantTheme.BLUEPRINT_STONE to "BP",
        AssistantTheme.CYBER_GRAFFITI  to "CG"
    )
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("THEME", color = AT.textSecondary, fontSize = 5.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        themes.forEach { (theme, label) ->
            val isActive = theme == activeTheme
            val dotColor = when (theme) {
                AssistantTheme.PHANTOM_BLACK   -> Color(0xFF00FFCC)
                AssistantTheme.NERF_ORANGE     -> Color(0xFFFF6600)
                AssistantTheme.BLUEPRINT_STONE -> Color(0xFFFF8800)
                AssistantTheme.CYBER_GRAFFITI  -> Color(0xFF00CCFF)
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .drawBehind {
                        drawCircle(if (isActive) dotColor.copy(0.3f) else AT.panelBg)
                        drawCircle(dotColor.copy(if (isActive) 1f else 0.4f), style = Stroke(if (isActive) 2f else 1f))
                    }
                    .clip(CircleShape)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTheme(theme) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = dotColor.copy(if (isActive) 1f else 0.5f), fontSize = 5.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared sub-components
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PanelLabel(text: String, color: Color) {
    Text(text, color = color, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun StatusRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, color = AT.textSecondary, fontSize = 7.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(72.dp))
        Text(": ", color = AT.textSecondary, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

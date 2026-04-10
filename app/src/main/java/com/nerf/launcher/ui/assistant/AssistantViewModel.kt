package com.nerf.launcher.state

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Theme Configuration Registry
// These fractional bounds are calibrated against each artwork's exact composition.
// xFraction/yFraction = top-left corner as fraction of screen dimensions.
// wFraction/hFraction = size as fraction of screen dimensions.
// ─────────────────────────────────────────────────────────────────────────────
object AssistantThemeRegistry {

    // ── THEME 1: Phantom Black (dark holographic neon)
    // Chat pane: ~y=0.57 to 0.82 of screen, full width with padding
    // Dart strip: ~y=0.82 to 0.88
    // Dock: ~y=0.88 to 0.97
    // Mic: center of dock at ~x=0.45, y=0.89, ~10% wide
    val PHANTOM_BLACK = AssistantThemeConfig(
        theme              = AssistantTheme.PHANTOM_BLACK,
        drawableResId      = 0, // placeholder — wire to R.drawable.asst_screen1 in project
        accentPrimary      = Color(0xFF00FFCC),
        accentSecondary    = Color(0xFFFF00FF),
        accentGlow         = Color(0x8800FFCC),
        chatPaneBounds     = OverlayBounds(0.01f, 0.565f, 0.98f, 0.255f),
        dockBounds         = OverlayBounds(0.00f, 0.883f, 1.00f, 0.095f),
        micBounds          = OverlayBounds(0.38f, 0.883f, 0.24f, 0.095f),
        chestBounds        = OverlayBounds(0.28f, 0.40f, 0.44f, 0.18f),
        handBounds         = OverlayBounds(0.00f, 0.46f, 0.28f, 0.18f),
        dartStripBounds    = OverlayBounds(0.01f, 0.824f, 0.98f, 0.058f),
        chatTextYStart     = 0.03f,
        chatTextXPad       = 0.03f,
        showDartStrip      = true,
        showModeToggle     = true,
        progressDotsVisible = false,
        visorGlowColor     = Color(0x9900FFCC),
        chestGlowColor     = Color(0x99FF00FF)
    )

    // ── THEME 2: NERF Orange (orange / navy)
    // Same structural layout as theme 1, different palette
    val NERF_ORANGE = AssistantThemeConfig(
        theme              = AssistantTheme.NERF_ORANGE,
        drawableResId      = 0, // wire to R.drawable.asst_screen2
        accentPrimary      = Color(0xFFFF6600),
        accentSecondary    = Color(0xFF0066CC),
        accentGlow         = Color(0x88FF6600),
        chatPaneBounds     = OverlayBounds(0.01f, 0.565f, 0.98f, 0.255f),
        dockBounds         = OverlayBounds(0.00f, 0.883f, 1.00f, 0.095f),
        micBounds          = OverlayBounds(0.38f, 0.883f, 0.24f, 0.095f),
        chestBounds        = OverlayBounds(0.28f, 0.40f, 0.44f, 0.18f),
        handBounds         = OverlayBounds(0.00f, 0.46f, 0.28f, 0.18f),
        dartStripBounds    = OverlayBounds(0.01f, 0.824f, 0.98f, 0.058f),
        chatTextYStart     = 0.03f,
        chatTextXPad       = 0.03f,
        showDartStrip      = true,
        showModeToggle     = true,
        progressDotsVisible = false,
        visorGlowColor     = Color(0x9900AAFF),
        chestGlowColor     = Color(0x99FF6600)
    )

    // ── THEME 3: Blueprint Stone (orange/navy, blueprint bg)
    // Identical composition to theme 2; background asset differs only
    val BLUEPRINT_STONE = AssistantThemeConfig(
        theme              = AssistantTheme.BLUEPRINT_STONE,
        drawableResId      = 0, // wire to R.drawable.asst_screen3
        accentPrimary      = Color(0xFFFF6600),
        accentSecondary    = Color(0xFF2255AA),
        accentGlow         = Color(0x88FF8800),
        chatPaneBounds     = OverlayBounds(0.01f, 0.565f, 0.98f, 0.255f),
        dockBounds         = OverlayBounds(0.00f, 0.883f, 1.00f, 0.095f),
        micBounds          = OverlayBounds(0.38f, 0.883f, 0.24f, 0.095f),
        chestBounds        = OverlayBounds(0.28f, 0.40f, 0.44f, 0.18f),
        handBounds         = OverlayBounds(0.00f, 0.46f, 0.28f, 0.18f),
        dartStripBounds    = OverlayBounds(0.01f, 0.824f, 0.98f, 0.058f),
        chatTextYStart     = 0.03f,
        chatTextXPad       = 0.03f,
        showDartStrip      = true,
        showModeToggle     = true,
        progressDotsVisible = false,
        visorGlowColor     = Color(0x9900CCFF),
        chestGlowColor     = Color(0x99FF6600)
    )

    // ── THEME 4: Cyber Graffiti (dark, mic-centric layout)
    // Layout differs: no dart strip, mic is large center of dock,
    // chat pane extends higher, progress dots shown inside pane
    val CYBER_GRAFFITI = AssistantThemeConfig(
        theme              = AssistantTheme.CYBER_GRAFFITI,
        drawableResId      = 0, // wire to R.drawable.asst_screen4
        accentPrimary      = Color(0xFF00CCFF),
        accentSecondary    = Color(0xFFFF00AA),
        accentGlow         = Color(0x8800CCFF),
        chatPaneBounds     = OverlayBounds(0.01f, 0.535f, 0.98f, 0.295f),
        dockBounds         = OverlayBounds(0.00f, 0.895f, 1.00f, 0.090f),
        micBounds          = OverlayBounds(0.36f, 0.877f, 0.28f, 0.108f),
        chestBounds        = OverlayBounds(0.24f, 0.34f, 0.52f, 0.22f),
        handBounds         = OverlayBounds(0.00f, 0.46f, 0.26f, 0.18f),
        dartStripBounds    = null,
        chatTextYStart     = 0.03f,
        chatTextXPad       = 0.02f,
        showDartStrip      = false,
        showModeToggle     = false,
        progressDotsVisible = true,
        visorGlowColor     = Color(0x9900FFCC),
        chestGlowColor     = Color(0xAAFF00AA)
    )

    fun forTheme(theme: AssistantTheme): AssistantThemeConfig = when (theme) {
        AssistantTheme.PHANTOM_BLACK   -> PHANTOM_BLACK
        AssistantTheme.NERF_ORANGE     -> NERF_ORANGE
        AssistantTheme.BLUEPRINT_STONE -> BLUEPRINT_STONE
        AssistantTheme.CYBER_GRAFFITI  -> CYBER_GRAFFITI
    }
}

class AssistantViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    val themeConfig: AssistantThemeConfig
        get() = AssistantThemeRegistry.forTheme(_uiState.value.activeTheme)

    fun onEvent(event: AssistantEvent) {
        when (event) {
            is AssistantEvent.ThemeSelected -> {
                _uiState.update { it.copy(activeTheme = event.theme) }
            }
            is AssistantEvent.MicTapped -> {
                val next = when (_uiState.value.assistantState) {
                    AssistantState.IDLE      -> AssistantState.LISTENING
                    AssistantState.LISTENING -> AssistantState.IDLE
                    else                     -> AssistantState.IDLE
                }
                _uiState.update { it.copy(assistantState = next) }
            }
            is AssistantEvent.ChestTapped -> {
                _uiState.update { it.copy(chestPanelOpen = !it.chestPanelOpen, handPanelOpen = false) }
            }
            is AssistantEvent.HandTapped -> {
                _uiState.update { it.copy(handPanelOpen = !it.handPanelOpen, chestPanelOpen = false) }
            }
            is AssistantEvent.SendMessage -> {
                val text = _uiState.value.inputText.trim()
                if (text.isBlank()) return
                val userMsg = ChatMessage(UUID.randomUUID().toString(), false, "[USER] $text")
                val aiReply = ChatMessage(UUID.randomUUID().toString(), true, "[AI] Processing: $text")
                _uiState.update { st ->
                    st.copy(
                        chatMessages = st.chatMessages + userMsg + aiReply,
                        inputText    = "",
                        assistantState = AssistantState.THINKING
                    )
                }
            }
            is AssistantEvent.InputChanged -> {
                _uiState.update { it.copy(inputText = event.text) }
            }
            is AssistantEvent.FireModeChanged -> {
                _uiState.update { it.copy(fireMode = event.mode) }
            }
            is AssistantEvent.DismissOverlays -> {
                _uiState.update { it.copy(chestPanelOpen = false, handPanelOpen = false) }
            }
        }
    }
}

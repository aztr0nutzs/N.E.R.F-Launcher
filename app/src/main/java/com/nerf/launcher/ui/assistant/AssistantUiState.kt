package com.nerf.launcher.state

// ─────────────────────────────────────────────────────────────────────────────
// Assistant Screen State, Themes, Events
// ─────────────────────────────────────────────────────────────────────────────

enum class AssistantTheme {
    PHANTOM_BLACK,   // Image 1: dark holographic neon
    NERF_ORANGE,     // Image 2: orange / navy NERF colorway
    BLUEPRINT_STONE, // Image 3: orange / blueprint background
    CYBER_GRAFFITI   // Image 4: dark graffiti / mic-centric
}

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

data class ChatMessage(
    val id: String,
    val isAI: Boolean,
    val text: String
)

/**
 * Fractional overlay bounds: values are fractions of the screen width/height.
 * topLeft = Offset(x fraction, y fraction), size = Size(w fraction, h fraction).
 * Applied as:  absoluteX = fraction * screenWidth
 */
data class OverlayBounds(
    val xFraction: Float,
    val yFraction: Float,
    val wFraction: Float,
    val hFraction: Float
)

/**
 * Per-theme color palette and layout anchors.
 * Bounds are tuned to match the exact pane/control positions in each artwork.
 */
data class AssistantThemeConfig(
    val theme: AssistantTheme,
    val drawableResId: Int,                  // R.drawable.asst_screen_X
    val accentPrimary: androidx.compose.ui.graphics.Color,
    val accentSecondary: androidx.compose.ui.graphics.Color,
    val accentGlow: androidx.compose.ui.graphics.Color,
    val chatPaneBounds: OverlayBounds,        // transcript text area
    val dockBounds: OverlayBounds,            // bottom icon row
    val micBounds: OverlayBounds,             // mic / central action button
    val chestBounds: OverlayBounds,           // chest/core reactor tap zone
    val handBounds: OverlayBounds,            // left hand projection zone
    val dartStripBounds: OverlayBounds?,      // dart count + fire-mode strip (null if absent)
    val chatTextYStart: Float,                // y fraction where chat text begins inside pane
    val chatTextXPad: Float,                  // x fraction padding inside pane for text
    val showDartStrip: Boolean,
    val showModeToggle: Boolean,
    val progressDotsVisible: Boolean,         // theme 4 only
    val visorGlowColor: androidx.compose.ui.graphics.Color,
    val chestGlowColor: androidx.compose.ui.graphics.Color
)

data class AssistantUiState(
    val activeTheme: AssistantTheme = AssistantTheme.PHANTOM_BLACK,
    val assistantState: AssistantState = AssistantState.IDLE,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage("1", true,  "[AI] V-Core 5 online. Ready, Commander."),
        ChatMessage("2", false, "[USER] Run a deep diagnostic on my primary weapon system"),
        ChatMessage("3", true,  "[AI] Initializing deep diagnostic on M.K. POWER systems..."),
        ChatMessage("4", false, "[USER] What's the status of the DIMENSIONAL RESONANCE?"),
        ChatMessage("5", true,  "[AI] Dim. Resonance stable at 98%. Scanning VOID.")
    ),
    val dartCount: Int = 12,
    val fireMode: FireMode = FireMode.SEMI_AUTO,
    val chestPanelOpen: Boolean = false,
    val handPanelOpen: Boolean = false,
    val inputText: String = ""
)

enum class FireMode { SEMI_AUTO, FULL_AUTO }

sealed class AssistantEvent {
    data class ThemeSelected(val theme: AssistantTheme) : AssistantEvent()
    object MicTapped : AssistantEvent()
    object ChestTapped : AssistantEvent()
    object HandTapped : AssistantEvent()
    object SendMessage : AssistantEvent()
    data class InputChanged(val text: String) : AssistantEvent()
    data class FireModeChanged(val mode: FireMode) : AssistantEvent()
    object DismissOverlays : AssistantEvent()
}

package com.nerf.launcher.ui.assistant

import com.nerf.launcher.util.assistant.AssistantController
import com.nerf.launcher.util.assistant.AssistantState
import com.nerf.launcher.util.assistant.PersonalityMood

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantUiState
//
//  Immutable snapshot of everything the Compose assistant screen needs to
//  render a single frame. Produced by AssistantViewModel and consumed by
//  the AssistantScreen composable hierarchy.
// ─────────────────────────────────────────────────────────────────────────────

data class AssistantUiState(
    /** Active robot state (idle, listening, thinking, speaking, …). */
    val robotState: AssistantState = AssistantState.IDLE,

    /** Currently active assistant theme configuration. */
    val activeTheme: AssistantThemeConfig = AssistantThemeRegistry.defaultTheme,

    /** Current personality mood. */
    val mood: PersonalityMood = PersonalityMood.SNARKY,

    /** Latest assistant text response (rendered in the response area). */
    val latestResponse: String = "",

    /** Full chat transcript (newest first). */
    val transcript: List<TranscriptMessage> = emptyList(),

    /** Whether the chat pane overlay is expanded. */
    val isChatPaneOpen: Boolean = true,

    /** Whether the chest/core overlay is active. */
    val isChestCoreActive: Boolean = false,

    /** Whether the hand projection overlay is showing. */
    val isHandProjectionActive: Boolean = false,

    /** Whether a side telemetry panel (left) is open. */
    val isSidePanelLeftOpen: Boolean = false,

    /** Whether a side telemetry panel (right) is open. */
    val isSidePanelRightOpen: Boolean = false,

    /** Whether voice/mic input is available on this device. */
    val isVoiceAvailable: Boolean = false,

    /** Whether we're actively listening for voice input. */
    val isListening: Boolean = false,

    /** Response bank loaded status text. */
    val bankStatusLabel: String = "RESPONSE BANK LOADING",

    /** Total interaction count for telemetry display. */
    val interactionCount: Int = 0,

    /** Whether the assistant screen is visible. */
    val isVisible: Boolean = false,

    /** Current text in the input field (for controlled input). */
    val inputText: String = ""
)

/**
 * Single message in the chat transcript.
 */
data class TranscriptMessage(
    val speaker: Speaker,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
) {
    enum class Speaker {
        USER,
        ASSISTANT;

        companion object {
            fun from(controllerSpeaker: AssistantController.Speaker): Speaker = when (controllerSpeaker) {
                AssistantController.Speaker.USER -> USER
                AssistantController.Speaker.ASSISTANT -> ASSISTANT
            }
        }
    }
}

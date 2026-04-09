package com.nerf.launcher.ui.assistant

import com.nerf.launcher.util.assistant.AssistantAction
import com.nerf.launcher.util.assistant.PersonalityMood

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantEvent
//
//  One-shot UI events produced by interactions within the assistant screen.
//  The ViewModel consumes these and delegates to the appropriate controller.
// ─────────────────────────────────────────────────────────────────────────────

sealed interface AssistantEvent {

    // ── Text / Voice Input ───────────────────────────────────────────────────
    /** User submitted text from the input field. */
    data class SubmitText(val text: String) : AssistantEvent

    /** User tapped the microphone button. */
    data object MicTapped : AssistantEvent

    /** User tapped "Repeat Last". */
    data object RepeatLast : AssistantEvent

    /** User tapped "Stop Voice" / interrupt. */
    data object InterruptSpeaking : AssistantEvent

    // ── Hotspot Interactions ─────────────────────────────────────────────────
    /** User tapped the chest/core zone. */
    data object ChestCoreTapped : AssistantEvent

    /** User tapped the hand projection zone. */
    data object HandProjectionTapped : AssistantEvent

    /** User tapped a side telemetry module. */
    data class SideModuleTapped(val side: Side) : AssistantEvent {
        enum class Side { LEFT, RIGHT }
    }

    // ── Quick Actions ────────────────────────────────────────────────────────
    /** User tapped a quick action from the dock. */
    data class LauncherCommandTapped(
        val command: AssistantAction.LauncherCommand
    ) : AssistantEvent

    // ── Theme ────────────────────────────────────────────────────────────────
    /** User requested theme switch. */
    data class SwitchTheme(val themeId: AssistantThemeId) : AssistantEvent

    /** User requested to cycle to the next theme. */
    data object CycleTheme : AssistantEvent

    // ── Mood ─────────────────────────────────────────────────────────────────
    /** User tapped the mood indicator to cycle mood. */
    data object CycleMood : AssistantEvent

    // ── Overlay State ────────────────────────────────────────────────────────
    /** User requested to dismiss the assistant screen. */
    data object Dismiss : AssistantEvent

    /** User toggled the chat pane open/closed. */
    data object ToggleChatPane : AssistantEvent

    /** User toggled the side telemetry panel. */
    data class ToggleSidePanel(val side: SideModuleTapped.Side) : AssistantEvent
}

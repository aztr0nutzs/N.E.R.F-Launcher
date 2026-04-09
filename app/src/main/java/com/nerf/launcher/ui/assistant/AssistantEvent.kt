package com.nerf.launcher.ui.assistant

import com.nerf.launcher.util.assistant.AssistantAction

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

    /** User tapped the microphone button (input row or dock mic). */
    data object MicTapped : AssistantEvent

    /** User tapped "Repeat Last". */
    data object RepeatLast : AssistantEvent

    /** User tapped "Stop Voice" / interrupt. */
    data object InterruptSpeaking : AssistantEvent

    // ── Input Focus ──────────────────────────────────────────────────────────
    data object InputFocused  : AssistantEvent
    data object InputUnfocused : AssistantEvent

    // ── Reactor Interactions ─────────────────────────────────────────────────
    /** User tapped the reactor core (inner tap zone). */
    data object ReactorCoreTapped : AssistantEvent

    /** User tapped a reactor ring sector. */
    data class ReactorSectorTapped(val sector: ReactorSector) : AssistantEvent

    // ── Robot Body Hotspots ──────────────────────────────────────────────────
    /** User tapped the chest/core zone (legacy — maps to ReactorCoreTapped). */
    data object ChestCoreTapped : AssistantEvent

    /** User tapped the hand projection zone. */
    data object HandProjectionTapped : AssistantEvent

    /** User tapped a side telemetry module. */
    data class SideModuleTapped(val side: Side) : AssistantEvent {
        enum class Side { LEFT, RIGHT }
    }

    // ── Left Action Stack ────────────────────────────────────────────────────
    data class LeftActionTapped(val action: LeftAction) : AssistantEvent

    // ── Bottom Dock ──────────────────────────────────────────────────────────
    data class DockActionTapped(val action: DockAction) : AssistantEvent
    data object DockCenterTapped : AssistantEvent

    // ── Toggle Module ────────────────────────────────────────────────────────
    data object ToggleModuleTapped : AssistantEvent

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

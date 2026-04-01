package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

sealed class AssistantAction {
    enum class LauncherCommand {
        OPEN_SETTINGS,
        OPEN_DIAGNOSTICS,
        OPEN_NODE_HUNTER,
        SHOW_LOCK_SURFACE,
        CYCLE_THEME,
        REPORT_CURRENT_THEME,
        REPORT_SYSTEM_STATE,
        REPORT_APP_FILTER_STATE,
        START_LOCAL_NETWORK_SCAN,
        SUMMARIZE_LOCAL_NETWORK_SCAN
    }

    data class SpeakCategory(
        val category: Category,
        val tags: Set<String> = emptySet()
    ) : AssistantAction()

    data class ExecuteLauncherCommand(
        val command: LauncherCommand,
        val tags: Set<String> = emptySet()
    ) : AssistantAction()

    data object RepeatLastResponse : AssistantAction()

    data object Ignore : AssistantAction()
}

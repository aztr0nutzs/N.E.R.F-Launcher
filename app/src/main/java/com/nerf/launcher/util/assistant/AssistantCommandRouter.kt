package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

class AssistantCommandRouter {

    fun route(
        intent: AssistantIntent,
        context: AssistantContextSnapshot,
        isButtonSpam: Boolean
    ): AssistantAction {
        if (isButtonSpam) {
            return AssistantAction.SpeakCategory(Category.BUTTON_SPAM)
        }

        val launcherCommand = when (intent.command) {
            AssistantIntent.Command.OPEN_SETTINGS -> AssistantAction.LauncherCommand.OPEN_SETTINGS
            AssistantIntent.Command.OPEN_DIAGNOSTICS -> AssistantAction.LauncherCommand.OPEN_DIAGNOSTICS
            AssistantIntent.Command.OPEN_NODE_HUNTER -> AssistantAction.LauncherCommand.OPEN_NODE_HUNTER
            AssistantIntent.Command.SHOW_LOCK_SURFACE -> AssistantAction.LauncherCommand.SHOW_LOCK_SURFACE
            AssistantIntent.Command.THEME_SWITCH -> AssistantAction.LauncherCommand.CYCLE_THEME
            AssistantIntent.Command.REPORT_CURRENT_THEME -> AssistantAction.LauncherCommand.REPORT_CURRENT_THEME
            AssistantIntent.Command.REPORT_SYSTEM_STATE -> AssistantAction.LauncherCommand.REPORT_SYSTEM_STATE
            AssistantIntent.Command.REPORT_APP_FILTER_STATE -> AssistantAction.LauncherCommand.REPORT_APP_FILTER_STATE
            AssistantIntent.Command.START_LOCAL_NETWORK_SCAN -> AssistantAction.LauncherCommand.START_LOCAL_NETWORK_SCAN
            AssistantIntent.Command.SUMMARIZE_LOCAL_NETWORK_SCAN -> AssistantAction.LauncherCommand.SUMMARIZE_LOCAL_NETWORK_SCAN
            else -> null
        }
        if (launcherCommand != null) {
            return AssistantAction.ExecuteLauncherCommand(
                command = launcherCommand,
                tags = intent.tags
            )
        }

        val category = intent.command.category ?: Category.UNKNOWN_COMMAND
        if (category == Category.UNKNOWN_COMMAND && context.lastResponse.isNullOrBlank()) {
            return AssistantAction.SpeakCategory(Category.UNKNOWN_COMMAND)
        }

        return AssistantAction.SpeakCategory(
            category = category,
            tags = intent.tags
        )
    }
}

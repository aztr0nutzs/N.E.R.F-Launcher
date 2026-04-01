package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

sealed class AssistantActionResult {
    data class CategoryResolved(
        val category: Category,
        val tags: Set<String> = emptySet()
    ) : AssistantActionResult()

    data class LauncherCommandHandled(
        val command: AssistantAction.LauncherCommand,
        val spokenText: String,
        val performed: Boolean
    ) : AssistantActionResult()

    data object RepeatLast : AssistantActionResult()

    data object Ignored : AssistantActionResult()
}

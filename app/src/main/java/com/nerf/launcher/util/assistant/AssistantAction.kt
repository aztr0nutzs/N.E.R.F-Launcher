package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

sealed class AssistantAction {
    data class SpeakCategory(
        val category: Category,
        val tags: Set<String> = emptySet()
    ) : AssistantAction()

    data object RepeatLastResponse : AssistantAction()

    data object Ignore : AssistantAction()
}

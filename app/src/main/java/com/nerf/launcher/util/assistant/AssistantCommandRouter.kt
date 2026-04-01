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

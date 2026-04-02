package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.ResponseRequest

class AssistantResponseComposer {

    fun compose(
        result: AssistantActionResult,
        context: AssistantContextSnapshot
    ): ResponsePlan = when (result) {
        is AssistantActionResult.CategoryResolved -> ResponsePlan.CategoryRequest(
            request = ResponseRequest(
                category = result.category,
                mood = context.mood,
                tags = result.tags
            )
        )

        is AssistantActionResult.LauncherCommandHandled -> ResponsePlan.DirectText(result.resolvedSpokenText())
        AssistantActionResult.RepeatLast -> ResponsePlan.RepeatLast
        AssistantActionResult.Ignored -> ResponsePlan.NoOp
    }

    sealed class ResponsePlan {
        data class CategoryRequest(val request: ResponseRequest) : ResponsePlan()
        data class DirectText(val text: String) : ResponsePlan()
        data object RepeatLast : ResponsePlan()
        data object NoOp : ResponsePlan()
    }
}

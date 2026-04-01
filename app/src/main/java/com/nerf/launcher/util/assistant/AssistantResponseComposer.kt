package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.ResponseRequest

class AssistantResponseComposer {

    fun compose(
        action: AssistantAction,
        context: AssistantContextSnapshot
    ): ResponsePlan = when (action) {
        is AssistantAction.SpeakCategory -> ResponsePlan.CategoryRequest(
            request = ResponseRequest(
                category = action.category,
                mood = context.mood,
                tags = action.tags
            )
        )

        AssistantAction.RepeatLastResponse -> ResponsePlan.RepeatLast
        AssistantAction.Ignore -> ResponsePlan.NoOp
    }

    sealed class ResponsePlan {
        data class CategoryRequest(val request: ResponseRequest) : ResponsePlan()
        data object RepeatLast : ResponsePlan()
        data object NoOp : ResponsePlan()
    }
}

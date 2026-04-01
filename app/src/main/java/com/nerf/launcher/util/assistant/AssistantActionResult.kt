package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

sealed class AssistantActionResult {
    data class Spoken(
        val text: String,
        val category: Category
    ) : AssistantActionResult()

    data class Repeated(val text: String) : AssistantActionResult()

    data object Ignored : AssistantActionResult()
}

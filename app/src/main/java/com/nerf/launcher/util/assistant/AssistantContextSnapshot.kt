package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

data class AssistantContextSnapshot(
    val mood: PersonalityMood,
    val interactionCount: Int,
    val state: AssistantState,
    val lastCategory: Category?,
    val lastResponse: String?,
    val timestampMs: Long,
    val isSpeaking: Boolean
)

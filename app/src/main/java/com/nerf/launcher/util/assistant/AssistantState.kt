package com.nerf.launcher.util.assistant

enum class AssistantState {
    IDLE,
    WAKE,
    LISTENING,
    THINKING,
    RESPONDING,
    SPEAKING,
    MUTED,
    ERROR
}

data class AssistantStateSnapshot(
    val state: AssistantState,
    val response: String? = null
)

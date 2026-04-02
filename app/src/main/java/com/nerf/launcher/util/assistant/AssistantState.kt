package com.nerf.launcher.util.assistant

enum class AssistantState {
    IDLE,
    WAKE,
    LISTENING,
    THINKING,
    RESPONDING,
    SPEAKING,
    MUTED,
    ERROR,
    REBOOTING,
    SHUTTING_DOWN;

    val isActive: Boolean
        get() = this != IDLE && this != MUTED && this != SHUTTING_DOWN

    val isBusy: Boolean
        get() = this == THINKING || this == RESPONDING || this == SPEAKING

    val canInterrupt: Boolean
        get() = this != REBOOTING && this != SHUTTING_DOWN

    val isSpeaking: Boolean
        get() = this == SPEAKING
}

enum class PersonalityMood(val label: String, val description: String) {
    SNARKY("Snarky", "Dry wit, sarcasm, deadpan delivery"),
    TACTICAL("Tactical", "Crisp, authoritative, mission-ready"),
    BORED("Bored", "Flat affect, slow delivery, existential malaise"),
    ALERT("Alert", "Heightened urgency, rapid-fire, intense"),
    SERIOUS("Serious", "Focused, measured, mission-critical tone"),
    PLAYFUL("Playful", "Lighter sarcasm, quicker wit, more showmanship"),
    SAVAGE("Savage", "Sharper sarcasm, heavier roast density, less mercy")
}

data class AssistantStateSnapshot(
    val state: AssistantState,
    val response: String? = null,
    val mood: PersonalityMood = PersonalityMood.SNARKY,
    val context: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isPriority: Boolean = false,
    val interactionCount: Int = 0
)

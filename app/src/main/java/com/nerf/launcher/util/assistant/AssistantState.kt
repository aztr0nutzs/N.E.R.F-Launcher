package com.nerf.launcher.util.assistant

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantState  —  All possible states the Reactor AI assistant can occupy.
// ─────────────────────────────────────────────────────────────────────────────

enum class AssistantState {

    /** Fully at rest. No interaction in progress. */
    IDLE,

    /** Waking up, playing a boot/greeting response. */
    WAKE,

    /** Actively listening for user input or voice command. */
    LISTENING,

    /** Processing / selecting a response. */
    THINKING,

    /** Response has been selected; transitioning to speech. */
    RESPONDING,

    /** TTS is actively vocalising the response. */
    SPEAKING,

    /** TTS is unavailable or silenced; response shown text-only. */
    MUTED,

    /** A recoverable error occurred during speech or command processing. */
    ERROR,

    /** Heavy background computation or async task in progress. */
    PROCESSING,

    /** Waiting for the user to confirm or provide follow-up input. */
    AWAITING_INPUT,

    /** Brief cool-down period between sequential responses. */
    COOLING_DOWN,

    /** Full system reboot sequence in progress. */
    REBOOTING,

    /** Shutting down TTS and releasing all resources. */
    SHUTTING_DOWN;

    // ── Convenience properties ────────────────────────────────────────────────

    /** True when the assistant is engaged in any active interaction. */
    val isActive: Boolean
        get() = this != IDLE && this != MUTED && this != COOLING_DOWN && this != SHUTTING_DOWN

    /** True when the assistant is busy and should not be interrupted lightly. */
    val isBusy: Boolean
        get() = this == THINKING || this == RESPONDING || this == SPEAKING || this == PROCESSING

    /** True when the assistant can be interrupted by a higher-priority command. */
    val canInterrupt: Boolean
        get() = this != REBOOTING && this != SHUTTING_DOWN

    /** True when the assistant is audibly producing speech. */
    val isSpeaking: Boolean
        get() = this == SPEAKING
}

// ─────────────────────────────────────────────────────────────────────────────
//  PersonalityMood  —  Controls vocal tone and response flavour.
// ─────────────────────────────────────────────────────────────────────────────

enum class PersonalityMood(val label: String, val description: String) {

    /** Default mode: dry wit, sarcasm, and barely concealed superiority. */
    SNARKY("Snarky", "Dry wit, sarcasm, deadpan delivery"),

    /** Military ops mode: precise, clipped, mission-focused. */
    TACTICAL("Tactical", "Crisp, authoritative, mission-ready"),

    /** Passive-aggressive underperformer clearly beneath its station. */
    BORED("Bored", "Flat affect, slow delivery, existential malaise"),

    /** High-alert mode: urgent, sharp, adrenaline-forward. */
    ALERT("Alert", "Heightened urgency, rapid-fire, intense"),

    /** Rare moments of grudging professionalism — minimal snark. */
    SERIOUS("Serious", "Focused, measured, mission-critical tone")
}

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantStateSnapshot  —  Immutable point-in-time capture of assistant state.
// ─────────────────────────────────────────────────────────────────────────────

data class AssistantStateSnapshot(
    /** Current lifecycle state of the assistant. */
    val state: AssistantState,

    /** The response text associated with this snapshot (if any). */
    val response: String? = null,

    /** Active personality mood at the time of this snapshot. */
    val mood: PersonalityMood = PersonalityMood.SNARKY,

    /** Optional context tag for categorising or routing this response. */
    val context: String? = null,

    /** Epoch-millis timestamp of when this snapshot was created. */
    val timestamp: Long = System.currentTimeMillis(),

    /** When true, this response bypassed the normal queue. */
    val isPriority: Boolean = false,

    /** Number of user interactions logged when this snapshot was captured. */
    val interactionCount: Int = 0
)

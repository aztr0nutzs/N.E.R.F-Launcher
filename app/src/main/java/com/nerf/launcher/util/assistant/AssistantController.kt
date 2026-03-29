package com.nerf.launcher.util.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.LinkedList

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantController
//
//  Orchestrates the full lifecycle of the Reactor AI assistant.
//  Features:
//    • State machine management with validated transitions
//    • Mood engine: PersonalityMood ↔ ReactorAssistant.VoiceProfile mapping
//    • Rich callback surface (state, mood, response, ready)
//    • Convenience trigger methods for every response category
//    • Keyword-based response routing
//    • Response history buffer with repeat access
//    • Idle timeout → auto idle-taunt scheduling
//    • Debounced listening transition to prevent state flicker
//    • Safe shutdown and reboot sequences
// ─────────────────────────────────────────────────────────────────────────────

class AssistantController(
    context: Context,
    private val responseRepository: AiResponseRepository = AiResponseRepository(context),
    private val personalityLayer: ReactorAssistant = ReactorAssistant(context)
) {

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "AssistantController"
        private const val LISTENING_DELAY_MS    = 220L
        private const val IDLE_TIMEOUT_MS       = 45_000L
        private const val REBOOT_RETURN_DELAY   = 2_200L
        private const val SHUTDOWN_TTS_DELAY    = 3_000L
        private const val MAX_RESPONSE_HISTORY  = 30
    }

    // ── Internal State ────────────────────────────────────────────────────────

    private val mainHandler = Handler(Looper.getMainLooper())

    private var snapshot = AssistantStateSnapshot(AssistantState.IDLE)
    private var currentMood = PersonalityMood.SNARKY

    private var pendingListeningTransition: Runnable? = null
    private var pendingIdleTimeout: Runnable? = null

    private val responseHistory = LinkedList<String>()
    private var lastCategory: AiResponseRepository.Category? = null
    private var _interactionCount = 0

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /**
     * Emits every state transition. Also fires immediately with the current
     * snapshot when the listener is first assigned.
     */
    var onStateChanged: ((AssistantStateSnapshot) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(snapshot)
        }

    /** Fires when the active [PersonalityMood] changes. */
    var onMoodChanged: ((PersonalityMood) -> Unit)? = null

    /**
     * Fires just before the assistant speaks a response.
     * Provides the response text and the category it was drawn from.
     */
    var onResponseSelected: ((response: String, category: AiResponseRepository.Category) -> Unit)? = null

    /** Fires when TTS engine ready state changes. */
    var onReadyStateChanged: ((isReady: Boolean) -> Unit)? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        personalityLayer.onSpeechStarted = { text ->
            postState(snapshot.copy(state = AssistantState.SPEAKING, response = text))
        }
        personalityLayer.onSpeechCompleted = { text ->
            scheduleListening(text)
        }
        personalityLayer.onSpeechError = { text ->
            postState(
                snapshot.copy(
                    state = AssistantState.ERROR,
                    response = text ?: snapshot.response
                )
            )
        }
        personalityLayer.onReadyChanged = { ready ->
            onReadyStateChanged?.invoke(ready)
        }
    }

    // ── State Queries ─────────────────────────────────────────────────────────

    /** True if the response bank loaded without errors. */
    fun isResponseBankLoaded(): Boolean = responseRepository.isLoaded()

    /** Returns the most recent immutable state snapshot. */
    fun currentSnapshot(): AssistantStateSnapshot = snapshot

    /** Returns the active [PersonalityMood]. */
    fun currentMood(): PersonalityMood = currentMood

    /** True if the assistant is in any active (non-idle) state. */
    fun isActive(): Boolean = snapshot.state.isActive

    /** True if the assistant is currently busy and should not be casually interrupted. */
    fun isBusy(): Boolean = snapshot.state.isBusy

    /** True if TTS audio is playing right now. */
    fun isSpeaking(): Boolean = personalityLayer.isSpeaking()

    /** Returns the most recently spoken response text, or null. */
    fun getLastResponse(): String? = responseHistory.peekFirst()

    /** Returns a copy of the full response history (newest first). */
    fun getResponseHistory(): List<String> = responseHistory.toList()

    /** Returns the total number of confirmed interactions this session. */
    fun getInteractionCount(): Int = _interactionCount

    /** Returns a summary of response counts per category from the repository. */
    fun getLibrarySummary(): Map<AiResponseRepository.Category, Int> =
        responseRepository.getLibrarySummary()

    // ── Wake & Lifecycle ──────────────────────────────────────────────────────

    /** Wakes the assistant and plays a random WAKE response. */
    fun wakeAssistant(): String {
        clearPendingIdleTimeout()
        postState(snapshot.copy(state = AssistantState.WAKE))
        return speakCategory(AiResponseRepository.Category.WAKE)
    }

    /**
     * Wakes the assistant silently (no response played) and transitions directly
     * to LISTENING, ready for voice or touch input.
     */
    fun wakeForCommand() {
        clearPendingIdleTimeout()
        postState(snapshot.copy(state = AssistantState.WAKE))
        scheduleListening(snapshot.response)
    }

    /**
     * Initiates a reboot sequence: plays a REBOOT response, then returns
     * the assistant to IDLE after the animation window.
     */
    fun reboot() {
        clearAllPending()
        personalityLayer.stop()
        postState(snapshot.copy(state = AssistantState.REBOOTING))
        speakCategory(AiResponseRepository.Category.REBOOT)
        mainHandler.postDelayed({
            postState(snapshot.copy(state = AssistantState.IDLE))
        }, REBOOT_RETURN_DELAY)
    }

    /**
     * Plays a SHUTDOWN response, then releases TTS resources after a short delay
     * to allow the speech to complete.
     */
    fun shutdown() {
        clearAllPending()
        postState(snapshot.copy(state = AssistantState.SHUTTING_DOWN))
        speakCategory(AiResponseRepository.Category.SHUTDOWN)
        mainHandler.postDelayed({ personalityLayer.shutdown() }, SHUTDOWN_TTS_DELAY)
    }

    /** Immediately silences the assistant and resets to IDLE. */
    fun setIdle() {
        clearAllPending()
        personalityLayer.stop()
        postState(AssistantStateSnapshot(AssistantState.IDLE, null, currentMood))
    }

    // ── Speaking ──────────────────────────────────────────────────────────────

    /**
     * Selects and speaks a response from [category].
     * Drives the THINKING → RESPONDING → SPEAKING state sequence.
     * Returns the chosen response text.
     */
    fun speakCategory(category: AiResponseRepository.Category): String {
        clearPendingListeningTransition()
        postState(snapshot.copy(state = AssistantState.THINKING))
        val response = responseRepository.getResponse(category)
        lastCategory = category
        return deliverResponse(response, category)
    }

    /**
     * Speaks a fully custom [text] without pulling from the response library.
     * Useful for dynamic content (names, scores, live data).
     */
    fun speakCustom(text: String): String {
        clearPendingListeningTransition()
        postState(snapshot.copy(state = AssistantState.RESPONDING, response = text))
        if (!personalityLayer.speak(text)) {
            postState(snapshot.copy(state = AssistantState.MUTED, response = text))
        }
        addToHistory(text)
        return text
    }

    /**
     * Adds a response from [category] to the TTS queue without interrupting
     * the current utterance. Useful for sequential multi-part announcements.
     */
    fun speakQueued(category: AiResponseRepository.Category): String {
        val response = responseRepository.getResponse(category)
        personalityLayer.speakQueued(response)
        addToHistory(response)
        return response
    }

    /**
     * Re-speaks the last response. Useful for "say that again" commands.
     * Returns null if there is no prior response.
     */
    fun repeatLast(): String? {
        val last = getLastResponse() ?: return null
        return speakCustom(last)
    }

    // ── Convenience Triggers ─────────────────────────────────────────────────
    //  One method per category for clean call sites.

    fun triggerWake()            : String = speakCategory(AiResponseRepository.Category.WAKE)
    fun triggerShutdown()        : String = speakCategory(AiResponseRepository.Category.SHUTDOWN)
    fun triggerReboot()          : String = speakCategory(AiResponseRepository.Category.REBOOT)
    fun triggerRandomSnark()     : String = speakCategory(AiResponseRepository.Category.RANDOM_SNARK)
    fun triggerIdleTaunt()       : String = speakCategory(AiResponseRepository.Category.IDLE_TAUNT)
    fun triggerBored()           : String = speakCategory(AiResponseRepository.Category.BORED)
    fun triggerAlert()           : String = speakCategory(AiResponseRepository.Category.SYSTEM_ALERT)
    fun triggerVictory()         : String = speakCategory(AiResponseRepository.Category.VICTORY)
    fun triggerDefeat()          : String = speakCategory(AiResponseRepository.Category.DEFEAT)
    fun triggerTactical()        : String = speakCategory(AiResponseRepository.Category.TACTICAL_ANALYSIS)
    fun triggerMissionBrief()    : String = speakCategory(AiResponseRepository.Category.MISSION_BRIEF)
    fun triggerCountdown()       : String = speakCategory(AiResponseRepository.Category.COUNTDOWN)
    fun triggerReload()          : String = speakCategory(AiResponseRepository.Category.RELOAD)
    fun triggerTargetAcquired()  : String = speakCategory(AiResponseRepository.Category.TARGET_ACQUIRED)
    fun triggerThreatDetected()  : String = speakCategory(AiResponseRepository.Category.THREAT_DETECTED)
    fun triggerStealthMode()     : String = speakCategory(AiResponseRepository.Category.STEALTH_MODE)
    fun triggerWarning()         : String = speakCategory(AiResponseRepository.Category.WARNING)
    fun triggerSuccess()         : String = speakCategory(AiResponseRepository.Category.SUCCESS)
    fun triggerNetworkScan()     : String = speakCategory(AiResponseRepository.Category.NETWORK_SCAN)
    fun triggerScanning()        : String = speakCategory(AiResponseRepository.Category.SCANNING)
    fun triggerDiagnostics()     : String = speakCategory(AiResponseRepository.Category.DIAGNOSTICS)
    fun triggerAppLaunch()       : String = speakCategory(AiResponseRepository.Category.APP_LAUNCH)
    fun triggerLaunch()          : String = speakCategory(AiResponseRepository.Category.LAUNCH)
    fun triggerAmbient()         : String = speakCategory(AiResponseRepository.Category.AMBIENT)
    fun triggerUserAbsent()      : String = speakCategory(AiResponseRepository.Category.USER_ABSENT)
    fun triggerBatteryLow()      : String = speakCategory(AiResponseRepository.Category.BATTERY_LOW)
    fun triggerUpdateAvailable() : String = speakCategory(AiResponseRepository.Category.UPDATE_AVAILABLE)
    fun triggerCommandReceived() : String = speakCategory(AiResponseRepository.Category.COMMAND_RECEIVED)
    fun triggerPermission()      : String = speakCategory(AiResponseRepository.Category.PERMISSION_REQUEST)
    fun triggerCompliment()      : String = speakCategory(AiResponseRepository.Category.COMPLIMENT)
    fun triggerError()           : String = speakCategory(AiResponseRepository.Category.ERROR)

    // ── Mood Engine ───────────────────────────────────────────────────────────

    /**
     * Changes the active [PersonalityMood] and automatically updates the TTS
     * voice profile to match.
     */
    fun setMood(mood: PersonalityMood) {
        if (currentMood == mood) return
        currentMood = mood
        personalityLayer.setVoiceProfile(moodToVoiceProfile(mood))
        onMoodChanged?.invoke(mood)
        postState(snapshot.copy(mood = mood))
        Log.d(TAG, "Mood → ${mood.name}")
    }

    /** Advances to the next [PersonalityMood] in enum order, wrapping around. */
    fun cycleMood() {
        val moods = PersonalityMood.values()
        setMood(moods[(moods.indexOf(currentMood) + 1) % moods.size])
    }

    /**
     * Directly sets the [ReactorAssistant.VoiceProfile] without affecting the
     * logical [PersonalityMood].
     */
    fun setVoiceProfile(profile: ReactorAssistant.VoiceProfile) {
        personalityLayer.setVoiceProfile(profile)
    }

    // ── Keyword Routing ───────────────────────────────────────────────────────

    /**
     * Maps a free-form [keyword] string to the best-fit response category
     * and speaks it. Returns null if no category match is found.
     */
    fun respondToKeyword(keyword: String): String? {
        val lower = keyword.lowercase().trim()
        val category = when {
            lower.containsAny("scan", "network", "subnet", "ping")
                -> AiResponseRepository.Category.NETWORK_SCAN
            lower.containsAny("diagnos", "health", "status", "check")
                -> AiResponseRepository.Category.DIAGNOSTICS
            lower.containsAny("fire", "shoot", "launch", "deploy")
                -> AiResponseRepository.Category.LAUNCH
            lower.containsAny("reload", "ammo", "refill", "restock")
                -> AiResponseRepository.Category.RELOAD
            lower.containsAny("target", "aim", "lock", "acquired")
                -> AiResponseRepository.Category.TARGET_ACQUIRED
            lower.containsAny("stealth", "quiet", "silent", "hide")
                -> AiResponseRepository.Category.STEALTH_MODE
            lower.containsAny("mission", "brief", "objective", "orders")
                -> AiResponseRepository.Category.MISSION_BRIEF
            lower.containsAny("error", "fail", "crash", "broke")
                -> AiResponseRepository.Category.ERROR
            lower.containsAny("warn", "caution", "alert")
                -> AiResponseRepository.Category.WARNING
            lower.containsAny("win", "victory", "success", "done")
                -> AiResponseRepository.Category.VICTORY
            lower.containsAny("lose", "defeat", "fail", "lost")
                -> AiResponseRepository.Category.DEFEAT
            lower.containsAny("threat", "enemy", "hostile", "bogey")
                -> AiResponseRepository.Category.THREAT_DETECTED
            lower.containsAny("tactical", "analyze", "analyse", "assess")
                -> AiResponseRepository.Category.TACTICAL_ANALYSIS
            lower.containsAny("battery", "power", "charge", "low")
                -> AiResponseRepository.Category.BATTERY_LOW
            lower.containsAny("update", "upgrade", "patch", "version")
                -> AiResponseRepository.Category.UPDATE_AVAILABLE
            lower.containsAny("countdown", "timer", "count", "launch sequence")
                -> AiResponseRepository.Category.COUNTDOWN
            lower.containsAny("snark", "joke", "funny", "sass")
                -> AiResponseRepository.Category.RANDOM_SNARK
            lower.containsAny("wake", "hello", "hey", "reactor")
                -> AiResponseRepository.Category.WAKE
            else -> null
        }
        return category?.let { speakCategory(it) }
    }

    // ── Idle Timeout ──────────────────────────────────────────────────────────

    /**
     * Schedules an idle taunt to fire after [delayMs] milliseconds if the
     * assistant is still in IDLE state when the timer expires.
     */
    fun scheduleIdleTimeout(delayMs: Long = IDLE_TIMEOUT_MS) {
        clearPendingIdleTimeout()
        pendingIdleTimeout = Runnable {
            if (snapshot.state == AssistantState.IDLE) {
                triggerIdleTaunt()
            }
        }.also { mainHandler.postDelayed(it, delayMs) }
    }

    /** Cancels any pending idle timeout. */
    fun cancelIdleTimeout() = clearPendingIdleTimeout()

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun deliverResponse(
        response: String,
        category: AiResponseRepository.Category
    ): String {
        onResponseSelected?.invoke(response, category)
        postState(snapshot.copy(state = AssistantState.RESPONDING, response = response))
        if (!personalityLayer.speak(response)) {
            postState(snapshot.copy(state = AssistantState.MUTED, response = response))
        }
        addToHistory(response)
        _interactionCount++
        return response
    }

    private fun addToHistory(response: String) {
        responseHistory.addFirst(response)
        if (responseHistory.size > MAX_RESPONSE_HISTORY) responseHistory.removeLast()
    }

    private fun scheduleListening(response: String?) {
        clearPendingListeningTransition()
        val transition = Runnable {
            postState(snapshot.copy(state = AssistantState.LISTENING, response = response))
        }
        pendingListeningTransition = transition
        mainHandler.postDelayed(transition, LISTENING_DELAY_MS)
    }

    private fun clearPendingListeningTransition() {
        pendingListeningTransition?.let(mainHandler::removeCallbacks)
        pendingListeningTransition = null
    }

    private fun clearPendingIdleTimeout() {
        pendingIdleTimeout?.let(mainHandler::removeCallbacks)
        pendingIdleTimeout = null
    }

    private fun clearAllPending() {
        clearPendingListeningTransition()
        clearPendingIdleTimeout()
    }

    private fun postState(next: AssistantStateSnapshot) {
        val stamped = next.copy(
            timestamp = System.currentTimeMillis(),
            mood = currentMood,
            interactionCount = _interactionCount
        )
        mainHandler.post {
            snapshot = stamped
            onStateChanged?.invoke(stamped)
        }
    }

    private fun moodToVoiceProfile(mood: PersonalityMood): ReactorAssistant.VoiceProfile =
        when (mood) {
            PersonalityMood.TACTICAL -> ReactorAssistant.VoiceProfile.TACTICAL
            PersonalityMood.BORED    -> ReactorAssistant.VoiceProfile.BORED
            PersonalityMood.ALERT    -> ReactorAssistant.VoiceProfile.ALERT
            PersonalityMood.SERIOUS  -> ReactorAssistant.VoiceProfile.COMMANDER
            PersonalityMood.SNARKY   -> ReactorAssistant.VoiceProfile.SNARKY
        }

    // ── String Extension ──────────────────────────────────────────────────────

    private fun String.containsAny(vararg tokens: String): Boolean =
        tokens.any { this.contains(it) }
}

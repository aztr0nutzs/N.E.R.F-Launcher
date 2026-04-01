package com.nerf.launcher.util.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.LinkedList

class AssistantController(
    context: Context,
    private val responseRepository: AiResponseRepository = AiResponseRepository(context),
    private val personalityLayer: ReactorAssistant = ReactorAssistant(context)
) {

    companion object {
        private const val TAG = "AssistantController"
        private const val LISTENING_DELAY_MS = 220L
        private const val IDLE_TIMEOUT_MS = 45_000L
        private const val REBOOT_RETURN_DELAY = 2_200L
        private const val SHUTDOWN_TTS_DELAY = 3_000L
        private const val MAX_RESPONSE_HISTORY = 40
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var snapshot = AssistantStateSnapshot(AssistantState.IDLE)
    private var currentMood = PersonalityMood.SNARKY

    private var pendingListeningTransition: Runnable? = null
    private var pendingIdleTimeout: Runnable? = null

    private val responseHistory = LinkedList<String>()
    private var lastCategory: AiResponseRepository.Category? = null
    private var _interactionCount = 0

    private val intentParser = AssistantIntentParser()
    private val commandRouter = AssistantCommandRouter()
    private val responseComposer = AssistantResponseComposer()
    private val memoryStore = AssistantMemoryStore()

    var onStateChanged: ((AssistantStateSnapshot) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(snapshot)
        }

    var onMoodChanged: ((PersonalityMood) -> Unit)? = null

    var onResponseSelected: ((response: String, category: AiResponseRepository.Category) -> Unit)? = null

    var onReadyStateChanged: ((isReady: Boolean) -> Unit)? = null
    var onLauncherAction: ((AssistantAction.LauncherCommand) -> AssistantActionResult.LauncherCommandHandled)? = null

    init {
        personalityLayer.onSpeechStarted = { text ->
            postState(snapshot.copy(state = AssistantState.SPEAKING, response = text))
        }
        personalityLayer.onSpeechCompleted = { text ->
            scheduleListening(text)
        }
        personalityLayer.onSpeechError = { text ->
            postState(snapshot.copy(state = AssistantState.ERROR, response = text ?: snapshot.response))
        }
        personalityLayer.onReadyChanged = { ready ->
            onReadyStateChanged?.invoke(ready)
        }
    }

    fun isResponseBankLoaded(): Boolean = responseRepository.isLoaded()

    fun currentSnapshot(): AssistantStateSnapshot = snapshot

    fun currentMood(): PersonalityMood = currentMood

    fun isActive(): Boolean = snapshot.state.isActive

    fun isBusy(): Boolean = snapshot.state.isBusy

    fun isSpeaking(): Boolean = personalityLayer.isSpeaking()

    fun getLastResponse(): String? = responseHistory.peekFirst()

    fun getResponseHistory(): List<String> = responseHistory.toList()

    fun getInteractionCount(): Int = _interactionCount

    fun getLibrarySummary(): Map<AiResponseRepository.Category, Int> =
        responseRepository.getLibrarySummary()

    fun searchResponses(query: String, limit: Int = 20): List<Pair<AiResponseRepository.Category, String>> =
        responseRepository.searchResponses(query, limit)

    fun wakeAssistant(): String {
        clearPendingIdleTimeout()
        postState(snapshot.copy(state = AssistantState.WAKE))
        return speakCategory(AiResponseRepository.Category.WAKE)
    }

    fun wakeForCommand() {
        clearPendingIdleTimeout()
        postState(snapshot.copy(state = AssistantState.WAKE))
        scheduleListening(snapshot.response)
    }

    fun reboot() {
        clearAllPending()
        personalityLayer.stop()
        postState(snapshot.copy(state = AssistantState.REBOOTING))
        speakCategory(AiResponseRepository.Category.REBOOT)
        mainHandler.postDelayed({
            postState(snapshot.copy(state = AssistantState.IDLE))
        }, REBOOT_RETURN_DELAY)
    }

    fun shutdown() {
        clearAllPending()
        postState(snapshot.copy(state = AssistantState.SHUTTING_DOWN))
        speakCategory(AiResponseRepository.Category.SHUTDOWN)
        mainHandler.postDelayed({ personalityLayer.shutdown() }, SHUTDOWN_TTS_DELAY)
    }

    fun setIdle() {
        clearAllPending()
        personalityLayer.stop()
        postState(AssistantStateSnapshot(AssistantState.IDLE, null, currentMood))
    }

    fun speakCategory(category: AiResponseRepository.Category): String =
        speakRequest(AiResponseRepository.ResponseRequest(category = category, mood = currentMood))

    fun speakCategory(
        category: AiResponseRepository.Category,
        tags: Set<String>
    ): String = speakRequest(
        AiResponseRepository.ResponseRequest(
            category = category,
            mood = currentMood,
            tags = tags
        )
    )

    fun speakCustom(text: String): String {
        clearPendingListeningTransition()
        postState(snapshot.copy(state = AssistantState.RESPONDING, response = text))
        if (!personalityLayer.speak(text)) {
            postState(snapshot.copy(state = AssistantState.MUTED, response = text))
        }
        addToHistory(text)
        _interactionCount++
        return text
    }

    fun speakQueued(category: AiResponseRepository.Category): String {
        val response = responseRepository.getResponse(
            AiResponseRepository.ResponseRequest(category = category, mood = currentMood)
        )
        personalityLayer.speakQueued(response)
        addToHistory(response)
        return response
    }

    fun speakStatusReport(extraTag: String? = null): String {
        val tags = buildSet {
            if (!extraTag.isNullOrBlank()) add(extraTag.lowercase())
            add(currentMood.name.lowercase())
        }
        return speakCategory(AiResponseRepository.Category.STATUS_REPORT, tags)
    }

    fun repeatLast(): String? {
        val last = getLastResponse() ?: return null
        return speakCustom(last)
    }

    fun triggerWake(): String = speakCategory(AiResponseRepository.Category.WAKE)
    fun triggerShutdown(): String = speakCategory(AiResponseRepository.Category.SHUTDOWN)
    fun triggerReboot(): String = speakCategory(AiResponseRepository.Category.REBOOT)
    fun triggerRandomSnark(): String = speakCategory(AiResponseRepository.Category.RANDOM_SNARK)
    fun triggerIdleTaunt(): String = speakCategory(AiResponseRepository.Category.IDLE_TAUNT)
    fun triggerBored(): String = speakCategory(AiResponseRepository.Category.BORED)
    fun triggerAlert(): String = speakCategory(AiResponseRepository.Category.SYSTEM_ALERT)
    fun triggerVictory(): String = speakCategory(AiResponseRepository.Category.VICTORY)
    fun triggerDefeat(): String = speakCategory(AiResponseRepository.Category.DEFEAT)
    fun triggerTactical(): String = speakCategory(AiResponseRepository.Category.TACTICAL_ANALYSIS)
    fun triggerMissionBrief(): String = speakCategory(AiResponseRepository.Category.MISSION_BRIEF)
    fun triggerCountdown(): String = speakCategory(AiResponseRepository.Category.COUNTDOWN)
    fun triggerReload(): String = speakCategory(AiResponseRepository.Category.RELOAD)
    fun triggerTargetAcquired(): String = speakCategory(AiResponseRepository.Category.TARGET_ACQUIRED)
    fun triggerThreatDetected(): String = speakCategory(AiResponseRepository.Category.THREAT_DETECTED)
    fun triggerStealthMode(): String = speakCategory(AiResponseRepository.Category.STEALTH_MODE)
    fun triggerWarning(): String = speakCategory(AiResponseRepository.Category.WARNING)
    fun triggerSuccess(): String = speakCategory(AiResponseRepository.Category.SUCCESS)
    fun triggerNetworkScan(): String = speakCategory(AiResponseRepository.Category.NETWORK_SCAN)
    fun triggerScanning(): String = speakCategory(AiResponseRepository.Category.SCANNING)
    fun triggerDiagnostics(): String = speakCategory(AiResponseRepository.Category.DIAGNOSTICS)
    fun triggerAppLaunch(): String = speakCategory(AiResponseRepository.Category.APP_LAUNCH)
    fun triggerLaunch(): String = speakCategory(AiResponseRepository.Category.LAUNCH)
    fun triggerAmbient(): String = speakCategory(AiResponseRepository.Category.AMBIENT)
    fun triggerUserAbsent(): String = speakCategory(AiResponseRepository.Category.USER_ABSENT)
    fun triggerBatteryLow(): String = speakCategory(AiResponseRepository.Category.BATTERY_LOW)
    fun triggerUpdateAvailable(): String = speakCategory(AiResponseRepository.Category.UPDATE_AVAILABLE)
    fun triggerCommandReceived(): String = speakCategory(AiResponseRepository.Category.COMMAND_RECEIVED)
    fun triggerPermission(): String = speakCategory(AiResponseRepository.Category.PERMISSION_REQUEST)
    fun triggerCompliment(): String = speakCategory(AiResponseRepository.Category.COMPLIMENT)
    fun triggerError(): String = speakCategory(AiResponseRepository.Category.ERROR)
    fun triggerButtonSpam(): String = speakCategory(AiResponseRepository.Category.BUTTON_SPAM)
    fun triggerRouterControl(): String = speakCategory(AiResponseRepository.Category.ROUTER_CONTROL)
    fun triggerThemeSwitch(): String = speakCategory(AiResponseRepository.Category.THEME_SWITCH)
    fun triggerNetworkSuccess(): String = speakCategory(AiResponseRepository.Category.NETWORK_SUCCESS)
    fun triggerNetworkFailure(): String = speakCategory(AiResponseRepository.Category.NETWORK_FAILURE)
    fun triggerUnknownCommand(): String = speakCategory(AiResponseRepository.Category.UNKNOWN_COMMAND)
    fun triggerStatusReport(): String = speakCategory(AiResponseRepository.Category.STATUS_REPORT)

    fun setMood(mood: PersonalityMood) {
        if (currentMood == mood) return
        currentMood = mood
        personalityLayer.setVoiceProfile(moodToVoiceProfile(mood))
        onMoodChanged?.invoke(mood)
        postState(snapshot.copy(mood = mood))
        Log.d(TAG, "Mood -> ${mood.name}")
    }

    fun cycleMood() {
        val moods = PersonalityMood.values()
        setMood(moods[(moods.indexOf(currentMood) + 1) % moods.size])
    }

    fun setVoiceProfile(profile: ReactorAssistant.VoiceProfile) {
        personalityLayer.setVoiceProfile(profile)
    }

    fun respondToKeyword(keyword: String): String? {
        return respondToInput(keyword)
    }

    fun respondToInput(input: String): String? {
        val intent = intentParser.parse(input) ?: return null
        val context = buildContextSnapshot()
        val isSpam = memoryStore.markInput(intent.normalizedInput, System.currentTimeMillis())

        val action = commandRouter.route(
            intent = intent,
            context = context,
            isButtonSpam = isSpam
        )

        return when (action) {
            is AssistantAction.ExecuteLauncherCommand -> {
                val actionResult = executeLauncherCommand(action.command)
                actionResult.spokenText
            }

            else -> when (val plan = responseComposer.compose(action, context)) {
            is AssistantResponseComposer.ResponsePlan.CategoryRequest -> {
                val response = speakRequest(plan.request)
                memoryStore.rememberCategory(plan.request.category)
                response
            }

            AssistantResponseComposer.ResponsePlan.RepeatLast -> repeatLast()
            AssistantResponseComposer.ResponsePlan.NoOp -> null
            }
        }
    }

    fun executeLauncherCommand(command: AssistantAction.LauncherCommand): AssistantActionResult.LauncherCommandHandled {
        val result = onLauncherAction?.invoke(command)
            ?: AssistantActionResult.LauncherCommandHandled(
                command = command,
                spokenText = "That launcher action is not wired on this build yet.",
                performed = false
            )
        speakCustom(result.spokenText)
        return result
    }

    fun scheduleIdleTimeout(delayMs: Long = IDLE_TIMEOUT_MS) {
        clearPendingIdleTimeout()
        pendingIdleTimeout = Runnable {
            if (snapshot.state == AssistantState.IDLE) {
                triggerIdleTaunt()
            }
        }.also { mainHandler.postDelayed(it, delayMs) }
    }

    fun cancelIdleTimeout() = clearPendingIdleTimeout()

    private fun speakRequest(request: AiResponseRepository.ResponseRequest): String {
        clearPendingListeningTransition()
        postState(snapshot.copy(state = AssistantState.THINKING))
        lastCategory = request.category
        val response = responseRepository.getResponse(request.copy(mood = currentMood))
        return deliverResponse(response, request.category)
    }

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
            PersonalityMood.BORED -> ReactorAssistant.VoiceProfile.BORED
            PersonalityMood.ALERT -> ReactorAssistant.VoiceProfile.ALERT
            PersonalityMood.SERIOUS -> ReactorAssistant.VoiceProfile.COMMANDER
            PersonalityMood.SNARKY -> ReactorAssistant.VoiceProfile.SNARKY
            PersonalityMood.PLAYFUL -> ReactorAssistant.VoiceProfile.BROADCAST
            PersonalityMood.SAVAGE -> ReactorAssistant.VoiceProfile.COMMANDER
        }

    private fun buildContextSnapshot(): AssistantContextSnapshot = AssistantContextSnapshot(
        mood = currentMood,
        interactionCount = _interactionCount,
        state = snapshot.state,
        lastCategory = lastCategory,
        lastResponse = getLastResponse(),
        timestampMs = System.currentTimeMillis(),
        isSpeaking = personalityLayer.isSpeaking()
    )
}

package com.nerf.launcher.util.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper

class AssistantController(
    context: Context,
    private val responseRepository: AiResponseRepository = AiResponseRepository(context),
    private val personalityLayer: ReactorAssistant = ReactorAssistant(context)
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingListeningTransition: Runnable? = null
    private var snapshot = AssistantStateSnapshot(AssistantState.IDLE)

    var onStateChanged: ((AssistantStateSnapshot) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(snapshot)
        }

    init {
        personalityLayer.onSpeechStarted = { spokenText ->
            postState(AssistantStateSnapshot(AssistantState.SPEAKING, spokenText))
        }
        personalityLayer.onSpeechCompleted = { spokenText ->
            scheduleListening(spokenText)
        }
        personalityLayer.onSpeechError = { spokenText ->
            postState(AssistantStateSnapshot(AssistantState.ERROR, spokenText ?: snapshot.response))
        }
    }

    fun isResponseBankLoaded(): Boolean = responseRepository.isLoaded()

    fun currentSnapshot(): AssistantStateSnapshot = snapshot

    fun wakeAssistant(): String {
        postState(AssistantStateSnapshot(AssistantState.WAKE, snapshot.response))
        return speakCategory(AiResponseRepository.Category.WAKE)
    }

    fun wakeForCommand() {
        postState(AssistantStateSnapshot(AssistantState.WAKE, snapshot.response))
        scheduleListening(snapshot.response)
    }

    fun speakCategory(category: AiResponseRepository.Category): String {
        clearPendingListeningTransition()
        postState(AssistantStateSnapshot(AssistantState.THINKING, snapshot.response))
        val response = responseRepository.getResponse(category)
        postState(AssistantStateSnapshot(AssistantState.RESPONDING, response))
        if (!personalityLayer.speak(response)) {
            postState(AssistantStateSnapshot(AssistantState.MUTED, response))
        }
        return response
    }

    fun speakCustom(text: String): String {
        clearPendingListeningTransition()
        postState(AssistantStateSnapshot(AssistantState.RESPONDING, text))
        if (!personalityLayer.speak(text)) {
            postState(AssistantStateSnapshot(AssistantState.MUTED, text))
        }
        return text
    }

    fun setIdle() {
        clearPendingListeningTransition()
        postState(AssistantStateSnapshot(AssistantState.IDLE, null))
    }

    fun shutdown() {
        clearPendingListeningTransition()
        personalityLayer.shutdown()
    }

    private fun scheduleListening(response: String?) {
        clearPendingListeningTransition()
        val transition = Runnable {
            postState(AssistantStateSnapshot(AssistantState.LISTENING, response))
        }
        pendingListeningTransition = transition
        mainHandler.postDelayed(transition, 220L)
    }

    private fun clearPendingListeningTransition() {
        pendingListeningTransition?.let(mainHandler::removeCallbacks)
        pendingListeningTransition = null
    }

    private fun postState(next: AssistantStateSnapshot) {
        mainHandler.post {
            snapshot = next
            onStateChanged?.invoke(next)
        }
    }
}

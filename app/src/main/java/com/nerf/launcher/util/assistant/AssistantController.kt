package com.nerf.launcher.util.assistant

import android.content.Context

class AssistantController(
    context: Context,
    private val responseRepository: AiResponseRepository = AiResponseRepository(context),
    private val personalityLayer: ReactorAssistant = ReactorAssistant(context)
) {

    fun isResponseBankLoaded(): Boolean = responseRepository.isLoaded()

    fun wakeAssistant(): String {
        return speakCategory(AiResponseRepository.Category.WAKE)
    }

    fun speakCategory(category: AiResponseRepository.Category): String {
        val response = responseRepository.getResponse(category)
        personalityLayer.speak(response)
        return response
    }

    fun speakCustom(text: String): String {
        personalityLayer.speak(text)
        return text
    }

    fun shutdown() {
        personalityLayer.shutdown()
    }
}

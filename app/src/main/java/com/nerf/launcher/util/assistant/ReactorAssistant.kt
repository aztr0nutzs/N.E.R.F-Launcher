package com.nerf.launcher.util.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class ReactorAssistant(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("ReactorAssistant", "The language specified is not supported.")
            } else {
                isReady = true
                tts?.setPitch(0.85f) // Slightly lower pitch for a more serious/sarcastic tone
                tts?.setSpeechRate(0.95f) // Slightly slower delivery for comedic timing
            }
        } else {
            Log.e("ReactorAssistant", "TTS initialization failed.")
        }
    }

    fun speak(text: String): Boolean {
        if (!isReady) return false
        executeSpeech(text)
        return true
    }

    private fun executeSpeech(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AiSpeakId_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

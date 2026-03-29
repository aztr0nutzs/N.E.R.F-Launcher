package com.nerf.launcher.util.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class ReactorAssistant(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val utteranceTexts = ConcurrentHashMap<String, String>()

    var onSpeechStarted: ((String) -> Unit)? = null
    var onSpeechCompleted: ((String) -> Unit)? = null
    var onSpeechError: ((String?) -> Unit)? = null

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
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val text = utteranceId?.let { utteranceTexts[it] } ?: return
                        onSpeechStarted?.invoke(text)
                    }

                    override fun onDone(utteranceId: String?) {
                        val text = utteranceId?.let { utteranceTexts.remove(it) } ?: return
                        onSpeechCompleted?.invoke(text)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        val text = utteranceId?.let { utteranceTexts.remove(it) }
                        onSpeechError?.invoke(text)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        val text = utteranceId?.let { utteranceTexts.remove(it) }
                        onSpeechError?.invoke(text)
                    }
                })
            }
        } else {
            Log.e("ReactorAssistant", "TTS initialization failed.")
        }
    }

    fun speak(text: String): Boolean {
        if (!isReady) return false
        val utteranceId = "AiSpeakId_${System.currentTimeMillis()}"
        utteranceTexts[utteranceId] = text
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            utteranceTexts.remove(utteranceId)
            onSpeechError?.invoke(text)
            return false
        }
        return result == TextToSpeech.SUCCESS
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        utteranceTexts.clear()
    }
}

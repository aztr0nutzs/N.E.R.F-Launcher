package com.nerf.launcher.util.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.nerf.launcher.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class ReactorAssistant(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    
    // In-memory dictionary populated from JSON
    private val responseLibrary = mutableMapOf<Category, List<String>>()

    enum class Category {
        WAKE, NETWORK_SCAN, ERROR, DIAGNOSTICS, RANDOM_SNARK
    }

    init {
        loadResponsesFromJson()
        tts = TextToSpeech(context, this)
    }

    /**
     * Reads the JSON file from res/raw and parses it into the responseLibrary map.
     */
    private fun loadResponsesFromJson() {
        try {
            val inputStream = context.resources.openRawResource(R.raw.reactor_ai_responses)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            
            val jsonObject = JSONObject(jsonString)

            for (category in Category.values()) {
                val key = category.name
                if (jsonObject.has(key)) {
                    val jsonArray = jsonObject.getJSONArray(key)
                    val phrases = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        phrases.add(jsonArray.getString(i))
                    }
                    responseLibrary[category] = phrases
                } else {
                    responseLibrary[category] = listOf("Data for $key is missing. Someone forgot to update the JSON.")
                }
            }
            Log.d("ReactorAssistant", "Successfully loaded JSON response library.")
        } catch (e: Exception) {
            Log.e("ReactorAssistant", "Failed to parse JSON responses: ${e.message}")
            // Production-safe fallback so the app doesn't crash if the file is missing
            for (category in Category.values()) {
                responseLibrary[category] = listOf("Audio subsystem error. JSON dictionary not found.")
            }
        }
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

    fun speakCategory(category: Category) {
        if (!isReady) return
        val phrases = responseLibrary[category]
        val phrase = phrases?.random() ?: "Error accessing memory banks."
        executeSpeech(phrase)
    }

    fun speakCustom(text: String) {
        if (!isReady) return
        executeSpeech(text)
    }

    private fun executeSpeech(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AiSpeakId_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

package com.nerf.launcher.util.assistant

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

// ─────────────────────────────────────────────────────────────────────────────
//  ReactorAssistant
//
//  Wraps Android TextToSpeech and exposes a clean, event-driven API.
//  Features:
//    • Multiple named VoiceProfiles (pitch + rate presets)
//    • Interrupt speak (QUEUE_FLUSH) and queued speak (QUEUE_ADD)
//    • Utterance progress callbacks (started, completed, error)
//    • isSpeaking() query, stop(), pause/resume helpers
//    • Graceful shutdown and resource cleanup
//    • Engine warm-up preload call
// ─────────────────────────────────────────────────────────────────────────────

class ReactorAssistant(private val context: Context) : TextToSpeech.OnInitListener {

    // ── Voice Profiles ────────────────────────────────────────────────────────

    /**
     * Named presets for pitch and speech rate.
     * Applied automatically when [setVoiceProfile] is called.
     */
    enum class VoiceProfile(
        val pitch: Float,
        val rate: Float,
        val label: String
    ) {
        /** Default: dry, measured, slightly sardonic. */
        SNARKY(pitch = 0.90f, rate = 0.95f, label = "Snarky"),

        /** Authoritative, deliberate command voice. */
        COMMANDER(pitch = 0.78f, rate = 0.88f, label = "Commander"),

        /** Clipped, mission-precise briefing mode. */
        TACTICAL(pitch = 0.85f, rate = 1.00f, label = "Tactical"),

        /** Heightened urgency — rapid, sharp warnings. */
        ALERT(pitch = 1.00f, rate = 1.18f, label = "Alert"),

        /** Low, conspiratorial — stealth ops. */
        WHISPER(pitch = 0.95f, rate = 0.80f, label = "Whisper"),

        /** Wide, resonant — dramatic announcements and broadcasts. */
        BROADCAST(pitch = 0.72f, rate = 0.85f, label = "Broadcast"),

        /** Flat, disengaged — Reactor is clearly bored. */
        BORED(pitch = 0.88f, rate = 0.88f, label = "Bored")
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "ReactorAssistant"
        private const val UTTERANCE_PREFIX = "REACTOR_"
    }

    // ── Internal State ────────────────────────────────────────────────────────

    private var tts: TextToSpeech? = null
    private val _isReady = AtomicBoolean(false)
    private val releaseState = AtomicBoolean(false)

    /** True once the TTS engine has initialised successfully. */
    val isReady: Boolean get() = _isReady.get()

    private var currentProfile = VoiceProfile.SNARKY

    /** Maps utterance IDs → original text for callback delivery. */
    private val utteranceTexts = ConcurrentHashMap<String, String>()

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /** Invoked on the TTS thread when an utterance begins playing. */
    var onSpeechStarted: ((spokenText: String) -> Unit)? = null

    /** Invoked on the TTS thread when an utterance finishes cleanly. */
    var onSpeechCompleted: ((spokenText: String) -> Unit)? = null

    /** Invoked on the TTS thread when an utterance fails. */
    var onSpeechError: ((spokenText: String?) -> Unit)? = null

    /** Invoked whenever the TTS engine ready state changes. */
    var onReadyChanged: ((isReady: Boolean) -> Unit)? = null

    /** Invoked when the active voice profile changes. */
    var onProfileChanged: ((profile: VoiceProfile) -> Unit)? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (releaseState.get()) return
        if (status == TextToSpeech.SUCCESS) {
            val langResult = tts?.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                langResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e(TAG, "Locale.US not supported on this device.")
            } else {
                applyVoiceProfile(currentProfile)
                tts?.setOnUtteranceProgressListener(buildProgressListener())
                _isReady.set(true)
                onReadyChanged?.invoke(true)
                Log.d(TAG, "ReactorAssistant ready. Profile: ${currentProfile.label}")
            }
        } else {
            Log.e(TAG, "TTS init failed. Status: $status")
            onReadyChanged?.invoke(false)
        }
    }

    // ── Public Speaking API ───────────────────────────────────────────────────

    /**
     * Speaks [text] immediately, interrupting any ongoing utterance.
     * Returns true if the speak call was accepted by the TTS engine.
     */
    fun speak(text: String): Boolean = speakInternal(text, interrupt = true)

    /**
     * Adds [text] to the back of the TTS queue without interrupting
     * what is currently playing.
     */
    fun speakQueued(text: String): Boolean = speakInternal(text, interrupt = false)

    /** Alias for [speak] — explicitly signals intent to interrupt. */
    fun speakInterrupt(text: String): Boolean = speakInternal(text, interrupt = true)

    /** Returns true if the TTS engine is actively producing audio. */
    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    /** Stops all current and queued speech immediately. */
    fun stop() {
        if (releaseState.get()) return
        tts?.stop()
    }

    /**
     * Stops current speech without clearing the engine queue —
     * effectively a soft pause. Resume by calling [speak] or [speakQueued].
     */
    fun pause() {
        if (releaseState.get()) return
        tts?.stop()
        Log.d(TAG, "Speech paused.")
    }

    // ── Voice Profile API ─────────────────────────────────────────────────────

    /**
     * Switches to the given [VoiceProfile], updating pitch and speech rate.
     * Safe to call before TTS is ready — applied on init if pending.
     */
    fun setVoiceProfile(profile: VoiceProfile) {
        currentProfile = profile
        if (releaseState.get()) return
        if (_isReady.get()) {
            applyVoiceProfile(profile)
            onProfileChanged?.invoke(profile)
            Log.d(TAG, "Voice profile → ${profile.label} (pitch=${profile.pitch}, rate=${profile.rate})")
        }
    }

    /** Returns the currently active [VoiceProfile]. */
    fun getVoiceProfile(): VoiceProfile = currentProfile

    /** Advances to the next [VoiceProfile] in enum order, wrapping around. */
    fun cycleVoiceProfile() {
        val profiles = VoiceProfile.values()
        setVoiceProfile(profiles[(profiles.indexOf(currentProfile) + 1) % profiles.size])
    }

    // ── Pitch / Rate Fine-Tuning ──────────────────────────────────────────────

    /** Overrides pitch directly (0.5 – 2.0). Does not change [currentProfile]. */
    fun setPitch(pitch: Float) {
        if (releaseState.get()) return
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    /** Overrides speech rate directly (0.5 – 2.0). Does not change [currentProfile]. */
    fun setSpeechRate(rate: Float) {
        if (releaseState.get()) return
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    // ── Engine Warm-Up ────────────────────────────────────────────────────────

    /**
     * Silently pre-warms the TTS engine to reduce first-utterance latency.
     * Call this after [isReady] becomes true, before the first real speech event.
     */
    fun preload() {
        if (!_isReady.get()) return
        val warmId = "${UTTERANCE_PREFIX}WARMUP"
        val params = Bundle()
        tts?.speak(" ", TextToSpeech.QUEUE_FLUSH, params, warmId)
        Log.d(TAG, "TTS engine pre-warmed.")
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    /**
     * Stops all speech, releases TTS resources, and marks the engine as not ready.
     * After calling this, [isReady] returns false and a new instance is required.
     */
    fun release() {
        if (!releaseState.compareAndSet(false, true)) return
        tts?.stop()
        tts?.shutdown()
        tts = null
        utteranceTexts.clear()
        _isReady.set(false)
        onReadyChanged?.invoke(false)
        Log.d(TAG, "ReactorAssistant shut down.")
    }

    fun shutdown() {
        release()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun speakInternal(text: String, interrupt: Boolean): Boolean {
        if (releaseState.get()) return false
        if (!_isReady.get()) {
            Log.w(TAG, "TTS not ready — discarding: \"${text.take(60)}\"")
            return false
        }
        if (text.isBlank()) return false

        val id = "${UTTERANCE_PREFIX}${UUID.randomUUID()}"
        utteranceTexts[id] = text

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val queueMode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val result = tts?.speak(text, queueMode, params, id)

        if (result == TextToSpeech.ERROR) {
            utteranceTexts.remove(id)
            onSpeechError?.invoke(text)
            Log.e(TAG, "TTS speak() returned ERROR for: \"${text.take(60)}\"")
            return false
        }
        return true
    }

    private fun applyVoiceProfile(profile: VoiceProfile) {
        tts?.setPitch(profile.pitch)
        tts?.setSpeechRate(profile.rate)
    }

    private fun buildProgressListener(): UtteranceProgressListener =
        object : UtteranceProgressListener() {

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
                Log.e(TAG, "TTS error code $errorCode on utterance $utteranceId")
                onSpeechError?.invoke(text)
            }
        }
}

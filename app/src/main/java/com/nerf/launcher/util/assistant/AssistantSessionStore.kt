package com.nerf.launcher.util.assistant

import android.content.Context
import android.content.SharedPreferences

class AssistantSessionStore(context: Context) {

    data class AssistantPreferences(
        val mood: PersonalityMood = PersonalityMood.SNARKY,
        val voiceProfile: ReactorAssistant.VoiceProfile = ReactorAssistant.VoiceProfile.SNARKY,
        val muted: Boolean = false,
        val verbosityLevel: Int = DEFAULT_VERBOSITY
    )

    data class AssistantSessionMemory(
        val lastCommand: String? = null,
        val lastResponse: String? = null,
        val lastLauncherSurface: String? = null,
        val lastSuccessfulAction: String? = null
    )

    companion object {
        private const val PREF_NAME = "assistant_session_prefs"

        private const val KEY_MOOD = "assistant_mood"
        private const val KEY_VOICE_PROFILE = "assistant_voice_profile"
        private const val KEY_MUTED = "assistant_muted"
        private const val KEY_VERBOSITY_LEVEL = "assistant_verbosity"

        private const val KEY_LAST_COMMAND = "assistant_last_command"
        private const val KEY_LAST_RESPONSE = "assistant_last_response"
        private const val KEY_LAST_SURFACE = "assistant_last_surface"
        private const val KEY_LAST_SUCCESS_ACTION = "assistant_last_success_action"

        private const val MIN_VERBOSITY = 1
        private const val MAX_VERBOSITY = 3
        const val DEFAULT_VERBOSITY = 2
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun loadPreferences(): AssistantPreferences {
        val mood = prefs.getString(KEY_MOOD, PersonalityMood.SNARKY.name)
            ?.let(::resolveMood)
            ?: PersonalityMood.SNARKY
        val voiceProfile = prefs.getString(KEY_VOICE_PROFILE, ReactorAssistant.VoiceProfile.SNARKY.name)
            ?.let(::resolveVoiceProfile)
            ?: ReactorAssistant.VoiceProfile.SNARKY
        val muted = prefs.getBoolean(KEY_MUTED, false)
        val verbosity = prefs.getInt(KEY_VERBOSITY_LEVEL, DEFAULT_VERBOSITY)
            .coerceIn(MIN_VERBOSITY, MAX_VERBOSITY)

        return AssistantPreferences(
            mood = mood,
            voiceProfile = voiceProfile,
            muted = muted,
            verbosityLevel = verbosity
        )
    }

    fun persistPreferences(preferences: AssistantPreferences) {
        prefs.edit()
            .putString(KEY_MOOD, preferences.mood.name)
            .putString(KEY_VOICE_PROFILE, preferences.voiceProfile.name)
            .putBoolean(KEY_MUTED, preferences.muted)
            .putInt(KEY_VERBOSITY_LEVEL, preferences.verbosityLevel.coerceIn(MIN_VERBOSITY, MAX_VERBOSITY))
            .apply()
    }

    fun loadSessionMemory(): AssistantSessionMemory = AssistantSessionMemory(
        lastCommand = prefs.getString(KEY_LAST_COMMAND, null),
        lastResponse = prefs.getString(KEY_LAST_RESPONSE, null),
        lastLauncherSurface = prefs.getString(KEY_LAST_SURFACE, null),
        lastSuccessfulAction = prefs.getString(KEY_LAST_SUCCESS_ACTION, null)
    )

    fun persistSessionMemory(memory: AssistantSessionMemory) {
        prefs.edit()
            .putString(KEY_LAST_COMMAND, memory.lastCommand)
            .putString(KEY_LAST_RESPONSE, memory.lastResponse)
            .putString(KEY_LAST_SURFACE, memory.lastLauncherSurface)
            .putString(KEY_LAST_SUCCESS_ACTION, memory.lastSuccessfulAction)
            .apply()
    }

    private fun resolveMood(name: String): PersonalityMood? =
        PersonalityMood.values().firstOrNull { it.name == name }

    private fun resolveVoiceProfile(name: String): ReactorAssistant.VoiceProfile? =
        ReactorAssistant.VoiceProfile.values().firstOrNull { it.name == name }
}

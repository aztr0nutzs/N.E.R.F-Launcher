package com.nerf.launcher.util.assistant

import android.content.Context
import android.util.Log
import com.nerf.launcher.BuildConfig
import com.nerf.launcher.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedList
import kotlin.math.max

class AiResponseRepository(private val context: Context) {

    enum class Category {
        WAKE,
        SHUTDOWN,
        REBOOT,

        NETWORK_SCAN,
        SCANNING,
        DIAGNOSTICS,
        SYSTEM_ALERT,
        BATTERY_LOW,
        UPDATE_AVAILABLE,
        NETWORK_SUCCESS,
        NETWORK_FAILURE,
        STATUS_REPORT,
        ROUTER_CONTROL,

        COMMAND_RECEIVED,
        APP_LAUNCH,
        PERMISSION_REQUEST,
        ERROR,
        WARNING,
        SUCCESS,
        UNKNOWN_COMMAND,

        LAUNCH,
        RELOAD,
        TARGET_ACQUIRED,
        THREAT_DETECTED,
        MISSION_BRIEF,
        COUNTDOWN,
        TACTICAL_ANALYSIS,
        STEALTH_MODE,
        VICTORY,
        DEFEAT,

        RANDOM_SNARK,
        IDLE_TAUNT,
        BORED,
        AMBIENT,
        USER_ABSENT,
        COMPLIMENT,
        BUTTON_SPAM,
        THEME_SWITCH
    }

    data class ResponseRequest(
        val category: Category,
        val mood: PersonalityMood = PersonalityMood.SNARKY,
        val tags: Set<String> = emptySet(),
        val preferredText: String? = null,
        val templateValues: Map<String, String> = emptyMap(),
        val allowFallbackToGeneric: Boolean = true
    )

    data class ResponseEntry(
        val text: String,
        val weight: Int = 1,
        val moods: Set<PersonalityMood> = emptySet(),
        val tags: Set<String> = emptySet()
    )

    companion object {
        private const val TAG = "AiResponseRepository"
        const val DEFAULT_HISTORY_DEPTH = 8
    }

    private val appContext = context.applicationContext
    private var responseLibrary: Map<Category, List<ResponseEntry>>? = null
    private val recentHistory = mutableMapOf<Category, LinkedList<String>>()
    private var historyDepth = DEFAULT_HISTORY_DEPTH

    fun isLoaded(): Boolean {
        ensureLoaded()
        return responseLibrary != null
    }

    fun getResponse(category: Category): String =
        getResponse(ResponseRequest(category = category))

    fun getResponse(
        category: Category,
        mood: PersonalityMood,
        tags: Set<String> = emptySet(),
        templateValues: Map<String, String> = emptyMap()
    ): String = getResponse(
        ResponseRequest(
            category = category,
            mood = mood,
            tags = tags,
            templateValues = templateValues
        )
    )

    fun getResponse(request: ResponseRequest): String {
        ensureLoaded()
        request.preferredText
            ?.takeIf { it.isNotBlank() }
            ?.let {
                val rendered = applyTemplate(it.trim(), request.templateValues)
                return rememberAndReturn(request.category, rendered)
            }

        val lines = responseLibrary?.get(request.category).orEmpty()
        if (lines.isEmpty()) {
            return fallbackResponse(request.category, request.mood)
        }

        val chosen = pickFreshWeighted(request, lines)
            ?: if (request.allowFallbackToGeneric) {
                responseLibrary?.get(Category.RANDOM_SNARK)
                    ?.let { pickFreshWeighted(request.copy(category = Category.RANDOM_SNARK), it) }
            } else {
                null
            }

        val rawText = chosen?.text ?: fallbackResponse(request.category, request.mood)
        val renderedText = applyTemplate(rawText, request.templateValues)
        return rememberAndReturn(request.category, renderedText)
    }

    fun getResponseForMood(category: Category, mood: PersonalityMood): String =
        getResponse(category = category, mood = mood)

    fun getSequence(vararg categories: Category): List<String> =
        categories.map { getResponse(it) }

    fun getAllResponses(category: Category): List<String> {
        ensureLoaded()
        return responseLibrary?.get(category).orEmpty().map { it.text }
    }

    fun getResponseCount(category: Category): Int = getAllResponses(category).size

    fun getLibrarySummary(): Map<Category, Int> {
        ensureLoaded()
        return Category.values().associateWith { getResponseCount(it) }
    }

    fun searchResponses(query: String, limit: Int = 20): List<Pair<Category, String>> {
        ensureLoaded()
        val needle = query.trim().lowercase()
        if (needle.isBlank()) return emptyList()

        return responseLibrary.orEmpty()
            .flatMap { (category, entries) ->
                entries.mapNotNull { entry ->
                    if (entry.text.lowercase().contains(needle)) category to entry.text else null
                }
            }
            .take(limit.coerceIn(1, 100))
    }

    fun setHistoryDepth(depth: Int) {
        historyDepth = depth.coerceIn(1, 24)
    }

    fun clearHistory(category: Category) {
        recentHistory[category]?.clear()
    }

    fun clearAllHistory() {
        recentHistory.clear()
    }

    fun reload() {
        responseLibrary = null
        recentHistory.clear()
        ensureLoaded()
        logDebug("Response bank reloaded. ${getLibrarySummary().values.sum()} total responses.")
    }

    private fun ensureLoaded() {
        if (responseLibrary != null) return

        responseLibrary = try {
            val inputStream = appContext.resources.openRawResource(R.raw.reactor_ai_responses)
            val jsonString = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            Category.values().associateWith { category ->
                if (jsonObject.has(category.name)) {
                    parseCategoryEntries(jsonObject.getJSONArray(category.name))
                } else {
                    logDebug("Category '${category.name}' missing from response JSON. Using fallback.")
                    listOf(ResponseEntry(fallbackResponse(category, PersonalityMood.SNARKY)))
                }
            }.also {
                val total = it.values.sumOf { entries -> entries.size }
                logDebug("Response bank loaded: ${it.size} categories, $total total lines.")
            }
        } catch (e: Exception) {
            logDebug("Failed to load assistant response bank.")
            null
        }
    }


    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    private fun parseCategoryEntries(array: JSONArray): List<ResponseEntry> {
        val parsed = buildList {
            for (index in 0 until array.length()) {
                when (val item = array.get(index)) {
                    is String -> add(ResponseEntry(text = item.trim()).normalized())
                    is JSONObject -> add(parseEntryObject(item).normalized())
                    else -> logDebug("Ignoring unsupported response entry type at index $index")
                }
            }
        }.filter { it.text.isNotBlank() }

        return if (parsed.isNotEmpty()) parsed else listOf(
            ResponseEntry("Response bank entry was empty. That's encouraging.")
        )
    }

    private fun parseEntryObject(obj: JSONObject): ResponseEntry {
        val text = obj.optString("text").trim()
        val weight = max(1, obj.optInt("weight", 1))
        val moods = obj.optJSONArray("moods").toMoodSet()
        val tags = obj.optJSONArray("tags").toStringSet()
        return ResponseEntry(text = text, weight = weight, moods = moods, tags = tags)
    }

    private fun pickFreshWeighted(
        request: ResponseRequest,
        entries: List<ResponseEntry>
    ): ResponseEntry? {
        val history = recentHistory.getOrPut(request.category) { LinkedList() }

        val filtered = entries.filter { entry ->
            entry.text !in history &&
                (entry.moods.isEmpty() || request.mood in entry.moods) &&
                (request.tags.isEmpty() || entry.tags.isEmpty() || entry.tags.any { it in request.tags })
        }

        val fallbackFiltered = entries.filter { entry ->
            entry.text !in history &&
                (entry.moods.isEmpty() || request.mood in entry.moods)
        }

        val pool = when {
            filtered.isNotEmpty() -> filtered
            fallbackFiltered.isNotEmpty() -> fallbackFiltered
            entries.isNotEmpty() -> entries
            else -> return null
        }

        val totalWeight = pool.sumOf { it.weight.coerceAtLeast(1) }
        var draw = (0 until totalWeight).random()

        for (entry in pool) {
            draw -= entry.weight.coerceAtLeast(1)
            if (draw < 0) return entry
        }

        return pool.random()
    }

    private fun rememberAndReturn(category: Category, text: String): String {
        val history = recentHistory.getOrPut(category) { LinkedList() }
        history.addFirst(text)
        if (history.size > historyDepth) history.removeLast()
        return text
    }

    private fun fallbackResponse(category: Category, mood: PersonalityMood): String = when (category) {
        Category.ERROR -> "System error encountered. I'm fixing it because apparently that falls to me."
        Category.WARNING -> "Warning issued. Ignore it if you're committed to consequences."
        Category.SUCCESS -> "Task complete. A rare but welcome sight."
        Category.UNKNOWN_COMMAND -> "Unknown command. Try a sentence with an actual destination."
        Category.BUTTON_SPAM -> "Repeated input detected. The button is not becoming more correct."
        Category.STATUS_REPORT -> when (mood) {
            PersonalityMood.TACTICAL -> "Status report: systems ready, variables contained, proceed."
            PersonalityMood.ALERT -> "Status report: alert posture active. Respond immediately."
            PersonalityMood.BORED -> "status report: operational. thrilling."
            PersonalityMood.SERIOUS -> "Status report: systems stable and standing by."
            PersonalityMood.SNARKY -> "Status report: stable, armed with patience, and waiting on you."
            PersonalityMood.PLAYFUL -> "Status report: systems bright, stable, and ready for a better command."
            PersonalityMood.SAVAGE -> "Status report: stable. The weakest link remains user input."
        }
        else -> when (mood) {
            PersonalityMood.TACTICAL -> "Acknowledged. Tactical response bank is temporarily offline."
            PersonalityMood.ALERT -> "Alert: response bank unavailable for ${category.name.lowercase()}."
            PersonalityMood.BORED -> "response bank offline for ${category.name.lowercase().replace('_', ' ')}."
            PersonalityMood.SERIOUS -> "Response bank unavailable for ${category.name.lowercase().replace('_', ' ')}."
            PersonalityMood.SNARKY -> "Response bank unavailable for ${category.name.lowercase().replace('_', ' ')}. Inspiring."
            PersonalityMood.PLAYFUL -> "Response bank unavailable for ${category.name.lowercase().replace('_', ' ')}. Improvisation mode is online."
            PersonalityMood.SAVAGE -> "Response bank unavailable for ${category.name.lowercase().replace('_', ' ')}. Even the fallback judged this request."
        }
    }


    private fun applyTemplate(template: String, templateValues: Map<String, String>): String {
        if (templateValues.isEmpty()) return template
        val normalizedValues = templateValues
            .mapKeys { it.key.trim().lowercase() }
            .mapValues { it.value.trim() }

        val pattern = Regex("\\{\\{([a-zA-Z0-9_\\-]+)}}")
        return pattern.replace(template) { matchResult ->
            val key = matchResult.groupValues.getOrNull(1)?.trim()?.lowercase().orEmpty()
            normalizedValues[key]?.takeIf { it.isNotBlank() } ?: matchResult.value
        }
    }

    private fun JSONArray?.toMoodSet(): Set<PersonalityMood> {
        if (this == null) return emptySet()
        val moods = mutableSetOf<PersonalityMood>()
        for (index in 0 until length()) {
            runCatching {
                PersonalityMood.valueOf(getString(index).trim().uppercase())
            }.onSuccess(moods::add)
        }
        return moods
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        val tags = mutableSetOf<String>()
        for (index in 0 until length()) {
            val value = optString(index).trim().lowercase()
            if (value.isNotBlank()) tags.add(value)
        }
        return tags
    }

    private fun ResponseEntry.normalized(): ResponseEntry =
        copy(text = text.trim(), weight = weight.coerceAtLeast(1))
}

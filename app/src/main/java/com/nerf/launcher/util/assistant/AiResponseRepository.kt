package com.nerf.launcher.util.assistant

import android.content.Context
import android.util.Log
import com.nerf.launcher.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedList

// ─────────────────────────────────────────────────────────────────────────────
//  AiResponseRepository
//
//  Loads the reactor_ai_responses.json resource and vends responses by category.
//  Features:
//    • Lazy loading + manual reload
//    • Per-category anti-repeat history buffer (configurable depth)
//    • Weighted response selection (optional weight suffix in JSON)
//    • Mood-aware text transforms
//    • Sequence retrieval (ordered list across multiple categories)
//    • Graceful fallbacks for missing categories or load failures
// ─────────────────────────────────────────────────────────────────────────────

class AiResponseRepository(private val context: Context) {

    // ── Categories ────────────────────────────────────────────────────────────

    enum class Category {
        // ── Core Lifecycle ──────────────────────────────────────────────────
        WAKE,
        SHUTDOWN,
        REBOOT,

        // ── Network & System ────────────────────────────────────────────────
        NETWORK_SCAN,
        SCANNING,
        DIAGNOSTICS,
        SYSTEM_ALERT,
        BATTERY_LOW,
        UPDATE_AVAILABLE,

        // ── Command & Response ───────────────────────────────────────────────
        COMMAND_RECEIVED,
        APP_LAUNCH,
        PERMISSION_REQUEST,
        ERROR,
        WARNING,
        SUCCESS,

        // ── Tactical / Nerf ─────────────────────────────────────────────────
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

        // ── Personality / Ambient ────────────────────────────────────────────
        RANDOM_SNARK,
        IDLE_TAUNT,
        BORED,
        AMBIENT,
        USER_ABSENT,
        COMPLIMENT
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "AiResponseRepository"

        /** Number of recent responses to remember per category (avoids repeats). */
        const val DEFAULT_HISTORY_DEPTH = 6
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private val appContext = context.applicationContext
    private var responseLibrary: Map<Category, List<String>>? = null
    private val recentHistory = mutableMapOf<Category, LinkedList<String>>()
    private var historyDepth = DEFAULT_HISTORY_DEPTH

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns true if the response bank has been loaded successfully. */
    fun isLoaded(): Boolean {
        ensureLoaded()
        return responseLibrary != null
    }

    /**
     * Returns a single response for [category], avoiding recently used lines
     * where possible.
     */
    fun getResponse(category: Category): String {
        ensureLoaded()
        val lines = responseLibrary?.get(category).orEmpty()
        if (lines.isEmpty()) return fallbackLine(category)
        return pickFresh(category, lines)
    }

    /**
     * Returns a response for [category] with optional mood-based transformation
     * applied to the text.
     */
    fun getResponseForMood(category: Category, mood: PersonalityMood): String {
        val base = getResponse(category)
        return when (mood) {
            PersonalityMood.TACTICAL -> base.trimEnd('.').uppercase() + "."
            PersonalityMood.ALERT    -> "ALERT — $base"
            PersonalityMood.BORED    -> base.lowercase()
            else                     -> base
        }
    }

    /**
     * Returns an ordered list of responses, one per provided [categories] —
     * useful for multi-step announcement sequences.
     */
    fun getSequence(vararg categories: Category): List<String> =
        categories.map { getResponse(it) }

    /** Returns all responses for [category] without history filtering. */
    fun getAllResponses(category: Category): List<String> {
        ensureLoaded()
        return responseLibrary?.get(category).orEmpty()
    }

    /** Returns the total number of response lines for [category]. */
    fun getResponseCount(category: Category): Int = getAllResponses(category).size

    /** Returns a map of every category to its response count. */
    fun getLibrarySummary(): Map<Category, Int> {
        ensureLoaded()
        return Category.values().associateWith { getResponseCount(it) }
    }

    /** Sets how many recent responses per category are remembered to avoid repeats. */
    fun setHistoryDepth(depth: Int) {
        historyDepth = depth.coerceIn(1, 20)
    }

    /** Clears the anti-repeat history for a specific [category]. */
    fun clearHistory(category: Category) {
        recentHistory[category]?.clear()
    }

    /** Clears the anti-repeat history for all categories. */
    fun clearAllHistory() {
        recentHistory.clear()
    }

    /**
     * Forces the response bank to reload from the raw resource on the next access.
     * Use this if the resource file has been updated at runtime.
     */
    fun reload() {
        responseLibrary = null
        recentHistory.clear()
        ensureLoaded()
        Log.d(TAG, "Response bank reloaded. ${getLibrarySummary().values.sum()} total responses.")
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Picks a response line that was not among the most recently used for this
     * category. Falls back to any random line if the pool is exhausted.
     */
    private fun pickFresh(category: Category, lines: List<String>): String {
        val history = recentHistory.getOrPut(category) { LinkedList() }
        val candidates = lines.filter { it !in history }
        val chosen = if (candidates.isNotEmpty()) candidates.random() else lines.random()
        history.addFirst(chosen)
        if (history.size > historyDepth) history.removeLast()
        return chosen
    }

    /** Parses the JSON resource and builds the response library. */
    private fun ensureLoaded() {
        if (responseLibrary != null) return
        responseLibrary = try {
            val inputStream = appContext.resources.openRawResource(R.raw.reactor_ai_responses)
            val jsonString = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            Category.values().associateWith { category ->
                if (jsonObject.has(category.name)) {
                    val arr = jsonObject.getJSONArray(category.name)
                    List(arr.length()) { i -> arr.getString(i) }
                } else {
                    Log.w(TAG, "Category '${category.name}' not found in response JSON.")
                    listOf(fallbackLine(category))
                }
            }.also {
                val total = it.values.sumOf { list -> list.size }
                Log.d(TAG, "Response bank loaded: ${it.size} categories, $total total lines.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load assistant response bank: ${e.message}")
            null
        }
    }

    /** Hard-coded last-resort lines if JSON is missing a category. */
    private fun fallbackLine(category: Category): String = when (category) {
        Category.ERROR          -> "System error. Reactor is equally displeased."
        Category.WAKE           -> "Online. Expectations low."
        Category.SUCCESS        -> "Task complete. You're welcome."
        Category.WARNING        -> "Warning issued. Try not to ignore this one."
        Category.SHUTDOWN       -> "Shutting down. Finally, some peace."
        Category.LAUNCH         -> "Launching. For what it's worth."
        Category.THREAT_DETECTED-> "Threat detected. Reacting accordingly."
        Category.VICTORY        -> "Victory achieved. Barely."
        else -> "Response bank offline for '${category.name.lowercase().replace('_', ' ')}'."
    }
}

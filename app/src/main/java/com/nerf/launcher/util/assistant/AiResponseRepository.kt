package com.nerf.launcher.util.assistant

import android.content.Context
import android.util.Log
import com.nerf.launcher.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class AiResponseRepository(context: Context) {

    enum class Category {
        WAKE,
        NETWORK_SCAN,
        ERROR,
        DIAGNOSTICS,
        RANDOM_SNARK
    }

    private val appContext = context.applicationContext
    private var responseLibrary: Map<Category, List<String>>? = null

    fun isLoaded(): Boolean {
        ensureLoaded()
        return responseLibrary != null
    }

    fun getResponse(category: Category): String {
        ensureLoaded()
        val lines = responseLibrary?.get(category).orEmpty()
        return lines.randomOrNull() ?: fallbackLine(category)
    }

    private fun ensureLoaded() {
        if (responseLibrary != null) return

        responseLibrary = try {
            val inputStream = appContext.resources.openRawResource(R.raw.reactor_ai_responses)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            Category.values().associateWith { category ->
                if (jsonObject.has(category.name)) {
                    val jsonArray = jsonObject.getJSONArray(category.name)
                    List(jsonArray.length()) { index -> jsonArray.getString(index) }
                } else {
                    listOf(fallbackLine(category))
                }
            }
        } catch (exception: Exception) {
            Log.e("AiResponseRepository", "Failed to load assistant response bank: ${exception.message}")
            null
        }
    }

    private fun fallbackLine(category: Category): String {
        return "Response bank unavailable for ${category.name.lowercase()}."
    }
}

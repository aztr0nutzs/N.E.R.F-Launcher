package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

class AssistantMemoryStore(
    private val buttonSpamWindowMs: Long = 1_200L,
    private val buttonSpamThreshold: Int = 4,
    private val maxRecentIntents: Int = 12,
    private val maxRecentCategories: Int = 12
) {

    private var lastInputSignature: String? = null
    private var lastInputTimestampMs: Long = 0L
    private var repeatedInputCount: Int = 0

    private val recentCategories = ArrayDeque<Category>()
    private val recentIntents = ArrayDeque<AssistantIntent>()

    fun markInput(signature: String, nowMs: Long): Boolean {
        if (signature == lastInputSignature && nowMs - lastInputTimestampMs <= buttonSpamWindowMs) {
            repeatedInputCount++
        } else {
            repeatedInputCount = 1
        }
        lastInputSignature = signature
        lastInputTimestampMs = nowMs
        return repeatedInputCount >= buttonSpamThreshold
    }

    fun rememberIntent(intent: AssistantIntent) {
        recentIntents.addFirst(intent)
        trimToSize(recentIntents, maxRecentIntents)
    }

    fun latestIntent(): AssistantIntent? = recentIntents.firstOrNull()

    fun rememberCategory(category: Category) {
        recentCategories.addFirst(category)
        trimToSize(recentCategories, maxRecentCategories)
    }

    fun latestCategory(): Category? = recentCategories.firstOrNull()

    fun recentCategoryList(): List<Category> = recentCategories.toList()

    private fun <T> trimToSize(buffer: ArrayDeque<T>, maxSize: Int) {
        while (buffer.size > maxSize) {
            buffer.removeLast()
        }
    }
}

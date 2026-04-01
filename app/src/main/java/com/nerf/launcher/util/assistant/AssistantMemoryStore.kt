package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

class AssistantMemoryStore(
    private val buttonSpamWindowMs: Long = 1_200L,
    private val buttonSpamThreshold: Int = 4
) {

    private var lastInputSignature: String? = null
    private var lastInputTimestampMs: Long = 0L
    private var repeatedInputCount: Int = 0
    private val recentCategories = ArrayDeque<Category>()

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

    fun rememberCategory(category: Category, maxSize: Int = 12) {
        recentCategories.addFirst(category)
        while (recentCategories.size > maxSize) recentCategories.removeLast()
    }

    fun latestCategory(): Category? = recentCategories.firstOrNull()
}

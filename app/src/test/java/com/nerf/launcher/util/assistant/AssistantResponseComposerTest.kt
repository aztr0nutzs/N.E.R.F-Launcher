package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantResponseComposerTest {

    private val composer = AssistantResponseComposer()

    @Test
    fun `launcher command with response category composes category request preserving spoken text`() {
        val plan = composer.compose(
            result = AssistantActionResult.LauncherCommandHandled(
                command = AssistantAction.LauncherCommand.START_LOCAL_NETWORK_SCAN,
                spokenText = "Starting local network scan now.",
                outcome = AssistantActionResult.LauncherOutcome.PERFORMED,
                responseCategory = Category.SCANNING
            ),
            context = contextSnapshot()
        )

        assertTrue(plan is AssistantResponseComposer.ResponsePlan.CategoryRequest)
        val request = (plan as AssistantResponseComposer.ResponsePlan.CategoryRequest).request
        assertEquals(Category.SCANNING, request.category)
        assertEquals("Starting local network scan now.", request.preferredText)
    }

    @Test
    fun `launcher command without response category remains direct text`() {
        val plan = composer.compose(
            result = AssistantActionResult.LauncherCommandHandled(
                command = AssistantAction.LauncherCommand.OPEN_SETTINGS,
                spokenText = "Opening launcher settings now.",
                outcome = AssistantActionResult.LauncherOutcome.PERFORMED
            ),
            context = contextSnapshot()
        )

        assertTrue(plan is AssistantResponseComposer.ResponsePlan.DirectText)
        assertEquals(
            "Opening launcher settings now.",
            (plan as AssistantResponseComposer.ResponsePlan.DirectText).text
        )
    }

    private fun contextSnapshot() = AssistantContextSnapshot(
        mood = PersonalityMood.SNARKY,
        interactionCount = 0,
        state = AssistantState.IDLE,
        lastCategory = null,
        recentCategories = emptyList(),
        lastResponse = null,
        lastIntent = null,
        timestampMs = 0L,
        isSpeaking = false
    )
}

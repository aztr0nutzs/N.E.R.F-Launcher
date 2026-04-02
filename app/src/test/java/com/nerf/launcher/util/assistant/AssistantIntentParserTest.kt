package com.nerf.launcher.util.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantIntentParserTest {

    private val parser = AssistantIntentParser()

    @Test
    fun `open alone falls back to unknown`() {
        val intent = parser.parse("open")
        assertEquals(AssistantIntent.Command.UNKNOWN, intent?.command)
    }

    @Test
    fun `open settings resolves to open settings`() {
        val intent = parser.parse("open settings")
        assertEquals(AssistantIntent.Command.OPEN_SETTINGS, intent?.command)
    }

    @Test
    fun `open diagnostics resolves to diagnostics`() {
        val intent = parser.parse("open diagnostics")
        assertEquals(AssistantIntent.Command.OPEN_DIAGNOSTICS, intent?.command)
    }

    @Test
    fun `open it falls back to unknown`() {
        val intent = parser.parse("open it")
        assertEquals(AssistantIntent.Command.UNKNOWN, intent?.command)
    }

    @Test
    fun `show status resolves to status report`() {
        val intent = parser.parse("show status")
        assertEquals(AssistantIntent.Command.STATUS_REPORT, intent?.command)
    }

    @Test
    fun `scan now resolves to start local network scan`() {
        val intent = parser.parse("scan now")
        assertEquals(AssistantIntent.Command.START_LOCAL_NETWORK_SCAN, intent?.command)
    }

    @Test
    fun `scan again resolves to start local network scan`() {
        val intent = parser.parse("scan again")
        assertEquals(AssistantIntent.Command.START_LOCAL_NETWORK_SCAN, intent?.command)
    }
}

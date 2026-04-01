package com.nerf.launcher.util.assistant

class AssistantIntentParser {

    private val matchers: List<Pair<AssistantIntent.Command, List<String>>> = listOf(
        AssistantIntent.Command.OPEN_SETTINGS to listOf("open settings", "settings", "launcher settings"),
        AssistantIntent.Command.OPEN_DIAGNOSTICS to listOf("open diagnostics", "diagnostics", "reactor diagnostics"),
        AssistantIntent.Command.OPEN_NODE_HUNTER to listOf("open node hunter", "node hunter", "hunter module"),
        AssistantIntent.Command.SHOW_LOCK_SURFACE to listOf("show lock surface", "lock surface", "lock screen"),
        AssistantIntent.Command.REPORT_CURRENT_THEME to listOf("current theme", "report theme", "what theme"),
        AssistantIntent.Command.REPORT_SYSTEM_STATE to listOf(
            "battery",
            "storage",
            "uptime",
            "power save",
            "power-save",
            "system state"
        ),
        AssistantIntent.Command.REPORT_APP_FILTER_STATE to listOf(
            "app count",
            "filter count",
            "filtered apps",
            "apps loaded"
        ),
        AssistantIntent.Command.START_LOCAL_NETWORK_SCAN to listOf(
            "start local network scan",
            "start network scan",
            "scan network now"
        ),
        AssistantIntent.Command.SUMMARIZE_LOCAL_NETWORK_SCAN to listOf(
            "network scan summary",
            "summarize network",
            "scan results"
        ),
        AssistantIntent.Command.NETWORK_SCAN to listOf("scan", "network", "subnet", "ping", "wifi", "wi-fi", "lan"),
        AssistantIntent.Command.STATUS_REPORT to listOf("diagnos", "health", "status", "check", "report"),
        AssistantIntent.Command.ROUTER_CONTROL to listOf("router", "gateway", "modem", "dhcp", "firewall", "qos"),
        AssistantIntent.Command.LAUNCH to listOf("fire", "shoot", "launch", "deploy", "blast"),
        AssistantIntent.Command.RELOAD to listOf("reload", "ammo", "refill", "restock", "mag"),
        AssistantIntent.Command.TARGET_ACQUIRED to listOf("target", "aim", "lock", "acquired", "track"),
        AssistantIntent.Command.STEALTH_MODE to listOf("stealth", "quiet", "silent", "hide"),
        AssistantIntent.Command.MISSION_BRIEF to listOf("mission", "brief", "objective", "orders", "plan"),
        AssistantIntent.Command.ERROR to listOf("error", "crash", "broke", "broken", "bug", "failure"),
        AssistantIntent.Command.WARNING to listOf("warn", "caution", "alert", "danger"),
        AssistantIntent.Command.VICTORY to listOf("win", "victory", "success", "done", "nailed it"),
        AssistantIntent.Command.DEFEAT to listOf("lose", "defeat", "lost", "we failed"),
        AssistantIntent.Command.THREAT_DETECTED to listOf("threat", "enemy", "hostile", "bogey"),
        AssistantIntent.Command.TACTICAL_ANALYSIS to listOf("tactical", "analyze", "analyse", "assess", "angle", "flank"),
        AssistantIntent.Command.BATTERY_LOW to listOf("battery", "power", "charge", "low battery"),
        AssistantIntent.Command.UPDATE_AVAILABLE to listOf("update", "upgrade", "patch", "version", "new build"),
        AssistantIntent.Command.COUNTDOWN to listOf("countdown", "timer", "count", "launch sequence"),
        AssistantIntent.Command.RANDOM_SNARK to listOf("snark", "joke", "funny", "sass", "sarcasm", "roast"),
        AssistantIntent.Command.THEME_SWITCH to listOf("theme", "skin", "palette", "look", "color", "colour"),
        AssistantIntent.Command.APP_LAUNCH to listOf("open", "launch app", "start app", "module"),
        AssistantIntent.Command.PERMISSION_REQUEST to listOf("permission", "allow", "grant access"),
        AssistantIntent.Command.WAKE to listOf("hello", "hey", "yo", "wake", "reactor", "assistant"),
        AssistantIntent.Command.COMPLIMENT to listOf("good job", "nice", "thanks", "thank you")
    )

    fun parse(input: String): AssistantIntent? {
        val normalized = input.lowercase().trim()
        if (normalized.isBlank()) return null

        for ((command, tokens) in matchers) {
            val matched = tokens.firstOrNull { normalized.contains(it) }
            if (matched != null) {
                return AssistantIntent(
                    rawInput = input,
                    normalizedInput = normalized,
                    command = command,
                    matchedToken = matched,
                    tags = setOf(command.name.lowercase())
                )
            }
        }

        return AssistantIntent(
            rawInput = input,
            normalizedInput = normalized,
            command = AssistantIntent.Command.UNKNOWN
        )
    }
}

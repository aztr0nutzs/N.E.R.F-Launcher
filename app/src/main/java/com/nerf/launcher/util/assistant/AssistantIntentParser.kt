package com.nerf.launcher.util.assistant

class AssistantIntentParser {

    private data class Rule(
        val command: AssistantIntent.Command,
        val tokens: List<String>
    )

    private val rules: List<Rule> = listOf(
        Rule(AssistantIntent.Command.OPEN_SETTINGS, listOf("open settings", "launcher settings", "settings")),
        Rule(AssistantIntent.Command.OPEN_DIAGNOSTICS, listOf("open diagnostics", "reactor diagnostics", "diagnostics")),
        Rule(AssistantIntent.Command.OPEN_NODE_HUNTER, listOf("open node hunter", "hunter module", "node hunter")),
        Rule(AssistantIntent.Command.SHOW_LOCK_SURFACE, listOf("show lock surface", "lock surface", "lock screen")),
        Rule(AssistantIntent.Command.REPORT_CURRENT_THEME, listOf("current theme", "report theme", "what theme")),
        Rule(
            AssistantIntent.Command.REPORT_SYSTEM_STATE,
            listOf("system state", "power save", "power-save", "battery", "storage", "uptime")
        ),
        Rule(
            AssistantIntent.Command.REPORT_APP_FILTER_STATE,
            listOf("app count", "filter count", "app-count", "filter-count", "filtered apps", "apps loaded")
        ),
        Rule(
            AssistantIntent.Command.START_LOCAL_NETWORK_SCAN,
            listOf("start local network scan", "start network scan", "scan network now", "scan local network")
        ),
        Rule(
            AssistantIntent.Command.SUMMARIZE_LOCAL_NETWORK_SCAN,
            listOf("network scan summary", "summarize network", "scan results", "summarize local network scan")
        ),
        Rule(AssistantIntent.Command.NETWORK_SCAN, listOf("network", "subnet", "ping", "wi-fi", "wifi", "lan", "scan")),
        Rule(AssistantIntent.Command.STATUS_REPORT, listOf("status", "health", "diagnos", "check", "report")),
        Rule(AssistantIntent.Command.ROUTER_CONTROL, listOf("router", "gateway", "modem", "dhcp", "firewall", "qos")),
        Rule(AssistantIntent.Command.LAUNCH, listOf("launch", "fire", "shoot", "deploy", "blast")),
        Rule(AssistantIntent.Command.RELOAD, listOf("reload", "ammo", "refill", "restock", "mag")),
        Rule(AssistantIntent.Command.TARGET_ACQUIRED, listOf("target", "aim", "lock", "acquired", "track")),
        Rule(AssistantIntent.Command.STEALTH_MODE, listOf("stealth", "quiet", "silent", "hide")),
        Rule(AssistantIntent.Command.MISSION_BRIEF, listOf("mission", "brief", "objective", "orders", "plan")),
        Rule(AssistantIntent.Command.ERROR, listOf("error", "crash", "broken", "broke", "bug", "failure")),
        Rule(AssistantIntent.Command.WARNING, listOf("warn", "caution", "alert", "danger")),
        Rule(AssistantIntent.Command.VICTORY, listOf("victory", "win", "success", "done", "nailed it")),
        Rule(AssistantIntent.Command.DEFEAT, listOf("defeat", "lose", "lost", "we failed")),
        Rule(AssistantIntent.Command.THREAT_DETECTED, listOf("threat", "enemy", "hostile", "bogey")),
        Rule(AssistantIntent.Command.TACTICAL_ANALYSIS, listOf("tactical", "analyze", "analyse", "assess", "angle", "flank")),
        Rule(AssistantIntent.Command.BATTERY_LOW, listOf("low battery", "battery", "power", "charge")),
        Rule(AssistantIntent.Command.UPDATE_AVAILABLE, listOf("update", "upgrade", "patch", "version", "new build")),
        Rule(AssistantIntent.Command.COUNTDOWN, listOf("countdown", "launch sequence", "timer", "count")),
        Rule(AssistantIntent.Command.RANDOM_SNARK, listOf("snark", "joke", "funny", "sass", "sarcasm", "roast")),
        Rule(AssistantIntent.Command.THEME_SWITCH, listOf("cycle theme", "theme switch", "change theme", "theme", "skin", "palette", "look", "color", "colour")),
        Rule(AssistantIntent.Command.APP_LAUNCH, listOf("launch app", "start app", "open", "module")),
        Rule(AssistantIntent.Command.PERMISSION_REQUEST, listOf("permission", "allow", "grant access")),
        Rule(AssistantIntent.Command.WAKE, listOf("hello", "hey", "yo", "wake", "reactor", "assistant")),
        Rule(AssistantIntent.Command.COMPLIMENT, listOf("good job", "nice", "thanks", "thank you"))
    )

    fun parse(input: String): AssistantIntent? {
        val normalized = normalize(input)
        if (normalized.isBlank()) return null

        for (rule in rules) {
            val matched = rule.tokens.firstOrNull { token ->
                containsToken(normalized, token)
            } ?: continue

            return AssistantIntent(
                rawInput = input,
                normalizedInput = normalized,
                command = rule.command,
                matchedToken = matched,
                tags = setOf(rule.command.name.lowercase())
            )
        }

        return AssistantIntent(
            rawInput = input,
            normalizedInput = normalized,
            command = AssistantIntent.Command.UNKNOWN,
            tags = setOf(AssistantIntent.Command.UNKNOWN.name.lowercase())
        )
    }

    private fun normalize(input: String): String = input
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun containsToken(input: String, token: String): Boolean {
        if (token.contains(' ')) {
            return input.contains(token)
        }
        val pattern = Regex("\\b${Regex.escape(token)}\\b")
        return pattern.containsMatchIn(input)
    }
}

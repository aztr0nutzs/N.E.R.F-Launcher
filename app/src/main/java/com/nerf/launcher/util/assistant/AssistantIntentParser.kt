package com.nerf.launcher.util.assistant

class AssistantIntentParser {

    private data class Rule(
        val command: AssistantIntent.Command,
        val exactPhrases: Set<String> = emptySet(),
        val clueTokenGroups: List<Set<String>> = emptyList(),
        val keywordHints: Set<String> = emptySet(),
        val exclusionTokens: Set<String> = emptySet()
    )

    private val rules: List<Rule> = listOf(
        Rule(
            command = AssistantIntent.Command.OPEN_SETTINGS,
            exactPhrases = setOf("open settings", "launcher settings", "settings"),
            clueTokenGroups = listOf(setOf("open", "settings"), setOf("settings")),
            exclusionTokens = setOf("diagnostics", "reactor", "node", "hunter")
        ),
        Rule(
            command = AssistantIntent.Command.OPEN_DIAGNOSTICS,
            exactPhrases = setOf("open diagnostics", "reactor diagnostics", "diagnostics"),
            clueTokenGroups = listOf(setOf("open", "diagnostics"), setOf("reactor", "diagnostics"), setOf("diagnostics"))
        ),
        Rule(
            command = AssistantIntent.Command.OPEN_NODE_HUNTER,
            exactPhrases = setOf("open node hunter", "hunter module", "node hunter"),
            clueTokenGroups = listOf(setOf("open", "node", "hunter"), setOf("node", "hunter"), setOf("hunter", "module"))
        ),
        Rule(
            command = AssistantIntent.Command.SHOW_LOCK_SURFACE,
            exactPhrases = setOf("show lock surface", "lock surface", "lock screen"),
            clueTokenGroups = listOf(setOf("show", "lock", "surface"), setOf("lock", "surface"), setOf("lock", "screen"))
        ),
        Rule(
            command = AssistantIntent.Command.REPORT_CURRENT_THEME,
            exactPhrases = setOf("current theme", "report theme", "what theme"),
            clueTokenGroups = listOf(setOf("current", "theme"), setOf("report", "theme"), setOf("what", "theme"))
        ),
        Rule(
            command = AssistantIntent.Command.REPORT_SYSTEM_STATE,
            exactPhrases = setOf("system state", "report system state"),
            clueTokenGroups = listOf(setOf("system", "state"), setOf("power", "save")),
            keywordHints = setOf("battery", "storage", "uptime", "power-save", "power")
        ),
        Rule(
            command = AssistantIntent.Command.REPORT_APP_FILTER_STATE,
            exactPhrases = setOf("app count", "filter count", "filtered apps", "apps loaded"),
            clueTokenGroups = listOf(setOf("app", "count"), setOf("filter", "count"), setOf("apps", "loaded")),
            keywordHints = setOf("filtered", "apps")
        ),
        Rule(
            command = AssistantIntent.Command.START_LOCAL_NETWORK_SCAN,
            exactPhrases = setOf(
                "start local network scan",
                "start network scan",
                "scan network now",
                "scan local network",
                "scan now",
                "scan again"
            ),
            clueTokenGroups = listOf(
                setOf("start", "network", "scan"),
                setOf("scan", "network"),
                setOf("scan", "local", "network"),
                setOf("scan", "now"),
                setOf("scan", "again")
            )
        ),
        Rule(
            command = AssistantIntent.Command.SUMMARIZE_LOCAL_NETWORK_SCAN,
            exactPhrases = setOf("network scan summary", "summarize network", "scan results", "summarize local network scan"),
            clueTokenGroups = listOf(setOf("network", "scan", "summary"), setOf("scan", "results"), setOf("summarize", "network"))
        ),
        Rule(
            command = AssistantIntent.Command.NETWORK_SCAN,
            clueTokenGroups = listOf(setOf("network", "scan"), setOf("local", "network")),
            keywordHints = setOf("network", "subnet", "ping", "wi-fi", "wifi", "lan", "scan"),
            exclusionTokens = setOf("status")
        ),
        Rule(
            command = AssistantIntent.Command.STATUS_REPORT,
            exactPhrases = setOf("show status", "status report", "system status"),
            clueTokenGroups = listOf(setOf("show", "status"), setOf("status", "report"), setOf("system", "status")),
            keywordHints = setOf("status", "health", "check", "report", "diagnos")
        ),
        Rule(
            command = AssistantIntent.Command.ROUTER_CONTROL,
            keywordHints = setOf("router", "gateway", "modem", "dhcp", "firewall", "qos")
        ),
        Rule(command = AssistantIntent.Command.LAUNCH, keywordHints = setOf("launch", "fire", "shoot", "deploy", "blast")),
        Rule(command = AssistantIntent.Command.RELOAD, keywordHints = setOf("reload", "ammo", "refill", "restock", "mag")),
        Rule(command = AssistantIntent.Command.TARGET_ACQUIRED, keywordHints = setOf("target", "aim", "lock", "acquired", "track")),
        Rule(command = AssistantIntent.Command.STEALTH_MODE, keywordHints = setOf("stealth", "quiet", "silent", "hide")),
        Rule(command = AssistantIntent.Command.MISSION_BRIEF, keywordHints = setOf("mission", "brief", "objective", "orders", "plan")),
        Rule(command = AssistantIntent.Command.ERROR, keywordHints = setOf("error", "crash", "broken", "broke", "bug", "failure")),
        Rule(command = AssistantIntent.Command.WARNING, keywordHints = setOf("warn", "caution", "alert", "danger")),
        Rule(command = AssistantIntent.Command.VICTORY, keywordHints = setOf("victory", "win", "success", "done", "nailed")),
        Rule(command = AssistantIntent.Command.DEFEAT, keywordHints = setOf("defeat", "lose", "lost", "failed")),
        Rule(command = AssistantIntent.Command.THREAT_DETECTED, keywordHints = setOf("threat", "enemy", "hostile", "bogey")),
        Rule(command = AssistantIntent.Command.TACTICAL_ANALYSIS, keywordHints = setOf("tactical", "analyze", "analyse", "assess", "angle", "flank")),
        Rule(command = AssistantIntent.Command.BATTERY_LOW, keywordHints = setOf("low", "battery", "power", "charge")),
        Rule(command = AssistantIntent.Command.UPDATE_AVAILABLE, keywordHints = setOf("update", "upgrade", "patch", "version", "new", "build")),
        Rule(command = AssistantIntent.Command.COUNTDOWN, keywordHints = setOf("countdown", "launch", "sequence", "timer", "count")),
        Rule(command = AssistantIntent.Command.RANDOM_SNARK, keywordHints = setOf("snark", "joke", "funny", "sass", "sarcasm", "roast")),
        Rule(
            command = AssistantIntent.Command.THEME_SWITCH,
            exactPhrases = setOf("cycle theme", "theme switch", "change theme"),
            clueTokenGroups = listOf(setOf("cycle", "theme"), setOf("change", "theme"), setOf("theme", "switch")),
            keywordHints = setOf("theme", "skin", "palette", "look", "color", "colour")
        ),
        Rule(
            command = AssistantIntent.Command.APP_LAUNCH,
            exactPhrases = setOf("launch app", "start app", "open app"),
            clueTokenGroups = listOf(setOf("launch", "app"), setOf("start", "app"), setOf("open", "app")),
            keywordHints = setOf("module"),
            exclusionTokens = setOf("settings", "diagnostics", "status")
        ),
        Rule(command = AssistantIntent.Command.PERMISSION_REQUEST, keywordHints = setOf("permission", "allow", "grant", "access")),
        Rule(command = AssistantIntent.Command.WAKE, keywordHints = setOf("hello", "hey", "yo", "wake", "reactor", "assistant")),
        Rule(command = AssistantIntent.Command.COMPLIMENT, keywordHints = setOf("good", "job", "nice", "thanks", "thank", "you"))
    )

    fun parse(input: String): AssistantIntent? {
        val normalized = normalize(input)
        if (normalized.isBlank()) return null

        val tokens = tokenize(normalized)
        val candidates = rules.mapNotNull { rule ->
            val score = scoreRule(normalized, tokens, rule)
            if (score > 0) ScoredRule(rule, score) else null
        }

        val best = candidates.maxByOrNull { it.score }
        if (best == null || best.score < MIN_CONFIDENCE_SCORE || hasAmbiguousWinner(best, candidates)) {
            return AssistantIntent(
                rawInput = input,
                normalizedInput = normalized,
                command = AssistantIntent.Command.UNKNOWN,
                tags = setOf(AssistantIntent.Command.UNKNOWN.name.lowercase())
            )
        }

        return AssistantIntent(
            rawInput = input,
            normalizedInput = normalized,
            command = best.rule.command,
            matchedToken = findMatchedToken(normalized, tokens, best.rule),
            tags = setOf(best.rule.command.name.lowercase())
        )
    }

    private fun scoreRule(input: String, tokens: Set<String>, rule: Rule): Int {
        var score = 0

        if (rule.exactPhrases.any { it == input }) {
            score += EXACT_MATCH_BOOST
        } else if (rule.exactPhrases.any { phrase -> input.contains(phrase) }) {
            score += PHRASE_MATCH_BOOST
        }

        score += rule.clueTokenGroups.sumOf { group ->
            if (group.all { token -> token in tokens }) MULTI_TOKEN_CLUE_BOOST else 0
        }

        score += rule.keywordHints.count { hint -> hint in tokens || input.contains(hint) } * KEYWORD_HINT_BOOST

        score -= rule.exclusionTokens.count { exclusion -> exclusion in tokens || input.contains(exclusion) } * EXCLUSION_PENALTY

        return score
    }

    private fun findMatchedToken(input: String, tokens: Set<String>, rule: Rule): String? {
        return rule.exactPhrases.firstOrNull { phrase -> input.contains(phrase) }
            ?: rule.clueTokenGroups.firstOrNull { group -> group.all(tokens::contains) }?.joinToString(" ")
            ?: rule.keywordHints.firstOrNull { hint -> hint in tokens || input.contains(hint) }
    }

    private fun hasAmbiguousWinner(best: ScoredRule, candidates: List<ScoredRule>): Boolean {
        val secondBest = candidates
            .filterNot { it === best }
            .maxByOrNull { it.score }
            ?: return false
        return best.score - secondBest.score <= AMBIGUITY_DELTA
    }

    private fun normalize(input: String): String = input
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun tokenize(input: String): Set<String> = input
        .split(Regex("[^a-z0-9]+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()

    private data class ScoredRule(val rule: Rule, val score: Int)

    private companion object {
        const val EXACT_MATCH_BOOST = 14
        const val PHRASE_MATCH_BOOST = 8
        const val MULTI_TOKEN_CLUE_BOOST = 4
        const val KEYWORD_HINT_BOOST = 2
        const val EXCLUSION_PENALTY = 5
        const val MIN_CONFIDENCE_SCORE = 4
        const val AMBIGUITY_DELTA = 1
    }
}

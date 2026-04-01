package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

data class AssistantIntent(
    val rawInput: String,
    val normalizedInput: String,
    val command: Command,
    val matchedToken: String? = null,
    val tags: Set<String> = emptySet()
) {
    enum class Command(val category: Category?) {
        NETWORK_SCAN(Category.NETWORK_SCAN),
        STATUS_REPORT(Category.STATUS_REPORT),
        ROUTER_CONTROL(Category.ROUTER_CONTROL),
        LAUNCH(Category.LAUNCH),
        RELOAD(Category.RELOAD),
        TARGET_ACQUIRED(Category.TARGET_ACQUIRED),
        STEALTH_MODE(Category.STEALTH_MODE),
        MISSION_BRIEF(Category.MISSION_BRIEF),
        ERROR(Category.ERROR),
        WARNING(Category.WARNING),
        VICTORY(Category.VICTORY),
        DEFEAT(Category.DEFEAT),
        THREAT_DETECTED(Category.THREAT_DETECTED),
        TACTICAL_ANALYSIS(Category.TACTICAL_ANALYSIS),
        BATTERY_LOW(Category.BATTERY_LOW),
        UPDATE_AVAILABLE(Category.UPDATE_AVAILABLE),
        COUNTDOWN(Category.COUNTDOWN),
        RANDOM_SNARK(Category.RANDOM_SNARK),
        THEME_SWITCH(Category.THEME_SWITCH),
        APP_LAUNCH(Category.APP_LAUNCH),
        PERMISSION_REQUEST(Category.PERMISSION_REQUEST),
        WAKE(Category.WAKE),
        COMPLIMENT(Category.COMPLIMENT),
        UNKNOWN(Category.UNKNOWN_COMMAND)
    }
}

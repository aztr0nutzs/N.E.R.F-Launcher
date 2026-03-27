package com.nerf.launcher.util

/**
 * Repository of predefined Nerf themes.
 */
object ThemeRepository {
    /** Classic Nerf – orange primary, blue accent, dark window background. */
    val CLASSIC_NERF = NerfTheme(
        name = "Classic Nerf",
        primary = 0xFF6A00FF,   // #FF6A00
        secondary = 0xFF00F0FF, // #00F0FF
        accent = 0xFFFFEA00,    // #FFEA00
        windowBackground = 0xFF212121FF // #212121
    )

    /** Stealth Ops – dark primary, red accent, dark window background. */
    val STEALTH_OPS = NerfTheme(
        name = "Stealth Ops",
        primary = 0xFF212121FF, // #212121
        secondary = 0xFFFF0000FF, // #FF0000
        accent = 0xFFFF4500FF,  // #FF4500 (OrangeRed)
        windowBackground = 0xFF212121FF // #212121
    )

    /** Elite Blue – blue primary, cyan accent, dark window background. */
    val ELITE_BLUE = NerfTheme(
        name = "Elite Blue",
        primary = 0xFF00B0FFFF, // #00B0FF
        secondary = 0xFF00F0FFFF, // #00F0FF
        accent = 0xFFFFFF00FF,  // #FFFF00 (Yellow)
        windowBackground = 0xFF212121FF // #212121
    )

    /** Zombie Strike – green accent on dark. */
    val ZOMBIE_STRIKE = NerfTheme(
        name = "Zombie Strike",
        primary = 0xFF212121FF, // #212121
        secondary = 0xFF00BF00FF, // #00BF00
        accent = 0xFF00FF00FF,  // #00FF00
        windowBackground = 0xFF212121FF // #212121
    )

    /** Hyper Neon – vivid magenta primary, cyan secondary, dark window background. */
    val HYPER_NEON = NerfTheme(
        name = "Hyper Neon",
        primary = 0xFFFF00FFFF, // #FF00FF
        secondary = 0xFF00FFFFFFFF, // #00FFFF
        accent = 0xFFFFFF00FFFF, // #FFFF00
        windowBackground = 0xFF212121FF // #212121
    )

    /** Returns all predefined themes as a list. */
    val all: List<NerfTheme>
        get() = listOf(CLASSIC_NERF, STEALTH_OPS, ELITE_BLUE, ZOMBIE_STRIKE, HYPER_NEON)

    /** Find a theme by name (case‑insensitive). */
    fun byName(name: String): NerfTheme? =
        all.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
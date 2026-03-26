package com.nerf.launcher.util

import android.graphics.Color

/**
 * Repository of predefined Nerf themes.
 */
object ThemeRepository {
    /** Classic Nerf – orange primary, cyan secondary, yellow accent, dark background. */
    val CLASSIC_NERF = NerfTheme(
        name = "Classic Nerf",
        primary = Color.parseColor("#FF6A00"),
        secondary = Color.parseColor("#00F0FF"),
        accent = Color.parseColor("#FFEA00"),
        windowBackground = Color.parseColor("#212121")
    )

    /** Stealth Ops – dark primary, red accent, dark window background. */
    val STEALTH_OPS = NerfTheme(
        name = "Stealth Ops",
        primary = Color.parseColor("#212121"),
        secondary = Color.parseColor("#FF0000"),
        accent = Color.parseColor("#FF4500"), // OrangeRed
        windowBackground = Color.parseColor("#212121")
    )

    /** Elite Blue – blue primary, cyan accent, dark window background. */
    val ELITE_BLUE = NerfTheme(
        name = "Elite Blue",
        primary = Color.parseColor("#00B0FF"),
        secondary = Color.parseColor("#00F0FF"),
        accent = Color.parseColor("#FFFF00"), // Yellow
        windowBackground = Color.parseColor("#212121")
    )

    /** Zombie Strike – green accent on dark. */
    val ZOMBIE_STRIKE = NerfTheme(
        name = "Zombie Strike",
        primary = Color.parseColor("#212121"),
        secondary = Color.parseColor("#00BF00"),
        accent = Color.parseColor("#00FF00"),
        windowBackground = Color.parseColor("#212121")
    )

    /** Hyper Neon – vivid magenta primary, cyan secondary, dark window background. */
    val HYPER_NEON = NerfTheme(
        name = "Hyper Neon",
        primary = Color.parseColor("#FF00FF"),
        secondary = Color.parseColor("#00FFFF"),
        accent = Color.parseColor("#FFFF00"),
        windowBackground = Color.parseColor("#212121")
    )

    /** Returns all predefined themes as a list. */
    val all: List<NerfTheme>
        get() = listOf(CLASSIC_NERF, STEALTH_OPS, ELITE_BLUE, ZOMBIE_STRIKE, HYPER_NEON)

    /** Find a theme by name (case‑insensitive). */
    fun byName(name: String): NerfTheme? =
        all.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
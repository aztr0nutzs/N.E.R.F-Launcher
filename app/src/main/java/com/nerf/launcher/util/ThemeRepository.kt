package com.nerf.launcher.util

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.nerf.launcher.R

/**
 * Repository of predefined Nerf themes.
 */
object ThemeRepository {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Classic Nerf â€“ orange primary, cyan secondary, yellow accent, dark background. */
    val CLASSIC_NERF: NerfTheme
        get() = theme(
            name = "Classic Nerf",
            primaryResId = R.color.nerf_primary,
            secondaryResId = R.color.nerf_secondary,
            accentResId = R.color.nerf_accent,
            windowBackgroundResId = R.color.nerf_background
        )

    /** Stealth Ops â€“ dark primary, red accent, dark window background. */
    val STEALTH_OPS: NerfTheme
        get() = theme(
            name = "Stealth Ops",
            primaryResId = R.color.nerf_background,
            secondaryResId = R.color.theme_stealth_ops_secondary,
            accentResId = R.color.theme_stealth_ops_accent,
            windowBackgroundResId = R.color.nerf_background
        )

    /** Elite Blue â€“ blue primary, cyan accent, dark window background. */
    val ELITE_BLUE: NerfTheme
        get() = theme(
            name = "Elite Blue",
            primaryResId = R.color.theme_elite_blue_primary,
            secondaryResId = R.color.nerf_secondary,
            accentResId = R.color.theme_elite_blue_accent,
            windowBackgroundResId = R.color.nerf_background
        )

    /** Zombie Strike â€“ green accent on dark. */
    val ZOMBIE_STRIKE: NerfTheme
        get() = theme(
            name = "Zombie Strike",
            primaryResId = R.color.nerf_background,
            secondaryResId = R.color.theme_zombie_strike_secondary,
            accentResId = R.color.theme_zombie_strike_accent,
            windowBackgroundResId = R.color.nerf_background
        )

    /** Hyper Neon â€“ vivid magenta primary, cyan secondary, dark window background. */
    val HYPER_NEON: NerfTheme
        get() = theme(
            name = "Hyper Neon",
            primaryResId = R.color.theme_hyper_neon_primary,
            secondaryResId = R.color.theme_hyper_neon_secondary,
            accentResId = R.color.theme_hyper_neon_accent,
            windowBackgroundResId = R.color.nerf_background
        )

    /** Returns all predefined themes as a list. */
    val all: List<NerfTheme>
        get() = listOf(CLASSIC_NERF, STEALTH_OPS, ELITE_BLUE, ZOMBIE_STRIKE, HYPER_NEON)

    /** Find a theme by name (caseâ€‘insensitive). */
    fun byName(name: String): NerfTheme? =
        all.firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun theme(
        name: String,
        @ColorRes primaryResId: Int,
        @ColorRes secondaryResId: Int,
        @ColorRes accentResId: Int,
        @ColorRes windowBackgroundResId: Int
    ): NerfTheme {
        val context = requireContext()
        return NerfTheme(
            name = name,
            primary = ContextCompat.getColor(context, primaryResId),
            secondary = ContextCompat.getColor(context, secondaryResId),
            accent = ContextCompat.getColor(context, accentResId),
            windowBackground = ContextCompat.getColor(context, windowBackgroundResId)
        )
    }

    private fun requireContext(): Context {
        check(::appContext.isInitialized) {
            "ThemeRepository not initialized. Call ThemeRepository.init() from Application.onCreate()."
        }
        return appContext
    }
}

package com.nerf.launcher.util

import android.content.Context

/**
 * Manages available icon packs and the currently selected pack.
 */
object IconPackManager {
    const val DEFAULT_PACK = "system"
    private const val NERF_PACK = "nerf"
    private const val MINIMAL_PACK = "minimal"

    /** Returns the list of available icon pack identifiers. */
    fun getAvailablePacks(): List<String> = listOf(DEFAULT_PACK, NERF_PACK, MINIMAL_PACK)

    /** Returns the currently selected pack name, defaulting to system icons if none set. */
    fun getCurrentPack(context: Context): String =
        PreferencesManager.getIconPack(context) ?: DEFAULT_PACK

    /** Saves the selected pack name if it is valid. */
    fun setCurrentPack(context: Context, packName: String) {
        if (getAvailablePacks().contains(packName)) {
            PreferencesManager.saveIconPack(context, packName)
        }
    }
}

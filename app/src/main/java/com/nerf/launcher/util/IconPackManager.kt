package com.nerf.launcher.util

import android.content.Context
import java.io.IOException

/**
 * Manages available icon packs and the currently selected pack.
 */
object IconPackManager {
    const val DEFAULT_PACK = "system"
    private const val ICON_PACK_ASSET_ROOT = "icon_packs"

    /** Returns the list of available icon pack identifiers. */
    fun getAvailablePacks(context: Context): List<String> {
        val packsFromAssets = try {
            context.assets
                .list(ICON_PACK_ASSET_ROOT)
                ?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.sorted()
                ?.toList()
                .orEmpty()
        } catch (_: IOException) {
            emptyList()
        }
        return buildList {
            add(DEFAULT_PACK)
            addAll(packsFromAssets.filterNot { it == DEFAULT_PACK })
        }
    }

    /** Returns the currently selected pack name, defaulting to system icons if none set. */
    fun getCurrentPack(context: Context): String {
        val selectedPack = ConfigRepository.get().config.value?.iconPack ?: DEFAULT_PACK
        val availablePacks = getAvailablePacks(context)
        return if (selectedPack in availablePacks) {
            selectedPack
        } else {
            DEFAULT_PACK
        }
    }

    /** Saves the selected pack name if it is valid. */
    fun setCurrentPack(context: Context, packName: String) {
        if (getAvailablePacks(context).contains(packName)) {
            ConfigRepository.get().updateIconPack(packName)
        }
    }
}

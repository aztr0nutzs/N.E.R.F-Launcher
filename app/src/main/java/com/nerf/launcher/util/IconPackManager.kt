package com.nerf.launcher.util

import android.content.Context
import java.io.IOException

/**
 * Manages available icon packs and the currently selected pack.
 */
object IconPackManager {
    const val DEFAULT_PACK = "system"
    const val ICON_PACK_ASSET_ROOT = "icon_packs"
    private val ICON_EXTENSIONS = listOf(".png", ".webp", ".jpg", ".jpeg")
    @Volatile
    private var cachedAvailablePacks: List<String>? = null

    /** Returns the list of available icon pack identifiers. */
    fun getAvailablePacks(context: Context): List<String> {
        cachedAvailablePacks?.let { return it }
        val packsFromAssets = try {
            context.assets
                .list(ICON_PACK_ASSET_ROOT)
                ?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.filter { packName -> packName == DEFAULT_PACK || hasIcons(context, packName) }
                ?.sorted()
                ?.toList()
                .orEmpty()
        } catch (_: IOException) {
            emptyList()
        }
        return buildList {
            add(DEFAULT_PACK)
            addAll(packsFromAssets.filterNot { it == DEFAULT_PACK })
        }.also { resolved ->
            cachedAvailablePacks = resolved
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

    fun hasAdditionalPackAssets(context: Context): Boolean {
        return getAvailablePacks(context).any { it != DEFAULT_PACK }
    }

    fun clearAvailabilityCache() {
        cachedAvailablePacks = null
    }

    private fun hasIcons(context: Context, packName: String): Boolean {
        val files = try {
            context.assets.list("$ICON_PACK_ASSET_ROOT/$packName").orEmpty()
        } catch (_: IOException) {
            return false
        }
        return files.any { fileName ->
            val normalized = fileName.lowercase()
            ICON_EXTENSIONS.any { normalized.endsWith(it) }
        }
    }
}

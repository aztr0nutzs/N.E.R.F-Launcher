package com.nerf.launcher.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.nerf.launcher.R
import java.io.File
import java.io.IOException

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Provides the appropriate icon for a given package name based on the selected icon pack.
 * Implements a clean fallback chain:
 *   1. Custom icon file override (if exists)
 *   2. Selected icon pack asset (if exists)
 *   3. System default icon via PackageManager
 *   4. Ultimate placeholder (gray square)
 * Results are cached in IconCache to avoid repeated lookups during scrolling.
 */
class IconProvider(
    private val context: Context,
    private val iconCache: IconCache
) {
    companion object {
        private const val ICON_PACK_ASSET_ROOT = "icon_packs"
        private const val CUSTOM_ICON_DIR = "custom_icons"
    }

    private val packageManager: PackageManager = context.packageManager
    private val assets: AssetManager = context.assets
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Loads an icon asynchronously and sets it onto the target ImageView.
     * Checks the cache first, falling back to background loading if needed.
     */
    fun loadIconInto(packageName: String, imageView: android.widget.ImageView) {
        val selectedPack = IconPackManager.getCurrentPack(context)
        val cacheKey = "$selectedPack:$packageName"
        val cached = iconCache.get(cacheKey)
        if (cached != null) {
            imageView.setImageDrawable(cached)
            return
        }

        // Apply placeholder and tag mapping
        imageView.setImageDrawable(null)
        imageView.tag = packageName

        scope.launch {
            val icon = resolveIcon(packageName, selectedPack)
            iconCache.put(cacheKey, icon)

            withContext(Dispatchers.Main) {
                if (imageView.tag == packageName && IconPackManager.getCurrentPack(context) == selectedPack) {
                    imageView.setImageDrawable(icon)
                }
            }
        }
    }

    private fun resolveIcon(packageName: String, selectedPack: String): Drawable {
        return loadCustomIcon(packageName)
            ?: loadPackIcon(packageName, selectedPack)
            ?: loadSystemIcon(packageName)
    }

    private fun loadCustomIcon(packageName: String): Drawable? {
        val customIconFile = File(context.filesDir, "$CUSTOM_ICON_DIR/$packageName.png")
        if (!customIconFile.exists() || !customIconFile.isFile) {
            return null
        }

        return try {
            customIconFile.inputStream().use { stream ->
                Drawable.createFromStream(stream, null)
            }
        } catch (e: IOException) {
            null
        }
    }

    /** Attempts to load an icon from the currently selected icon pack assets. */
    private fun loadPackIcon(packageName: String, selectedPack: String): Drawable? {
        if (selectedPack == IconPackManager.DEFAULT_PACK) {
            return null
        }

        val assetPath = "$ICON_PACK_ASSET_ROOT/$selectedPack/$packageName.png"
        return try {
            assets.open(assetPath).use { stream ->
                Drawable.createFromStream(stream, null)
            }
        } catch (_: IOException) {
            null
        }
    }

    /** Retrieves the default system icon for a package, with an ultimate fallback. */
    private fun loadSystemIcon(packageName: String): Drawable {
        return try {
            val info: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            info.loadIcon(packageManager)
        } catch (e: PackageManager.NameNotFoundException) {
            // If the package cannot be found, use a generic placeholder.
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_module
            ) ?: ColorDrawable(0xFFCCCCCC.toInt())
        }
    }

    /** Clears the internal icon cache. */
    fun evictCache() {
        iconCache.evictAll()
    }
}

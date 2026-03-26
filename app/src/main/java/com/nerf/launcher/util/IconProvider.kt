package com.nerf.launcher.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream

/**
 * Provides the appropriate icon for a given package name based on the selected icon pack.
 * Implements a clean fallback chain:
 *   1. Custom pack asset (if exists)
 *   2. System default icon via PackageManager
 *   3. Ultimate placeholder (gray square)
 * Results are cached in IconCache to avoid repeated lookups during scrolling.
 */
class IconProvider(
    private val context: Context,
    private val iconCache: IconCache
) {
    private val packageManager: PackageManager = context.packageManager
    private val assets: AssetManager = context.assets

    /**
     * Returns a Drawable representing the icon for the supplied package name.
     * The result is cached to avoid repeated asset lookups during scrolling.
     */
    fun getIcon(packageName: String): Drawable {
        val cacheKey = "${IconPackManager.getCurrentPack(context)}:$packageName"
        return iconCache.get(cacheKey) ?: loadIconFromPack(packageName).also { icon ->
            if (icon != null) {
                iconCache.put(cacheKey, icon)
            }
        }
    }

    /** Attempts to load an icon from the currently selected icon pack assets. */
    private fun loadIconFromPack(packageName: String): Drawable? {
        val packName = IconPackManager.getCurrentPack(context)
        if (packName == "system") {
            // System pack – defer to system icon loader.
            return loadSystemIcon(packageName)
        }
        // Custom pack: try to find <pack>/<package>.png in the assets folder.
        val assetPath = "icon_packs/$packName/$packageName.png"
        return try {
            val inputStream: InputStream = assets.open(assetPath)
            val drawable: Drawable = Drawable.createFromStream(inputStream, null)
            inputStream.close()
            drawable
        } catch (e: IOException) {
            // Not found in custom pack – fall back to system icon.
            return loadSystemIcon(packageName)
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
                android.R.drawable.sym_def_app_icon
            ) ?: ColorDrawable(0xFFCCCCCC.toInt())
        }
    }

    /** Clears the internal icon cache. */
    fun evictCache() {
        iconCache.evictAll()
    }
}
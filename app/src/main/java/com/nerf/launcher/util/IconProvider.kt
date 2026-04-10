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
import java.util.WeakHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Provides the appropriate icon for a given package name based on the selected icon pack.
 * Implements a clean fallback chain:
 *   1. Custom icon file override (if exists)
 *   2. Selected icon pack asset (if exists)
 *   3. System default icon via PackageManager
 *   4. Generic module fallback drawable (with a gray square as the final safeguard)
 * Results are cached in IconCache to avoid repeated lookups during scrolling.
 */
class IconProvider(
    private val context: Context,
    private val iconCache: IconCache
) {
    companion object {
        private const val CUSTOM_ICON_DIR = "custom_icons"
        private val SUPPORTED_IMAGE_EXTENSIONS = listOf("png", "webp", "jpg", "jpeg")
    }

    private val packageManager: PackageManager = context.packageManager
    private val assets: AssetManager = context.assets
    private val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlightLoads = mutableMapOf<String, kotlinx.coroutines.Deferred<Drawable>>()
    private val inFlightLock = Mutex()
    private val latestRequestByView = WeakHashMap<android.widget.ImageView, String>()
    private val latestRequestLock = Any()

    /**
     * Loads an icon asynchronously and sets it onto the target ImageView.
     * Checks the cache first, falling back to background loading if needed.
     */
    fun loadIconInto(packageName: String, imageView: android.widget.ImageView) {
        val selectedPack = IconPackManager.getCurrentPack(context)
        val cacheKey = "$selectedPack:$packageName"
        val currentRequest = synchronized(latestRequestLock) { latestRequestByView[imageView] }
        if (currentRequest == cacheKey) {
            iconCache.get(cacheKey)?.let { cachedDrawable ->
                if (imageView.drawable !== cachedDrawable) {
                    imageView.setImageDrawable(cachedDrawable)
                }
            }
            return
        }
        synchronized(latestRequestLock) {
            latestRequestByView[imageView] = cacheKey
        }
        val cached = iconCache.get(cacheKey)
        if (cached != null) {
            imageView.setImageDrawable(cached)
            return
        }

        scope.launch {
            val icon = getOrLoadIcon(cacheKey, packageName, selectedPack)
            iconCache.put(cacheKey, icon)

            withContext(Dispatchers.Main) {
                val latestRequest = synchronized(latestRequestLock) { latestRequestByView[imageView] }
                if (latestRequest == cacheKey && IconPackManager.getCurrentPack(context) == selectedPack) {
                    imageView.setImageDrawable(icon)
                }
            }
        }
    }

    /**
     * Suspend version for Compose consumers — routes through the same cache and
     * deduplication path as [loadIconInto]. Call from a [kotlinx.coroutines.Dispatchers.IO]
     * context (e.g. via [androidx.compose.runtime.produceState]).
     *
     * @param packageName The app package name to load an icon for.
     * @param iconPack    The currently active icon pack name (read from [IconPackManager]).
     */
    suspend fun loadIconSuspend(packageName: String, iconPack: String): Drawable {
        val cacheKey = "$iconPack:$packageName"
        return getOrLoadIcon(cacheKey, packageName, iconPack)
    }

    private suspend fun getOrLoadIcon(
        cacheKey: String,
        packageName: String,
        selectedPack: String
    ): Drawable {
        val cached = iconCache.get(cacheKey)
        if (cached != null) return cached

        val deferred = inFlightLock.withLock {
            inFlightLoads[cacheKey] ?: scope.async {
                resolveIcon(packageName, selectedPack)
            }.also { created ->
                inFlightLoads[cacheKey] = created
            }
        }

        return try {
            deferred.await()
        } catch (e: CancellationException) {
            throw e
        } finally {
            inFlightLock.withLock {
                if (inFlightLoads[cacheKey] === deferred) {
                    inFlightLoads.remove(cacheKey)
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
        for (extension in SUPPORTED_IMAGE_EXTENSIONS) {
            val customIconFile = File(context.filesDir, "$CUSTOM_ICON_DIR/$packageName.$extension")
            if (!customIconFile.exists() || !customIconFile.isFile) {
                continue
            }
            val drawable = try {
                customIconFile.inputStream().use { stream ->
                    Drawable.createFromStream(stream, null)
                }
            } catch (_: IOException) {
                null
            }
            if (drawable != null) return drawable
        }
        return null
    }

    /** Attempts to load an icon from the currently selected icon pack assets. */
    private fun loadPackIcon(packageName: String, selectedPack: String): Drawable? {
        if (selectedPack == IconPackManager.DEFAULT_PACK) {
            return null
        }

        for (extension in SUPPORTED_IMAGE_EXTENSIONS) {
            val assetPath = "${IconPackManager.ICON_PACK_ASSET_ROOT}/$selectedPack/$packageName.$extension"
            val drawable = try {
                assets.open(assetPath).use { stream ->
                    Drawable.createFromStream(stream, null)
                }
            } catch (_: IOException) {
                null
            }
            if (drawable != null) return drawable
        }
        return null
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
    fun evictCache(packName: String? = null) {
        if (packName.isNullOrBlank()) {
            iconCache.evictAll()
            return
        }
        iconCache.evictPack(packName)
    }

    fun clearInFlightLoads() {
        scope.coroutineContext.cancelChildren()
        inFlightLoads.clear()
    }

    fun release() {
        clearInFlightLoads()
        scope.coroutineContext.cancelChildren()
    }
}

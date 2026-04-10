package com.nerf.launcher.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.nerf.launcher.util.IconCache
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.IconProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
//  AppIconLoader
//
//  Compose-native icon loading for the AppDrawerScreen.
//  Uses the same IconProvider + IconCache infrastructure as the legacy
//  AppAdapter so icon pack selection and caching are shared.
//
//  ComposeIconCache is a process-scoped singleton so the cache persists
//  across recompositions and across drawer open/close cycles.
// ─────────────────────────────────────────────────────────────────────────────

private object ComposeIconCache {
    // 120-entry LRU — enough for a full screen of apps with some scroll headroom.
    val cache: IconCache = IconCache(maxSize = 120)
}

/**
 * Returns an [ImageBitmap] for the given [packageName] and [iconPack], or null
 * while the icon is still loading. Shows the system default icon if no pack
 * icon is available.
 *
 * The result is cached in [ComposeIconCache] so repeated calls for the same
 * key return immediately without re-loading.
 *
 * @param packageName The package name of the app to load an icon for.
 * @param iconPack    The currently active icon pack name (from AppConfig.iconPack).
 */
@Composable
fun rememberAppIcon(packageName: String, iconPack: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext

    return produceState<ImageBitmap?>(
        initialValue = null,
        key1 = packageName,
        key2 = iconPack
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                val provider = IconProvider(context, ComposeIconCache.cache)
                val drawable: Drawable = provider.loadIconSuspend(packageName, iconPack)
                // toBitmap() converts any Drawable (BitmapDrawable, VectorDrawable,
                // AdaptiveIconDrawable, etc.) to a Bitmap suitable for Compose.
                drawable.toBitmap().asImageBitmap()
            } catch (_: Exception) {
                null  // Leave the glyph fallback shown on any error.
            }
        }
    }.value
}

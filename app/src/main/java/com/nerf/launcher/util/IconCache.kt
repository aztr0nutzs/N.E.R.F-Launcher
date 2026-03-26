package com.nerf.launcher.util

import android.graphics.drawable.Drawable
import androidx.collection.LruCache

/**
 * Simple LRU cache for Drawables.
 * Key format: "<iconPack>:<packageName>"
 */
class IconCache(private val maxSize: Int) {
    private val cache: LruCache<String, Drawable> = LruCache(maxSize)

    /** Retrieve a cached drawable, or null if absent. */
    fun get(key: String): Drawable? = cache.get(key)

    /** Store a drawable in the cache. */
    fun put(key: String, value: Drawable) {
        cache.put(key, value)
    }

    /** Remove all cached entries. */
    fun evictAll() {
        cache.evictAll()
    }
}
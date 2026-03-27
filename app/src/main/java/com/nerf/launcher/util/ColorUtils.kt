package com.nerf.launcher.util

import android.graphics.Color

object ColorUtils {

    /** 
     * Calculate whether a color is light based on luminance.
     * Useful for determining icon tint or text visibility over colored backgrounds. 
     */
    fun isColorLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return luminance > 0.5
    }
}

package com.nerf.launcher.model

import android.graphics.drawable.Drawable

/**
 * Represents an installable application with its label, package, class name and icon.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val className: String,
    val icon: Drawable
)
package com.nerf.launcher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val className: String,
    val icon: Drawable
)
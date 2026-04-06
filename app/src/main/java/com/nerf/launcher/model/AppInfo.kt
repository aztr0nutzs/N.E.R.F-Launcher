package com.nerf.launcher.model

import java.util.Locale

data class AppInfo(
    val appName: String,
    val packageName: String,
    val className: String,
    val normalizedAppName: String = appName.lowercase(Locale.getDefault()),
    val normalizedPackageName: String = packageName.lowercase(Locale.getDefault())
)

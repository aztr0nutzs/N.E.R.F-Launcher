package com.nerf.launcher.util

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.nerf.launcher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUtils {

    suspend fun loadInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
        val apps = mutableListOf<AppInfo>()
        for (info in resolveInfos.sortedWith(ResolveInfo.DisplayNameComparator(pm))) {
            val activityInfo = info.activityInfo
            val label = activityInfo.loadLabel(pm).toString()
            val packageName = activityInfo.packageName
            val className = activityInfo.name
            apps.add(
                AppInfo(
                    appName = label,
                    packageName = packageName,
                    className = className
                )
            )
        }
        apps
    }

    fun launchApp(context: Context, appInfo: AppInfo) {
        val intent = Intent().apply {
            setClassName(appInfo.packageName, appInfo.className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            // App was uninstalled or updated between drawer load and tap — silently no-op.
        } catch (_: IllegalArgumentException) {
            // Malformed package/class — silently no-op.
        }
    }

    fun openDefaultHomeSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

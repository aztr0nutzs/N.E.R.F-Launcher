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

    /** Launch an app by its full AppInfo (uses class name for precise targeting). */
    fun launchApp(context: Context, appInfo: AppInfo) {
        val intent = Intent().apply {
            setClassName(appInfo.packageName, appInfo.className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Launch an app by package name using the launcher intent. */
    fun launchApp(context: Context, packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent != null) {
            context.startActivity(intent)
        }
    }

    fun openDefaultHomeSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

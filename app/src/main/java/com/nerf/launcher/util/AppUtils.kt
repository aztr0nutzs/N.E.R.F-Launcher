package com.nerf.launcher.util

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.nerf.launcher.model.AppInfo

object AppUtils {

    fun loadInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
        val apps = mutableListOf<AppInfo>()
        for (info in resolveInfos.sortedWith(ResolveInfo.DisplayNameComparator(pm))) {
            val activityInfo = info.activityInfo
            val label = activityInfo.loadLabel(pm).toString()
            val icon = activityInfo.loadIcon(pm)
            val packageName = activityInfo.packageName
            val className = activityInfo.name
            apps.add(
                AppInfo(
                    appName = label,
                    packageName = packageName,
                    className = className,
                    icon = icon
                )
            )
        }
        return apps
    }

    fun launchApp(context: Context, appInfo: AppInfo) {
        val intent = Intent().apply {
            setClassName(appInfo.packageName, appInfo.className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openDefaultHomeSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
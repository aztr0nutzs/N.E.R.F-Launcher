package com.nerf.launcher

import android.app.Application
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.ThemeRepository

/**
 * Application class to initialize singleton repositories.
 */
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeRepository.init(this)
        ConfigRepository.init(this)
    }
}

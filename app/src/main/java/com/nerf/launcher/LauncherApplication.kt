package com.nerf.launcher

import android.app.Application
import com.nerf.launcher.util.ConfigRepository

/**
 * Application class to initialize the ConfigRepository singleton.
 */
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize the ConfigRepository singleton with the application context
        ConfigRepository.init(this)
    }
}
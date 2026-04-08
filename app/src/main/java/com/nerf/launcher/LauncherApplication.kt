package com.nerf.launcher

import android.app.Application
import android.util.Log
import com.nerf.launcher.util.ConfigRepository

/**
 * Application class to initialize the ConfigRepository singleton.
 */
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("LauncherApplication", "onCreate: initializing ConfigRepository")
        // Initialize the ConfigRepository singleton with the application context
        ConfigRepository.init(this)
        Log.d("LauncherApplication", "onCreate: ConfigRepository initialized")
    }
}

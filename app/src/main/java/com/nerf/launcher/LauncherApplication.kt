package com.nerf.launcher

import android.app.Application
import com.nerf.launcher.util.ConfigRepository

/**
 * Application class. Initializes process-scoped singletons before any
 * Activity or Service is created.
 *
 * [ConfigRepository] must be initialized here so that it is ready before
 * [MainActivity] creates [LauncherViewModel], which calls [ConfigRepository.get]
 * in its [init] block.
 */
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ConfigRepository.init(this)
    }
}

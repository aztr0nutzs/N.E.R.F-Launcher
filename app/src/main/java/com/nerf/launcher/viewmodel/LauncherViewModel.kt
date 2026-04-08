package com.nerf.launcher.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nerf.launcher.BuildConfig
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Supplies the UI with a LiveData list of installed apps, loaded off‑the‑main thread via coroutines.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "LauncherViewModel"
    }

    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps
    private var activeLoadJob: Job? = null
    private var reloadRequested: Boolean = false

    /** Triggers an asynchronous load of all launchable apps. */
    fun loadApps() {
        if (activeLoadJob?.isActive == true) {
            reloadRequested = true
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "loadApps requested while active; scheduling reload")
            }
            return
        }

        activeLoadJob = viewModelScope.launch(Dispatchers.IO) {
            do {
                reloadRequested = false
                val appList = AppUtils.loadInstalledApps(getApplication<Application>())
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "loadApps completed count=${appList.size}")
                }
                _apps.postValue(appList)
            } while (reloadRequested)
        }.also { job ->
            job.invokeOnCompletion {
                activeLoadJob = null
            }
        }
    }
}

package com.nerf.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.util.AppUtils
import kotlinx.coroutines.launch

/**
 * Supplies the UI with a LiveData list of installed apps, loaded off‑the‑main thread via coroutines.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps

    /** Triggers an asynchronous load of all launchable apps. */
    fun loadApps() {
        viewModelScope.launch {
            val appList = AppUtils.loadInstalledApps(getApplication<Application>())
            _apps.postValue(appList)
        }
    }
}
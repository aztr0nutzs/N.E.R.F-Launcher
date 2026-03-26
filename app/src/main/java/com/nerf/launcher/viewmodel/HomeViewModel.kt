package com.nerf.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            // Perform the blocking PackageManager query safely off the main thread
            val list = AppUtils.loadInstalledApps(context)
            
            // Post results natively on the Main thread as requested
            withContext(Dispatchers.Main) {
                _apps.value = list
            }
        }
    }
}
package com.nerf.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.util.AppUtils
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps

    fun loadApps() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val list = AppUtils.loadInstalledApps(context)
            _apps.value = list
        }
    }
}

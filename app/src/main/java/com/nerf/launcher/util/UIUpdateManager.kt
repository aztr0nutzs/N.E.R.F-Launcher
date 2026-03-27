package com.nerf.launcher.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nerf.launcher.util.NerfTheme

object UIUpdateManager {
    private val _theme = MutableLiveData<NerfTheme>()
    val theme: LiveData<NerfTheme> = _theme

    private val _iconPack = MutableLiveData<String>()
    val iconPack: LiveData<String> = _iconPack

    private val _gridSpan = MutableLiveData<Int>()
    val gridSpan: LiveData<Int> = _gridSpan

    fun setTheme(theme: NerfTheme) {
        _theme.value = theme
    }
    fun setIconPack(pack: String) {
        _iconPack.value = pack
    }
    fun setGridSpan(span: Int) {
        _gridSpan.value = span
    }
}
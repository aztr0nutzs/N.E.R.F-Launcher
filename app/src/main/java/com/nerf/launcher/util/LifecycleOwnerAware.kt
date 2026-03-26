package com.nerf.launcher.util

import androidx.lifecycle.LifecycleOwner

/**
 * Interface for views that need to bind to a LifecycleOwner in order to
 * safely observe LiveData without leaking observers.
 */
interface LifecycleOwnerAware {
    fun setLifecycleOwner(owner: LifecycleOwner)
}

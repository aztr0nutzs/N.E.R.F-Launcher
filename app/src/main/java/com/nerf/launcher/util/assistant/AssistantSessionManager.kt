package com.nerf.launcher.util.assistant

import android.content.Context

object AssistantSessionManager {

    private data class SharedSession(
        val controller: AssistantController,
        var owners: Int
    )

    private var sharedSession: SharedSession? = null

    @Synchronized
    fun acquire(context: Context): AssistantController {
        sharedSession?.let {
            it.owners += 1
            return it.controller
        }

        val controller = AssistantController(context.applicationContext)
        sharedSession = SharedSession(controller = controller, owners = 1)
        return controller
    }

    @Synchronized
    fun release(controller: AssistantController) {
        val current = sharedSession ?: return
        if (current.controller !== controller) return

        current.owners -= 1
        if (current.owners <= 0) {
            controller.dispose()
            sharedSession = null
        }
    }
}

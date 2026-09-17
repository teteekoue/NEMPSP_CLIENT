package com.example.nempsp.server

import android.content.Context
import com.example.nempsp.model.PspButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class NemPspServerEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val status: StateFlow<ServerStatus> = NemPspForegroundServerService.serverState

    fun startServer(port: Int = 8989) {
        NemPspForegroundServerService.startService(context)
    }

    fun stopServer() {
        NemPspForegroundServerService.stopService(context)
    }

    fun injectSimulatedInput(button: PspButton, isPressed: Boolean) {
        NemPspForegroundServerService.injectSimulatedInput(button, isPressed)
    }
}

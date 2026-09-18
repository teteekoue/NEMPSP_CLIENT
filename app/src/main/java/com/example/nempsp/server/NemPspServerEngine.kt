package com.example.nempsp.server

import android.content.Context
import com.example.nempsp.model.PspButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Façade du récepteur utilisée par l'interface.
 *
 * Le port demandé est maintenant réellement transmis au service (il était auparavant ignoré).
 */
class NemPspServerEngine(
    private val context: Context,
    @Suppress("unused") private val scope: CoroutineScope
) {
    val status: StateFlow<ServerStatus> = NemPspForegroundServerService.serverState

    fun startServer(port: Int = NemPspForegroundServerService.DEFAULT_PORT) {
        NemPspForegroundServerService.startService(context, port)
    }

    fun stopServer() {
        NemPspForegroundServerService.stopService(context)
    }

    fun injectSimulatedInput(button: PspButton, isPressed: Boolean) {
        NemPspForegroundServerService.injectSimulatedInput(button, isPressed)
    }
}

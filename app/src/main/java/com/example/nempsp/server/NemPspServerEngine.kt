package com.example.nempsp.server

import android.content.Context
import com.example.nempsp.injection.InjectionSupport
import com.example.nempsp.injection.NemPspInjectionService
import com.example.nempsp.model.PspButton
import com.example.nempsp.repository.InjectionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Façade du récepteur utilisée par l'interface.
 *
 * Le port demandé est maintenant réellement transmis au service (il était auparavant ignoré).
 * Elle expose aussi l'injection dans PPSSPP (service d'accessibilité) pour que l'écran Récepteur
 * puisse l'activer, la régler et la tester sans connaître les détails d'implémentation.
 */
class NemPspServerEngine(
    private val context: Context,
    @Suppress("unused") private val scope: CoroutineScope
) {
    val status: StateFlow<ServerStatus> = NemPspForegroundServerService.serverState

    /** Le service d'accessibilité est lié et sa boucle d'injection tourne. */
    val injectionRunning: StateFlow<Boolean> = NemPspInjectionService.isRunning

    /** PPSSPP est au premier plan : l'injection est effectivement armée. */
    val injectionGameFocused: StateFlow<Boolean> = NemPspInjectionService.gameFocused

    /** Dernier événement d'injection, affiché tel quel à l'utilisateur. */
    val injectionMessage: StateFlow<String> = NemPspInjectionService.lastMessage

    /** Nombre de gestes terminés avec succès depuis le démarrage du service. */
    val injectedGestures: StateFlow<Long> = NemPspInjectionService.injectedGestures

    fun startServer(port: Int = NemPspForegroundServerService.DEFAULT_PORT) {
        NemPspForegroundServerService.startService(context, port)
    }

    fun stopServer() {
        NemPspForegroundServerService.stopService(context)
    }

    fun injectSimulatedInput(button: PspButton, isPressed: Boolean) {
        NemPspForegroundServerService.injectSimulatedInput(button, isPressed)
    }

    // ------------------------------------------------------------------
    // Injection dans PPSSPP
    // ------------------------------------------------------------------

    fun injectionPreferences(): InjectionPreferences = InjectionPreferences(context)

    /** Le service figure-t-il dans les réglages d'accessibilité de l'appareil ? */
    fun isInjectionServiceEnabled(): Boolean = InjectionSupport.isServiceEnabled(context)

    fun openAccessibilitySettings() = InjectionSupport.openAccessibilitySettings(context)

    /**
     * Appui de test injecté dans PPSSPP. Retourne un message lisible : le service peut refuser
     * (non activé, PPSSPP en arrière-plan) et l'utilisateur doit savoir pourquoi.
     */
    fun runInjectionSelfTest(): String =
        NemPspInjectionService.instance?.selfTest()
            ?: "Le service d'injection n'est pas démarré : activez « NEMPSP — Manette pour PPSSPP » dans les réglages d'accessibilité."
}

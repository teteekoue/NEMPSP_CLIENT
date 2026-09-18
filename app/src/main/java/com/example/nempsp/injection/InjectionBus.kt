package com.example.nempsp.injection

import com.example.nempsp.model.PspButton
import com.example.nempsp.server.ServerDecodedPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * État instantané de la manette, tel que reçu par le récepteur.
 *
 * Pas d'horodatage volontairement : [InjectionBus] est un `StateFlow`, donc deux trames
 * identiques ne déclenchent pas de recomposition ni de geste inutile.
 */
data class InjectionFrame(
    val buttons: List<PspButton> = emptyList(),
    val analogX: Float = 0f,
    val analogY: Float = 0f,
    val sequence: Int = 0
) {
    companion object {
        val IDLE = InjectionFrame()
    }
}

/**
 * Passerelle entre le service récepteur (qui décode les paquets) et le service d'accessibilité
 * (qui injecte les appuis dans PPSSPP).
 *
 * Les deux vivent dans le même processus mais ce sont des composants Android distincts, sans
 * lien de cycle de vie : un singleton `StateFlow` évite tout `bindService` et permet à
 * l'injection de survivre à un redémarrage de l'interface.
 */
object InjectionBus {

    private val _frame = MutableStateFlow(InjectionFrame.IDLE)
    val frame: StateFlow<InjectionFrame> = _frame.asStateFlow()

    fun publish(decoded: ServerDecodedPacket) {
        _frame.value = InjectionFrame(
            buttons = decoded.activeButtons,
            analogX = decoded.analogX,
            analogY = decoded.analogY,
            sequence = decoded.sequenceNumber
        )
    }

    /** Remet la manette au repos (arrêt du récepteur, perte du client, PPSSPP quitté). */
    fun reset() {
        _frame.value = InjectionFrame.IDLE
    }
}

package com.example.nempsp.server

import com.example.nempsp.model.PspButton

/**
 * État publié par le service récepteur et observé par l'interface.
 *
 * Nouveauté importante : l'état ne ment plus. Auparavant `isRunning` passait à `true` AVANT
 * même que les sockets soient ouverts : si le port était occupé ou l'ouverture impossible,
 * l'écran affichait « EN LIGNE (8989) » alors que rien n'écoutait — exactement le cas où le
 * téléphone reçoit « port inaccessible » en UDP.
 */
data class ServerStatus(
    val isRunning: Boolean = false,
    val udpPort: Int = 8989,
    val tcpPort: Int = 8989,
    val localIpAddress: String = "127.0.0.1",
    /** Toutes les adresses IPv4 candidates (un Chromebook en expose souvent plusieurs). */
    val allIpAddresses: List<String> = emptyList(),
    val udpListening: Boolean = false,
    val tcpListening: Boolean = false,
    val btListening: Boolean = false,
    /** Raison lisible du dernier échec d'ouverture, `null` si tout va bien. */
    val startupError: String? = null,
    val activeConnectionsCount: Int = 0,
    val lastClientSource: String = "Aucun",
    val packetsReceived: Long = 0,
    val packetsPerSecond: Int = 0,
    val lastDecodedState: ServerDecodedPacket? = null,
    val activeButtons: List<PspButton> = emptyList(),
    val analogX: Float = 0.0f,
    val analogY: Float = 0.0f
) {
    /** Au moins un transport écoute réellement : un client peut se connecter. */
    val readyForClient: Boolean
        get() = isRunning && (udpListening || tcpListening || btListening)
}

package com.example.nempsp.network

import java.net.PortUnreachableException
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Protocole binaire NEMPSP v2 — identique sur tous les transports (UDP, TCP/USB, Bluetooth RFCOMM).
 *
 * Deux types de trames cohabitent :
 *
 * 1. TRAME MANETTE (9 octets) — inchangée, 100 % compatible avec l'ancien protocole :
 *    [0] 0x4E 'N' | [1] 0x4D 'M' | [2..3] séquence LE16 | [4..5] masque boutons LE16
 *    [6] analog X (0..255, 128 = centre) | [7] analog Y | [8] XOR des octets 0..7
 *
 * 2. TRAME DE CONTRÔLE (8 octets) — handshake et mesure de latence réelle :
 *    [0] 0x4E 'N' | [1] 0x50 'P' | [2] type | [3..6] charge utile LE32 | [7] XOR des octets 0..6
 *
 * Les trames de contrôle remplacent les « ping » sauvages (octet 0x00, chaînes ASCII) qui
 * polluaient le flux et faussaient le compteur de connexions du récepteur.
 */
object NemPspProtocol {

    /** Premier octet commun aux deux types de trames ('N'). */
    const val MAGIC_FIRST: Int = 0x4E

    /** Second octet d'une trame manette ('M'). */
    const val MAGIC_GAMEPAD: Int = 0x4D

    /** Second octet d'une trame de contrôle ('P'). */
    const val MAGIC_CONTROL: Int = 0x50

    const val GAMEPAD_FRAME_LENGTH: Int = 9
    const val CONTROL_FRAME_LENGTH: Int = 8

    const val CTRL_HELLO: Int = 1
    const val CTRL_WELCOME: Int = 2
    const val CTRL_PING: Int = 3
    const val CTRL_PONG: Int = 4

    const val PROTOCOL_VERSION: Int = 2

    const val DISCOVERY_REQUEST: String = "NEMPSP_DISCOVERY_REQUEST"
    const val DISCOVERY_REPLY_PREFIX: String = "NEMPSP_SERVER"

    /** XOR sur la plage [from, until[ — utilisé par les deux types de trames. */
    fun xor(data: ByteArray, from: Int, until: Int): Byte {
        var checksum = 0
        for (i in from until until) {
            checksum = checksum xor (data[i].toInt() and 0xFF)
        }
        return checksum.toByte()
    }

    fun controlFrame(type: Int, payload: Int): ByteArray {
        val frame = ByteArray(CONTROL_FRAME_LENGTH)
        frame[0] = MAGIC_FIRST.toByte()
        frame[1] = MAGIC_CONTROL.toByte()
        frame[2] = (type and 0xFF).toByte()
        frame[3] = (payload and 0xFF).toByte()
        frame[4] = ((payload shr 8) and 0xFF).toByte()
        frame[5] = ((payload shr 16) and 0xFF).toByte()
        frame[6] = ((payload shr 24) and 0xFF).toByte()
        frame[7] = xor(frame, 0, CONTROL_FRAME_LENGTH - 1)
        return frame
    }

    fun controlType(frame: ByteArray): Int = frame[2].toInt() and 0xFF

    fun controlPayload(frame: ByteArray): Int =
        (frame[3].toInt() and 0xFF) or
            ((frame[4].toInt() and 0xFF) shl 8) or
            ((frame[5].toInt() and 0xFF) shl 16) or
            ((frame[6].toInt() and 0xFF) shl 24)

    fun isControlFrame(frame: ByteArray): Boolean =
        frame.size >= CONTROL_FRAME_LENGTH &&
            (frame[0].toInt() and 0xFF) == MAGIC_FIRST &&
            (frame[1].toInt() and 0xFF) == MAGIC_CONTROL &&
            frame[CONTROL_FRAME_LENGTH - 1] == xor(frame, 0, CONTROL_FRAME_LENGTH - 1)

    fun helloFrame(): ByteArray = controlFrame(CTRL_HELLO, PROTOCOL_VERSION)
    fun welcomeFrame(): ByteArray = controlFrame(CTRL_WELCOME, PROTOCOL_VERSION)
    fun pingFrame(nonce: Int): ByteArray = controlFrame(CTRL_PING, nonce)
    fun pongFrame(nonce: Int): ByteArray = controlFrame(CTRL_PONG, nonce)

    /**
     * Traduit une exception réseau en message compréhensible pour le joueur.
     *
     * Le bug historique : `e.message` vaut `null` pour une [PortUnreachableException], ce qui
     * affichait « Erreur envoi paquet UDP: null » dans le journal — aucune information utile.
     */
    fun describeSendError(error: Throwable, target: String): String = when (error) {
        is PortUnreachableException ->
            "Aucun récepteur NEMPSP n'écoute sur $target : le périphérique a répondu « port inaccessible ». " +
                "Démarrez le Mode Récepteur sur l'appareil de jeu, ou relancez un scan réseau (l'IP a pu changer)."
        is SocketTimeoutException -> "Délai dépassé en envoyant vers $target."
        is SocketException ->
            "Liaison réseau interrompue vers $target (${error.message ?: "socket fermé"}). " +
                "WiFi coupé, IP modifiée ou récepteur arrêté."
        is SecurityException -> "Envoi refusé par Android : permission réseau manquante."
        is IllegalArgumentException ->
            "Adresse ou port invalide pour $target (${error.message ?: "argument incorrect"})."
        else -> "${error.javaClass.simpleName}${error.message?.let { " : $it" } ?: ""}"
    }

    /** Libellé court pour une exception (journal technique). */
    fun errorName(error: Throwable): String =
        error.javaClass.simpleName + (error.message?.let { " : $it" } ?: "")
}

/**
 * Découpeur de flux pour les transports orientés octets (TCP/USB et Bluetooth RFCOMM).
 *
 * Il se resynchronise proprement sur l'octet 'N' et ne perd plus un début de trame
 * (l'ancien code jetait un 'N' suivi d'un octet inattendu au lieu de le réexaminer).
 */
class NemPspStreamFramer(
    private val onGamepadFrame: (ByteArray) -> Unit,
    private val onControlFrame: (type: Int, payload: Int) -> Unit
) {
    private val buffer = ByteArray(NemPspProtocol.GAMEPAD_FRAME_LENGTH)
    private var index = 0
    private var expectedLength = 0

    fun push(data: ByteArray, length: Int) {
        var i = 0
        while (i < length) {
            val b = data[i].toInt() and 0xFF
            i++
            when (index) {
                0 -> if (b == NemPspProtocol.MAGIC_FIRST) {
                    buffer[0] = b.toByte()
                    index = 1
                }
                1 -> when (b) {
                    NemPspProtocol.MAGIC_GAMEPAD -> {
                        buffer[1] = b.toByte()
                        expectedLength = NemPspProtocol.GAMEPAD_FRAME_LENGTH
                        index = 2
                    }
                    NemPspProtocol.MAGIC_CONTROL -> {
                        buffer[1] = b.toByte()
                        expectedLength = NemPspProtocol.CONTROL_FRAME_LENGTH
                        index = 2
                    }
                    else -> {
                        // Mauvais second octet : on repart de zéro, mais cet octet peut être
                        // lui-même un début de trame ('N'), donc on le rejoue au lieu de le jeter.
                        index = 0
                        if (b == NemPspProtocol.MAGIC_FIRST) {
                            buffer[0] = b.toByte()
                            index = 1
                        }
                    }
                }
                else -> {
                    buffer[index] = b.toByte()
                    index++
                    if (index >= expectedLength) {
                        val frame = buffer.copyOf(expectedLength)
                        index = 0
                        expectedLength = 0
                        dispatch(frame)
                    }
                }
            }
        }
    }

    private fun dispatch(frame: ByteArray) {
        if ((frame[1].toInt() and 0xFF) == NemPspProtocol.MAGIC_CONTROL) {
            if (NemPspProtocol.isControlFrame(frame)) {
                onControlFrame(NemPspProtocol.controlType(frame), NemPspProtocol.controlPayload(frame))
            }
        } else {
            onGamepadFrame(frame)
        }
    }
}

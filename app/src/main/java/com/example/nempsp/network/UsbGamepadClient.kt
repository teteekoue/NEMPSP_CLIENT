package com.example.nempsp.network

import android.os.SystemClock
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.ConnectionStatus
import com.example.nempsp.model.GamepadState
import com.example.nempsp.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Client USB (tunnel `adb reverse` → 127.0.0.1:port, ou TCP direct).
 *
 * Corrections :
 * - Plus d'octet parasite `0x00` injecté dans le flux de paquets toutes les 1,5 s, ni de
 *   handshake ASCII mélangé au protocole binaire : on utilise les trames de contrôle du
 *   protocole v2 ([NemPspProtocol]).
 * - La latence est un vrai aller-retour (PING → PONG) au lieu du temps d'écriture local,
 *   qui affichait toujours « 1 ms » même quand le récepteur était mort.
 * - Déconnexion détectée réellement grâce à un lecteur d'entrée dédié.
 */
class UsbGamepadClient(private val scope: CoroutineScope) {

    companion object {
        const val DEFAULT_PORT: Int = 8989
        private const val CONNECT_TIMEOUT_MS: Int = 2500
        private const val PING_INTERVAL_MS: Long = 1000
        private const val CONNECTION_LOST_AFTER_MS: Long = 5000
    }

    private var usbSocket: Socket? = null
    private var outputStream: OutputStream? = null

    @Volatile
    private var targetPort: Int = DEFAULT_PORT

    @Volatile
    private var lastPongAt: Long = 0L

    @Volatile
    private var pingStartedAt: Long = 0L

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _statusDetail = MutableStateFlow("Aucune connexion USB en cours")
    val statusDetail: StateFlow<String> = _statusDetail.asStateFlow()

    private val _latencyMs = MutableStateFlow(-1L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private var readerJob: Job? = null
    private var pingJob: Job? = null

    fun connect(port: Int = DEFAULT_PORT) {
        if (port !in 1..65535) {
            _status.value = ConnectionStatus.ERROR
            _statusDetail.value = "Port invalide : « $port »"
            LogRepository.error("USB", _statusDetail.value, ConnectionMode.USB_ADB)
            return
        }
        targetPort = port
        _status.value = ConnectionStatus.CONNECTING
        _statusDetail.value = "Connexion au tunnel USB 127.0.0.1:$port..."
        _latencyMs.value = -1L

        scope.launch(Dispatchers.IO) {
            closeQuietly()
            try {
                val socket = Socket()
                socket.tcpNoDelay = true // Désactive l'algorithme de Nagle : latence minimale
                socket.connect(InetSocketAddress("127.0.0.1", targetPort), CONNECT_TIMEOUT_MS)
                usbSocket = socket
                outputStream = socket.getOutputStream()
                lastPongAt = 0L

                startReader(socket, socket.getInputStream())
                startPingLoop()

                LogRepository.info(
                    "USB",
                    "Socket TCP ouvert sur 127.0.0.1:$targetPort — attente de la réponse du récepteur...",
                    ConnectionMode.USB_ADB
                )
            } catch (e: Exception) {
                _status.value = ConnectionStatus.ERROR
                _statusDetail.value =
                    "Connexion USB impossible (${NemPspProtocol.errorName(e)}). " +
                        "Branchez le câble, activez le débogage USB, puis lancez sur l'appareil de jeu : " +
                        "adb reverse tcp:$targetPort tcp:$targetPort"
                LogRepository.error("USB", _statusDetail.value, ConnectionMode.USB_ADB)
            }
        }
    }

    fun disconnect() {
        closeQuietly()
        _status.value = ConnectionStatus.DISCONNECTED
        _statusDetail.value = "Tunnel USB fermé"
        _latencyMs.value = -1L
        LogRepository.info("USB", "Déconnexion du tunnel USB", ConnectionMode.USB_ADB)
    }

    fun sendGamepadState(state: GamepadState): Boolean {
        val stream = outputStream ?: return false
        val socket = usbSocket ?: return false
        if (socket.isClosed || !socket.isConnected) return false

        return try {
            stream.write(state.toBinaryPacket())
            stream.flush()
            true
        } catch (e: Exception) {
            _status.value = ConnectionStatus.ERROR
            _statusDetail.value = "Tunnel USB rompu : ${NemPspProtocol.errorName(e)}"
            LogRepository.error("USB", _statusDetail.value, ConnectionMode.USB_ADB)
            disconnect()
            false
        }
    }

    private fun startReader(socket: Socket, input: InputStream) {
        readerJob?.cancel()
        readerJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(256)
            val framer = NemPspStreamFramer(
                onGamepadFrame = { /* Le récepteur n'émet pas de trames manette */ },
                onControlFrame = { type, payload -> onControl(type, payload) }
            )
            try {
                while (isActive && !socket.isClosed) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) framer.push(buffer, read)
                }
            } catch (_: Exception) {
                // Fermeture normale du tunnel
            }
            if (_status.value == ConnectionStatus.CONNECTED || _status.value == ConnectionStatus.CONNECTING) {
                _status.value = ConnectionStatus.ERROR
                _statusDetail.value = "Le récepteur a fermé le tunnel USB. Relancez adb reverse puis reconnectez."
                LogRepository.warn("USB", _statusDetail.value, ConnectionMode.USB_ADB)
            }
            closeQuietly()
        }
    }

    private fun onControl(type: Int, payload: Int) {
        when (type) {
            NemPspProtocol.CTRL_PONG -> {
                val now = SystemClock.elapsedRealtime()
                val rtt = (now.toInt() - payload).coerceAtLeast(0)
                _latencyMs.value = rtt.toLong()
                lastPongAt = now
                if (_status.value != ConnectionStatus.CONNECTED) {
                    _status.value = ConnectionStatus.CONNECTED
                    _statusDetail.value = "Connecté au récepteur via USB (aller-retour ${rtt}ms)"
                    LogRepository.success("USB", "CONNEXION USB ÉTABLIE — aller-retour ${rtt}ms", ConnectionMode.USB_ADB)
                }
            }
            NemPspProtocol.CTRL_WELCOME -> {
                LogRepository.success(
                    "USB",
                    "Récepteur NEMPSP identifié (protocole v$payload)",
                    ConnectionMode.USB_ADB
                )
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingStartedAt = SystemClock.elapsedRealtime()
        lastPongAt = 0L
        pingJob = scope.launch(Dispatchers.IO) {
            var helloSent = false
            while (isActive) {
                val stream = outputStream
                val socket = usbSocket
                if (stream == null || socket == null || socket.isClosed) break

                try {
                    if (!helloSent) {
                        stream.write(NemPspProtocol.helloFrame())
                        helloSent = true
                    }
                    stream.write(NemPspProtocol.pingFrame(SystemClock.elapsedRealtime().toInt()))
                    stream.flush()
                } catch (e: Exception) {
                    _status.value = ConnectionStatus.ERROR
                    _statusDetail.value = "Tunnel USB rompu : ${NemPspProtocol.errorName(e)}"
                    LogRepository.error("USB", _statusDetail.value, ConnectionMode.USB_ADB)
                    break
                }

                val now = SystemClock.elapsedRealtime()
                if (lastPongAt == 0L && now - pingStartedAt > 4000 && _status.value != ConnectionStatus.CONNECTED) {
                    _status.value = ConnectionStatus.ERROR
                    _statusDetail.value =
                        "Le tunnel est ouvert mais le récepteur ne répond pas. " +
                            "Vérifiez que le Mode Récepteur est démarré sur l'appareil de jeu."
                    LogRepository.error("USB", _statusDetail.value, ConnectionMode.USB_ADB)
                } else if (lastPongAt != 0L && now - lastPongAt > CONNECTION_LOST_AFTER_MS) {
                    _status.value = ConnectionStatus.ERROR
                    _statusDetail.value = "Le récepteur USB ne répond plus. Relancez adb reverse."
                    _latencyMs.value = -1L
                    LogRepository.warn("USB", _statusDetail.value, ConnectionMode.USB_ADB)
                }

                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun closeQuietly() {
        pingJob?.cancel()
        readerJob?.cancel()
        pingJob = null
        readerJob = null
        try {
            outputStream?.close()
        } catch (_: Exception) {
        }
        try {
            usbSocket?.close()
        } catch (_: Exception) {
        }
        outputStream = null
        usbSocket = null
    }
}

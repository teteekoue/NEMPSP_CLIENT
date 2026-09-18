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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Client WiFi / UDP.
 *
 * Corrections majeures par rapport à la version précédente :
 * - Le statut « CONNECTÉ » n'est plus déclaré à la simple création du socket : il faut une
 *   vraie réponse (PONG) du récepteur. Fini la pastille verte alors que rien n'écoute.
 * - La latence est un véritable aller-retour UDP (PING/PONG) et non plus un handshake TCP
 *   toutes les 2 s, qui spammait le journal du récepteur et faussait son compteur de clients.
 * - Les erreurs d'envoi sont traduites en clair (la « PortUnreachableException » au message
 *   null affichait « Erreur envoi paquet UDP: null » 60 fois par seconde).
 * - Les erreurs sont journalisées au maximum une fois toutes les 2 s (plus de saturation).
 * - Reconnexion automatique : le ping continue tant que le socket existe, le statut repasse
 *   à CONNECTÉ dès que le récepteur répond de nouveau.
 */
class WifiUdpClient(private val scope: CoroutineScope) {

    companion object {
        const val DEFAULT_PORT: Int = 8989
        private const val RECEIVE_TIMEOUT_MS: Int = 400
        private const val PING_INTERVAL_CONNECTING_MS: Long = 350
        private const val PING_INTERVAL_CONNECTED_MS: Long = 1000
        private const val NO_REPLY_ERROR_AFTER_MS: Long = 4000
        private const val CONNECTION_LOST_AFTER_MS: Long = 5000
        private const val ERROR_LOG_THROTTLE_MS: Long = 2000
    }

    private var udpSocket: DatagramSocket? = null
    private var serverAddress: InetAddress? = null

    @Volatile
    private var targetIp: String = ""

    @Volatile
    private var targetPort: Int = DEFAULT_PORT

    @Volatile
    private var lastPongAt: Long = 0L

    @Volatile
    private var startedAt: Long = 0L

    @Volatile
    private var lastErrorLogAt: Long = 0L

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    /** Explication lisible de l'état courant, affichée dans l'interface. */
    private val _statusDetail = MutableStateFlow("Aucune connexion WiFi en cours")
    val statusDetail: StateFlow<String> = _statusDetail.asStateFlow()

    private val _latencyMs = MutableStateFlow(-1L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _discoveredServers = MutableStateFlow<List<String>>(emptyList())
    val discoveredServers: StateFlow<List<String>> = _discoveredServers.asStateFlow()

    private var receiveJob: Job? = null
    private var pingJob: Job? = null
    private var discoveryJob: Job? = null

    private val target: String
        get() = "$targetIp:$targetPort"

    fun connect(ip: String, port: Int = DEFAULT_PORT) {
        val cleanIp = ip.trim()
        if (!isUsableAddress(cleanIp)) {
            _status.value = ConnectionStatus.ERROR
            _statusDetail.value = "Adresse IP invalide : « $cleanIp ». Format attendu : 192.168.1.45"
            LogRepository.error("WIFI", _statusDetail.value, ConnectionMode.WIFI_UDP)
            return
        }
        if (port !in 1..65535) {
            _status.value = ConnectionStatus.ERROR
            _statusDetail.value = "Port invalide : « $port ». Valeur par défaut : $DEFAULT_PORT"
            LogRepository.error("WIFI", _statusDetail.value, ConnectionMode.WIFI_UDP)
            return
        }

        targetIp = cleanIp
        targetPort = port
        _status.value = ConnectionStatus.CONNECTING
        _statusDetail.value = "Ouverture du socket UDP vers $target..."
        _latencyMs.value = -1L
        lastPongAt = 0L

        scope.launch(Dispatchers.IO) {
            closeSocketQuietly()
            try {
                val socket = DatagramSocket()
                socket.soTimeout = RECEIVE_TIMEOUT_MS
                udpSocket = socket
                serverAddress = InetAddress.getByName(cleanIp)
                startedAt = SystemClock.elapsedRealtime()

                startReceiver(socket)
                startPingLoop()

                LogRepository.info(
                    "WIFI",
                    "Socket UDP ouvert (port local ${socket.localPort}) vers $target — attente d'une réponse du récepteur...",
                    ConnectionMode.WIFI_UDP
                )
            } catch (e: Exception) {
                _status.value = ConnectionStatus.ERROR
                _statusDetail.value = "Impossible d'ouvrir le socket UDP : ${NemPspProtocol.errorName(e)}"
                LogRepository.error("WIFI", _statusDetail.value, ConnectionMode.WIFI_UDP)
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        receiveJob?.cancel()
        discoveryJob?.cancel()
        pingJob = null
        receiveJob = null
        closeSocketQuietly()
        serverAddress = null
        lastPongAt = 0L
        _status.value = ConnectionStatus.DISCONNECTED
        _statusDetail.value = "Déconnecté du récepteur WiFi"
        _latencyMs.value = -1L
        LogRepository.info("WIFI", "Déconnexion WiFi effectuée", ConnectionMode.WIFI_UDP)
    }

    fun sendGamepadState(state: GamepadState): Boolean {
        val socket = udpSocket ?: return false
        val address = serverAddress ?: return false
        if (socket.isClosed) return false

        return try {
            val bytes = state.toBinaryPacket()
            socket.send(DatagramPacket(bytes, bytes.size, address, targetPort))
            true
        } catch (e: Exception) {
            handleSendFailure(e)
            false
        }
    }

    /**
     * Recherche automatique du récepteur sur le réseau local (broadcast UDP).
     * Le récepteur répond « NEMPSP_SERVER:... » ; on remplit [discoveredServers].
     */
    fun startAutoDiscovery() {
        discoveryJob?.cancel()
        _discoveredServers.value = emptyList()
        _statusDetail.value = "Recherche d'un récepteur NEMPSP sur le réseau local..."
        discoveryJob = scope.launch(Dispatchers.IO) {
            LogRepository.info(
                "WIFI",
                "Scan réseau : broadcast « ${NemPspProtocol.DISCOVERY_REQUEST} » sur 255.255.255.255:$DEFAULT_PORT",
                ConnectionMode.WIFI_UDP
            )
            var broadcastSocket: DatagramSocket? = null
            try {
                broadcastSocket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 1200
                }

                val payload = NemPspProtocol.DISCOVERY_REQUEST.toByteArray(Charsets.UTF_8)
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                broadcastSocket.send(DatagramPacket(payload, payload.size, broadcastAddr, DEFAULT_PORT))

                val buffer = ByteArray(512)
                val received = DatagramPacket(buffer, buffer.size)
                val found = mutableListOf<String>()
                val deadline = SystemClock.elapsedRealtime() + 5000

                while (isActive && SystemClock.elapsedRealtime() < deadline) {
                    try {
                        broadcastSocket.receive(received)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val text = String(received.data, 0, received.length, Charsets.UTF_8)
                    val senderIp = received.address.hostAddress ?: continue
                    if (text.startsWith(NemPspProtocol.DISCOVERY_REPLY_PREFIX) && !found.contains(senderIp)) {
                        found.add(senderIp)
                        _discoveredServers.value = found.toList()
                        _statusDetail.value = "Récepteur détecté : $senderIp — touchez-le pour vous connecter"
                        LogRepository.success("WIFI", "Récepteur NEMPSP détecté à $senderIp ($text)", ConnectionMode.WIFI_UDP)
                    }
                }

                if (found.isEmpty()) {
                    _statusDetail.value =
                        "Aucun récepteur trouvé par le scan. Saisissez l'IP affichée sur l'appareil de jeu, " +
                            "et vérifiez que les deux appareils sont sur le même WiFi (sans « isolation client »)."
                    LogRepository.warn("WIFI", _statusDetail.value, ConnectionMode.WIFI_UDP)
                }
            } catch (e: Exception) {
                _statusDetail.value = "Scan réseau impossible : ${NemPspProtocol.errorName(e)}"
                LogRepository.error("WIFI", _statusDetail.value, ConnectionMode.WIFI_UDP)
            } finally {
                try {
                    broadcastSocket?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Interne
    // ---------------------------------------------------------------------

    private fun startReceiver(socket: DatagramSocket) {
        receiveJob?.cancel()
        receiveJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(512)
            val packet = DatagramPacket(buffer, buffer.size)
            while (isActive && !socket.isClosed) {
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: Exception) {
                    break
                }
                if (packet.length < NemPspProtocol.CONTROL_FRAME_LENGTH) continue
                if (!NemPspProtocol.isControlFrame(buffer)) continue

                when (NemPspProtocol.controlType(buffer)) {
                    NemPspProtocol.CTRL_PONG -> onPongReceived(NemPspProtocol.controlPayload(buffer), packet)
                    NemPspProtocol.CTRL_WELCOME -> {
                        val from = packet.address.hostAddress ?: targetIp
                        LogRepository.success(
                            "WIFI",
                            "Récepteur NEMPSP identifié à $from (protocole v${NemPspProtocol.controlPayload(buffer)})",
                            ConnectionMode.WIFI_UDP
                        )
                    }
                }
            }
        }
    }

    private fun onPongReceived(nonce: Int, packet: DatagramPacket) {
        val now = SystemClock.elapsedRealtime()
        // Arithmétique Int : correcte même après un déboulement du compteur elapsedRealtime.
        val rtt = (now.toInt() - nonce).coerceAtLeast(0)
        _latencyMs.value = rtt.toLong()
        lastPongAt = now

        if (_status.value != ConnectionStatus.CONNECTED) {
            val from = packet.address.hostAddress ?: targetIp
            _status.value = ConnectionStatus.CONNECTED
            _statusDetail.value = "Connecté au récepteur $from:$targetPort (aller-retour ${rtt}ms)"
            LogRepository.success(
                "WIFI",
                "CONNEXION ÉTABLIE avec le récepteur $from:$targetPort — aller-retour ${rtt}ms",
                ConnectionMode.WIFI_UDP
            )
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            var helloSent = false
            while (isActive) {
                val socket = udpSocket
                val address = serverAddress
                if (socket == null || socket.isClosed || address == null) break

                try {
                    if (!helloSent) {
                        val hello = NemPspProtocol.helloFrame()
                        socket.send(DatagramPacket(hello, hello.size, address, targetPort))
                        helloSent = true
                    }
                    val ping = NemPspProtocol.pingFrame(SystemClock.elapsedRealtime().toInt())
                    socket.send(DatagramPacket(ping, ping.size, address, targetPort))
                } catch (e: Exception) {
                    handleSendFailure(e)
                }

                evaluateReplyTimeout()

                delay(
                    if (_status.value == ConnectionStatus.CONNECTED) {
                        PING_INTERVAL_CONNECTED_MS
                    } else {
                        PING_INTERVAL_CONNECTING_MS
                    }
                )
            }
        }
    }

    private fun evaluateReplyTimeout() {
        val now = SystemClock.elapsedRealtime()
        if (lastPongAt == 0L) {
            if (now - startedAt > NO_REPLY_ERROR_AFTER_MS && _status.value != ConnectionStatus.ERROR) {
                _status.value = ConnectionStatus.ERROR
                _statusDetail.value =
                    "Aucune réponse du récepteur $target depuis ${((now - startedAt) / 1000)}s. " +
                        "Le Mode Récepteur tourne-t-il sur l'autre appareil ? Êtes-vous sur le même WiFi ?"
                LogRepository.error("WIFI", _statusDetail.value, ConnectionMode.WIFI_UDP)
            }
            return
        }

        if (now - lastPongAt > CONNECTION_LOST_AFTER_MS) {
            if (_status.value == ConnectionStatus.CONNECTED) {
                _status.value = ConnectionStatus.ERROR
                _statusDetail.value =
                    "Le récepteur $target ne répond plus depuis ${((now - lastPongAt) / 1000)}s. " +
                        "Récepteur arrêté, WiFi coupé ou IP changée. Nouvelle tentative en cours..."
                LogRepository.error("WIFI", _statusDetail.value, ConnectionMode.WIFI_UDP)
            }
            _latencyMs.value = -1L
        }
    }

    private fun handleSendFailure(error: Throwable) {
        val detail = NemPspProtocol.describeSendError(error, target)
        if (_status.value != ConnectionStatus.ERROR) {
            _status.value = ConnectionStatus.ERROR
            _statusDetail.value = detail
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastErrorLogAt >= ERROR_LOG_THROTTLE_MS) {
            lastErrorLogAt = now
            LogRepository.error("WIFI", detail, ConnectionMode.WIFI_UDP)
        }
    }

    private fun closeSocketQuietly() {
        try {
            udpSocket?.close()
        } catch (_: Exception) {
        }
        udpSocket = null
    }

    private fun isUsableAddress(ip: String): Boolean {
        if (ip.isEmpty()) return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val value = part.toIntOrNull()
            value != null && value in 0..255 && !(part.length > 1 && part.startsWith("0"))
        }
    }
}

package com.example.nempsp.network

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
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer

class WifiUdpClient(private val scope: CoroutineScope) {
    private var udpSocket: DatagramSocket? = null
    private var serverAddress: InetAddress? = null
    private var targetIp: String = "192.168.1.100"
    private var targetPort: Int = 8989

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _latencyMs = MutableStateFlow(-1L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _discoveredServers = MutableStateFlow<List<String>>(emptyList())
    val discoveredServers: StateFlow<List<String>> = _discoveredServers.asStateFlow()

    private var pingJob: Job? = null
    private var discoveryJob: Job? = null

    fun connect(ip: String, port: Int = 8989) {
        targetIp = ip.trim()
        targetPort = port
        _status.value = ConnectionStatus.CONNECTING
        LogRepository.info("WIFI", "Initialisation socket UDP vers $targetIp:$targetPort...", ConnectionMode.WIFI_UDP)

        scope.launch(Dispatchers.IO) {
            try {
                udpSocket?.close()
                val socket = DatagramSocket()
                udpSocket = socket
                serverAddress = InetAddress.getByName(targetIp)
                _status.value = ConnectionStatus.CONNECTED
                LogRepository.success("WIFI", "Socket UDP prêt sur port local ${socket.localPort} -> $targetIp:$targetPort", ConnectionMode.WIFI_UDP)

                startPingTester()
            } catch (e: Exception) {
                _status.value = ConnectionStatus.ERROR
                LogRepository.error("WIFI", "Échec création socket UDP: ${e.localizedMessage}", ConnectionMode.WIFI_UDP)
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        discoveryJob?.cancel()
        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null
        serverAddress = null
        _status.value = ConnectionStatus.DISCONNECTED
        _latencyMs.value = -1L
        LogRepository.info("WIFI", "Déconnecté du serveur WiFi", ConnectionMode.WIFI_UDP)
    }

    fun sendGamepadState(state: GamepadState): Boolean {
        val socket = udpSocket ?: return false
        val address = serverAddress ?: return false
        if (socket.isClosed) return false

        return try {
            val bytes = state.toBinaryPacket()
            val packet = DatagramPacket(bytes, bytes.size, address, targetPort)
            socket.send(packet)
            true
        } catch (e: Exception) {
            LogRepository.error("WIFI", "Erreur envoi paquet UDP: ${e.message}", ConnectionMode.WIFI_UDP)
            false
        }
    }

    /**
     * Start background auto-discovery: Broadcast UDP packet on port 8989 and listen for reply
     */
    fun startAutoDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch(Dispatchers.IO) {
            LogRepository.info("WIFI", "Lancement recherche automatique du serveur NEMPSP sur le réseau local...", ConnectionMode.WIFI_UDP)
            try {
                val broadcastSocket = DatagramSocket()
                broadcastSocket.broadcast = true
                broadcastSocket.soTimeout = 2500

                val payload = "NEMPSP_DISCOVERY_REQUEST".toByteArray(Charsets.UTF_8)
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(payload, payload.size, broadcastAddr, 8989)
                broadcastSocket.send(packet)
                LogRepository.info("WIFI", "Signal broadcast envoyé sur 255.255.255.255:8989", ConnectionMode.WIFI_UDP)

                val buffer = ByteArray(512)
                val recvPacket = DatagramPacket(buffer, buffer.size)

                val foundList = mutableListOf<String>()
                val startTime = System.currentTimeMillis()
                while (isActive && System.currentTimeMillis() - startTime < 4000) {
                    try {
                        broadcastSocket.receive(recvPacket)
                        val text = String(recvPacket.data, 0, recvPacket.length, Charsets.UTF_8)
                        val senderIp = recvPacket.address.hostAddress ?: ""
                        if (text.startsWith("NEMPSP_SERVER") && !foundList.contains(senderIp)) {
                            foundList.add(senderIp)
                            _discoveredServers.value = foundList.toList()
                            LogRepository.success("WIFI", "Serveur Chromebook NEMPSP détecté à $senderIp ($text)", ConnectionMode.WIFI_UDP)
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        break
                    }
                }
                broadcastSocket.close()

                if (foundList.isEmpty()) {
                    LogRepository.warn("WIFI", "Aucun serveur auto-découvert. Entrez l'adresse IP du Chromebook manuellement.", ConnectionMode.WIFI_UDP)
                }
            } catch (e: Exception) {
                LogRepository.error("WIFI", "Erreur scan réseau local: ${e.localizedMessage}", ConnectionMode.WIFI_UDP)
            }
        }
    }

    private fun startPingTester() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive && _status.value == ConnectionStatus.CONNECTED) {
                try {
                    val start = System.currentTimeMillis()
                    val socket = Socket()
                    socket.connect(InetSocketAddress(targetIp, targetPort), 800)
                    val out = socket.getOutputStream()
                    out.write("PING\n".toByteArray(Charsets.UTF_8))
                    out.flush()
                    socket.close()
                    val duration = System.currentTimeMillis() - start
                    _latencyMs.value = duration
                } catch (_: Exception) {
                    // TCP ping might not be open if server only has UDP, that's fine
                }
                delay(2000)
            }
        }
    }
}

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
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class UsbGamepadClient(private val scope: CoroutineScope) {
    private var usbSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var targetPort: Int = 8989

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _latencyMs = MutableStateFlow(-1L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private var pingJob: Job? = null

    fun connect(port: Int = 8989) {
        targetPort = port
        _status.value = ConnectionStatus.CONNECTING
        LogRepository.info("USB", "Connexion via tunnel USB (ADB Reverse 127.0.0.1:$port)...", ConnectionMode.USB_ADB)

        scope.launch(Dispatchers.IO) {
            try {
                disconnect()
                val socket = Socket()
                socket.tcpNoDelay = true // Disable Nagle's algorithm for minimum gaming latency
                socket.connect(InetSocketAddress("127.0.0.1", targetPort), 1500)
                usbSocket = socket
                outputStream = socket.getOutputStream()

                _status.value = ConnectionStatus.CONNECTED
                LogRepository.success("USB", "Connecté avec succès au serveur Chromebook via USB (127.0.0.1:$port) !", ConnectionMode.USB_ADB)

                // Send initial handshake
                val hello = "NEMPSP_USB_CLIENT_HELLO\n".toByteArray(Charsets.UTF_8)
                outputStream?.write(hello)
                outputStream?.flush()

                startLatencyMonitor()
            } catch (e: Exception) {
                _status.value = ConnectionStatus.ERROR
                LogRepository.error("USB", "Échec connexion USB: ${e.localizedMessage}. Assurez-vous d'avoir exécuté 'adb reverse tcp:$port tcp:$port' sur le Chromebook.", ConnectionMode.USB_ADB)
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            usbSocket?.close()
        } catch (_: Exception) {}
        outputStream = null
        usbSocket = null
        _status.value = ConnectionStatus.DISCONNECTED
        _latencyMs.value = -1L
        LogRepository.info("USB", "Déconnexion tunnel USB", ConnectionMode.USB_ADB)
    }

    fun sendGamepadState(state: GamepadState): Boolean {
        val stream = outputStream ?: return false
        val socket = usbSocket ?: return false
        if (socket.isClosed || !socket.isConnected) return false

        return try {
            val packet = state.toBinaryPacket()
            stream.write(packet)
            stream.flush()
            true
        } catch (e: Exception) {
            LogRepository.error("USB", "Erreur transmission USB: ${e.message}", ConnectionMode.USB_ADB)
            disconnect()
            false
        }
    }

    private fun startLatencyMonitor() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive && _status.value == ConnectionStatus.CONNECTED) {
                try {
                    val start = System.currentTimeMillis()
                    // Send lightweight 1-byte ping heartbeat
                    outputStream?.write(byteArrayOf(0x00))
                    outputStream?.flush()
                    val duration = System.currentTimeMillis() - start
                    _latencyMs.value = duration.coerceAtLeast(1)
                } catch (_: Exception) {
                    // Ignored
                }
                delay(1500)
            }
        }
    }
}

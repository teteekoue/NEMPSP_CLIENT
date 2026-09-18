package com.example.nempsp.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
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
import java.util.UUID

/**
 * Client Bluetooth RFCOMM (profil série SPP).
 *
 * Corrections :
 * - `BluetoothAdapter.getDefaultAdapter()` (déprécié depuis API 31) remplacé par le
 *   `BluetoothManager` ; le `context` fourni est enfin utilisé.
 * - La latence n'est plus une constante inventée (« 2 ms ») : un vrai PING/PONG est échangé
 *   sur le canal RFCOMM, et la réception est surveillée pour détecter les déconnexions.
 * - Le handshake ASCII « NEMPSP_CLIENT_CONNECTED » (qui polluait le flux binaire) est
 *   remplacé par la trame de contrôle HELLO du protocole v2.
 */
class BluetoothGamepadClient(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        /** UUID série standard (SPP) pour la communication manette en RFCOMM. */
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PING_INTERVAL_MS: Long = 1000
        private const val CONNECTION_LOST_AFTER_MS: Long = 6000
    }

    private val bluetoothAdapter: BluetoothAdapter? = runCatching {
        val manager = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }.getOrNull()

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @Volatile
    private var lastPongAt: Long = 0L

    @Volatile
    private var connectStartedAt: Long = 0L

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _statusDetail = MutableStateFlow("Aucune connexion Bluetooth en cours")
    val statusDetail: StateFlow<String> = _statusDetail.asStateFlow()

    private val _latencyMs = MutableStateFlow(-1L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pairedDevices: StateFlow<List<Pair<String, String>>> = _pairedDevices.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private var connectionJob: Job? = null
    private var readerJob: Job? = null
    private var pingJob: Job? = null

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        @SuppressLint("MissingPermission")
        get() = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _statusDetail.value = "Bluetooth absent sur cet appareil"
            LogRepository.warn("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
            _pairedDevices.value = emptyList()
            return
        }
        if (!isBluetoothEnabled) {
            _statusDetail.value = "Bluetooth désactivé : activez-le dans les paramètres Android"
            LogRepository.warn("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
            _pairedDevices.value = emptyList()
            return
        }

        try {
            val devices: Set<BluetoothDevice> = adapter.bondedDevices ?: emptySet()
            val list = devices.map { device ->
                (runCatching { device.name }.getOrNull() ?: "Appareil inconnu") to device.address
            }.sortedBy { it.first }
            _pairedDevices.value = list
            _statusDetail.value =
                if (list.isEmpty()) "Aucun appareil appairé. Appairez d'abord les deux appareils dans Android."
                else "${list.size} appareil(s) appairé(s) disponible(s)"
            LogRepository.info("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
        } catch (e: SecurityException) {
            _statusDetail.value = "Permission Bluetooth refusée. Autorisez « Appareils à proximité » puis réessayez."
            LogRepository.error("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
        } catch (e: Exception) {
            _statusDetail.value = "Liste des appareils impossible : ${NemPspProtocol.errorName(e)}"
            LogRepository.error("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _status.value = ConnectionStatus.ERROR
            _statusDetail.value = "Bluetooth absent sur cet appareil"
            LogRepository.error("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
            return
        }
        if (!isBluetoothEnabled) {
            _status.value = ConnectionStatus.ERROR
            _statusDetail.value = "Bluetooth désactivé : activez-le dans les paramètres Android"
            LogRepository.error("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
            return
        }

        closeQuietly()
        _status.value = ConnectionStatus.CONNECTING
        _statusDetail.value = "Connexion Bluetooth RFCOMM vers $deviceAddress..."
        _latencyMs.value = -1L
        connectStartedAt = SystemClock.elapsedRealtime()
        lastPongAt = 0L

        connectionJob = scope.launch(Dispatchers.IO) {
            try {
                val device = adapter.getRemoteDevice(deviceAddress)
                val deviceName = runCatching { device.name }.getOrNull() ?: deviceAddress
                LogRepository.info("BT", "Connexion RFCOMM/SPP à $deviceName ($deviceAddress)...", ConnectionMode.BLUETOOTH)

                // L'exploration radio ralentit fortement la connexion : on l'arrête.
                runCatching { adapter.cancelDiscovery() }

                val socket = openSocketWithFallback(device)

                bluetoothSocket = socket
                outputStream = socket.outputStream
                _connectedDeviceName.value = deviceName
                _statusDetail.value = "Canal Bluetooth ouvert avec $deviceName — attente de la réponse du récepteur..."

                startReader(socket)
                startPingLoop()
            } catch (e: SecurityException) {
                failConnect("Permission Bluetooth refusée. Autorisez « Appareils à proximité » dans Android.")
            } catch (e: Exception) {
                failConnect(
                    "Échec de la connexion Bluetooth à $deviceAddress (${NemPspProtocol.errorName(e)}). " +
                        "Le récepteur NEMPSP tourne-t-il sur l'autre appareil ? Sont-ils appairés ?"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openSocketWithFallback(device: BluetoothDevice): BluetoothSocket {
        var lastError: Exception? = null

        // 1. RFCOMM sécurisé standard
        try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            return socket
        } catch (e: Exception) {
            lastError = e
            LogRepository.warn("BT", "RFCOMM sécurisé échoué (${NemPspProtocol.errorName(e)}), essai du mode non sécurisé...", ConnectionMode.BLUETOOTH)
        }

        // 2. RFCOMM non sécurisé
        try {
            val socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            return socket
        } catch (e: Exception) {
            lastError = e
            LogRepository.warn("BT", "RFCOMM non sécurisé échoué, essai du canal direct 1...", ConnectionMode.BLUETOOTH)
        }

        // 3. Canal RFCOMM direct (contourne certains appareils rétifs au SDP)
        try {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            val socket = method.invoke(device, 1) as BluetoothSocket
            socket.connect()
            return socket
        } catch (e: Exception) {
            lastError = e
        }

        throw lastError ?: Exception("Connexion Bluetooth impossible")
    }

    fun disconnect() {
        closeQuietly()
        _connectedDeviceName.value = null
        _status.value = ConnectionStatus.DISCONNECTED
        _statusDetail.value = "Déconnecté du récepteur Bluetooth"
        _latencyMs.value = -1L
        LogRepository.info("BT", "Déconnexion Bluetooth effectuée", ConnectionMode.BLUETOOTH)
    }

    fun sendGamepadState(state: GamepadState): Boolean {
        val stream = outputStream ?: return false
        val socket = bluetoothSocket ?: return false
        if (!runCatching { socket.isConnected }.getOrDefault(false)) return false

        return try {
            stream.write(state.toBinaryPacket())
            stream.flush()
            true
        } catch (e: Exception) {
            _statusDetail.value = "Canal Bluetooth rompu : ${NemPspProtocol.errorName(e)}"
            LogRepository.error("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
            disconnect()
            false
        }
    }

    private fun startReader(socket: BluetoothSocket) {
        readerJob?.cancel()
        readerJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(256)
            val framer = NemPspStreamFramer(
                onGamepadFrame = { /* Le récepteur n'émet pas de trames manette */ },
                onControlFrame = { type, payload -> onControl(type, payload) }
            )
            var input: InputStream? = null
            try {
                input = socket.inputStream
                while (isActive && runCatching { socket.isConnected }.getOrDefault(false)) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) framer.push(buffer, read)
                }
            } catch (_: Exception) {
                // Fermeture normale du canal
            }
            if (_status.value == ConnectionStatus.CONNECTED || _status.value == ConnectionStatus.CONNECTING) {
                failConnect("Le récepteur Bluetooth a fermé le canal. Relancez le Mode Récepteur puis reconnectez.")
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
                    _statusDetail.value = "Connecté à ${_connectedDeviceName.value ?: "récepteur"} (aller-retour ${rtt}ms)"
                    LogRepository.success("BT", "CONNEXION BLUETOOTH ÉTABLIE — aller-retour ${rtt}ms", ConnectionMode.BLUETOOTH)
                }
            }
            NemPspProtocol.CTRL_WELCOME -> LogRepository.success(
                "BT",
                "Récepteur NEMPSP identifié (protocole v$payload)",
                ConnectionMode.BLUETOOTH
            )
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            var helloSent = false
            while (isActive) {
                val stream = outputStream
                val socket = bluetoothSocket
                if (stream == null || socket == null) break
                if (!runCatching { socket.isConnected }.getOrDefault(false)) break

                try {
                    if (!helloSent) {
                        stream.write(NemPspProtocol.helloFrame())
                        helloSent = true
                    }
                    stream.write(NemPspProtocol.pingFrame(SystemClock.elapsedRealtime().toInt()))
                    stream.flush()
                } catch (e: Exception) {
                    failConnect("Canal Bluetooth rompu : ${NemPspProtocol.errorName(e)}")
                    break
                }

                val now = SystemClock.elapsedRealtime()
                if (lastPongAt == 0L && now - connectStartedAt > 5000 && _status.value != ConnectionStatus.CONNECTED) {
                    _status.value = ConnectionStatus.ERROR
                    _statusDetail.value =
                        "Canal Bluetooth ouvert mais le récepteur ne répond pas. " +
                            "Le Mode Récepteur est-il démarré sur l'autre appareil ?"
                    LogRepository.error("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
                } else if (lastPongAt != 0L && now - lastPongAt > CONNECTION_LOST_AFTER_MS) {
                    if (_status.value == ConnectionStatus.CONNECTED) {
                        _status.value = ConnectionStatus.ERROR
                        _statusDetail.value = "Le récepteur Bluetooth ne répond plus. Nouvelle tentative en cours..."
                        LogRepository.warn("BT", _statusDetail.value, ConnectionMode.BLUETOOTH)
                    }
                    _latencyMs.value = -1L
                }

                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun failConnect(message: String) {
        _status.value = ConnectionStatus.ERROR
        _statusDetail.value = message
        _connectedDeviceName.value = null
        _latencyMs.value = -1L
        LogRepository.error("BT", message, ConnectionMode.BLUETOOTH)
    }

    private fun closeQuietly() {
        connectionJob?.cancel()
        readerJob?.cancel()
        pingJob?.cancel()
        connectionJob = null
        readerJob = null
        pingJob = null
        try {
            outputStream?.close()
        } catch (_: Exception) {
        }
        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {
        }
        outputStream = null
        bluetoothSocket = null
    }

}

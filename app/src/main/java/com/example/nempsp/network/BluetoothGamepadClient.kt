package com.example.nempsp.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.ConnectionStatus
import com.example.nempsp.model.GamepadState
import com.example.nempsp.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID

class BluetoothGamepadClient(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        // Standard SPP UUID for RFCOMM serial gamepad communication
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pairedDevices: StateFlow<List<Pair<String, String>>> = _pairedDevices.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private var connectionJob: Job? = null

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            LogRepository.warn("BT", "Bluetooth désactivé ou non supporté sur l'appareil", ConnectionMode.BLUETOOTH)
            _pairedDevices.value = emptyList()
            return
        }

        try {
            val devices = bluetoothAdapter.bondedDevices
            val list = devices.map { device ->
                (device.name ?: "Appareil inconnu") to device.address
            }
            _pairedDevices.value = list
            LogRepository.info("BT", "${list.size} appareil(s) Bluetooth appairé(s) trouvé(s)", ConnectionMode.BLUETOOTH)
        } catch (e: Exception) {
            LogRepository.error("BT", "Impossible de lister les appareils appairés: ${e.message}", ConnectionMode.BLUETOOTH)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        if (bluetoothAdapter == null) {
            LogRepository.error("BT", "Bluetooth non disponible sur cet appareil", ConnectionMode.BLUETOOTH)
            return
        }

        disconnect()
        _status.value = ConnectionStatus.CONNECTING

        connectionJob = scope.launch(Dispatchers.IO) {
            try {
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                val devName = device.name ?: deviceAddress
                LogRepository.info("BT", "Tentative connexion RFCOMM SPP à $devName ($deviceAddress)...", ConnectionMode.BLUETOOTH)

                // Cancel discovery to accelerate connection
                try {
                    bluetoothAdapter.cancelDiscovery()
                } catch (_: Exception) {}

                var socket: BluetoothSocket? = null
                try {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    socket.connect()
                } catch (e1: Exception) {
                    LogRepository.warn("BT", "RFCOMM standard échoué (${e1.message}), tentative mode insecure...", ConnectionMode.BLUETOOTH)
                    try {
                        socket?.close()
                        socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                        socket.connect()
                    } catch (e2: Exception) {
                        LogRepository.warn("BT", "RFCOMM insecure échoué, tentative canal direct 1...", ConnectionMode.BLUETOOTH)
                        socket?.close()
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = method.invoke(device, 1) as BluetoothSocket
                        socket.connect()
                    }
                }

                bluetoothSocket = socket
                outputStream = socket.outputStream

                _connectedDeviceName.value = devName
                _status.value = ConnectionStatus.CONNECTED
                LogRepository.success("BT", "Connecté avec succès en Bluetooth à $devName", ConnectionMode.BLUETOOTH)

                // Handshake message
                val handshake = "NEMPSP_CLIENT_CONNECTED\n".toByteArray(Charsets.UTF_8)
                outputStream?.write(handshake)
                outputStream?.flush()

            } catch (e: Exception) {
                _status.value = ConnectionStatus.ERROR
                _connectedDeviceName.value = null
                LogRepository.error("BT", "Échec connexion Bluetooth: ${e.localizedMessage}", ConnectionMode.BLUETOOTH)
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {}
        outputStream = null
        bluetoothSocket = null
        _connectedDeviceName.value = null
        _status.value = ConnectionStatus.DISCONNECTED
        LogRepository.info("BT", "Déconnexion Bluetooth effectuée", ConnectionMode.BLUETOOTH)
    }

    fun sendGamepadState(state: GamepadState): Boolean {
        val stream = outputStream ?: return false
        val socket = bluetoothSocket ?: return false
        if (!socket.isConnected) return false

        return try {
            val packet = state.toBinaryPacket()
            stream.write(packet)
            stream.flush()
            true
        } catch (e: Exception) {
            LogRepository.error("BT", "Erreur envoi paquet Bluetooth: ${e.message}", ConnectionMode.BLUETOOTH)
            disconnect()
            false
        }
    }
}

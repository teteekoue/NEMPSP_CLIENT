package com.example.nempsp.network

import android.content.Context
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.ConnectionStatus
import com.example.nempsp.model.GamepadState
import com.example.nempsp.model.PspButton
import com.example.nempsp.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class ConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val wifiClient = WifiUdpClient(scope)
    val btClient = BluetoothGamepadClient(context, scope)
    val usbClient = UsbGamepadClient(scope)

    private val _currentMode = MutableStateFlow(ConnectionMode.WIFI_UDP)
    val currentMode: StateFlow<ConnectionMode> = _currentMode.asStateFlow()

    // Combined connection status for active mode
    val connectionStatus: StateFlow<ConnectionStatus> = combine(
        _currentMode,
        wifiClient.status,
        btClient.status,
        usbClient.status
    ) { mode, wifiStatus, btStatus, usbStatus ->
        when (mode) {
            ConnectionMode.WIFI_UDP -> wifiStatus
            ConnectionMode.BLUETOOTH -> btStatus
            ConnectionMode.USB_ADB -> usbStatus
        }
    }.stateIn(scope, SharingStarted.Eagerly, ConnectionStatus.DISCONNECTED)

    // Latency ms
    val latencyMs: StateFlow<Long> = combine(
        _currentMode,
        wifiClient.latencyMs,
        usbClient.latencyMs
    ) { mode, wifiLat, usbLat ->
        when (mode) {
            ConnectionMode.WIFI_UDP -> wifiLat
            ConnectionMode.BLUETOOTH -> 2L // BT RFCOMM typical estimated ping
            ConnectionMode.USB_ADB -> usbLat
        }
    }.stateIn(scope, SharingStarted.Eagerly, -1L)

    private val _currentState = MutableStateFlow(GamepadState())
    val currentState: StateFlow<GamepadState> = _currentState.asStateFlow()

    // Transmission telemetry
    private val packetCounter = AtomicInteger(0)
    private val _totalPacketsSent = MutableStateFlow(0)
    val totalPacketsSent: StateFlow<Int> = _totalPacketsSent.asStateFlow()

    private val _packetsPerSecond = MutableStateFlow(0)
    val packetsPerSecond: StateFlow<Int> = _packetsPerSecond.asStateFlow()

    private var fpsJob: Job? = null
    private var continuousLoopJob: Job? = null
    private var sequenceCounter = 0

    // Virtual Server / Test Loopback mode for instant debugging
    private val _testModeEnabled = MutableStateFlow(false)
    val testModeEnabled: StateFlow<Boolean> = _testModeEnabled.asStateFlow()

    init {
        startTelemetryLoop()
        startTransmissionLoop()
        LogRepository.info("NEMPSP", "Gestionnaire de connexion NEMPSP démarré", ConnectionMode.WIFI_UDP)
    }

    fun setMode(mode: ConnectionMode) {
        if (_currentMode.value == mode) return
        _currentMode.value = mode
        LogRepository.info("MODE", "Mode de communication changé vers: ${mode.displayName}", mode)
    }

    fun setTestMode(enabled: Boolean) {
        _testModeEnabled.value = enabled
        if (enabled) {
            LogRepository.success("TEST", "Mode Test / Simulation Locale Activé (Les paquets sont vérifiés et loggués en temps réel)", _currentMode.value)
        } else {
            LogRepository.info("TEST", "Mode Test désactivé", _currentMode.value)
        }
    }

    fun onButtonChanged(button: PspButton, pressed: Boolean) {
        val updated = _currentState.value.withButton(button, pressed)
        _currentState.value = updated
        dispatchState(updated)

        if (pressed) {
            LogRepository.packet(
                "BTN",
                "${button.symbol} [${button.label}] APPUYÉ (Masque: 0x${Integer.toHexString(updated.buttonsMask)})",
                _currentMode.value
            )
        } else {
            LogRepository.packet(
                "BTN",
                "${button.symbol} [${button.label}] RELÂCHÉ",
                _currentMode.value
            )
        }
    }

    fun onAnalogChanged(x: Float, y: Float) {
        val updated = _currentState.value.withAnalog(x, y)
        _currentState.value = updated
        dispatchState(updated)
    }

    private fun dispatchState(baseState: GamepadState) {
        sequenceCounter = (sequenceCounter + 1) and 0xFFFF
        val stateWithSeq = baseState.copy(sequenceNumber = sequenceCounter)

        var sent = false
        val mode = _currentMode.value

        if (_testModeEnabled.value) {
            // In test mode, count as transmitted and simulate instant receipt
            sent = true
        } else {
            sent = when (mode) {
                ConnectionMode.WIFI_UDP -> wifiClient.sendGamepadState(stateWithSeq)
                ConnectionMode.BLUETOOTH -> btClient.sendGamepadState(stateWithSeq)
                ConnectionMode.USB_ADB -> usbClient.sendGamepadState(stateWithSeq)
            }
        }

        if (sent) {
            packetCounter.incrementAndGet()
            _totalPacketsSent.value = packetCounter.get()
        }
    }

    private fun startTransmissionLoop() {
        continuousLoopJob?.cancel()
        continuousLoopJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                // Keep streaming analog state and button heartbeats at 60 Hz if connected
                val isConnected = connectionStatus.value == ConnectionStatus.CONNECTED || _testModeEnabled.value
                if (isConnected) {
                    dispatchState(_currentState.value)
                }
                delay(16) // ~60 FPS
            }
        }
    }

    private fun startTelemetryLoop() {
        fpsJob?.cancel()
        fpsJob = scope.launch(Dispatchers.Default) {
            var lastCount = 0
            while (isActive) {
                delay(1000)
                val current = packetCounter.get()
                _packetsPerSecond.value = current - lastCount
                lastCount = current
            }
        }
    }
}

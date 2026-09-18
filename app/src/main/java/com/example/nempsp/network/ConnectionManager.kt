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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/**
 * Orchestrateur des trois transports (WiFi UDP, Bluetooth RFCOMM, USB/TCP).
 *
 * Corrections :
 * - **Un seul thread émetteur** : auparavant l'interface ET une boucle 60 Hz écrivaient en
 *   même temps sur le socket, avec un compteur de séquence non atomique → paquets envoyés
 *   dans le désordre et séquences dupliquées. Tout passe désormais par une unique coroutine
 *   (réveil immédiat sur appui + battement de cœur à la fréquence configurée).
 * - La fréquence d'envoi utilise enfin `LayoutConfig.autoSendRateMs` (elle était ignorée,
 *   codée en dur à 16 ms).
 * - La latence Bluetooth est réelle (PING/PONG) et non plus une constante inventée de 2 ms.
 * - Aucun paquet n'est émis tant que le récepteur n'a pas réellement répondu.
 * - `statusDetail` expose une explication lisible de l'état (affichée dans l'interface).
 */
class ConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val wifiClient = WifiUdpClient(scope)
    val btClient = BluetoothGamepadClient(context, scope)
    val usbClient = UsbGamepadClient(scope)

    private val _currentMode = MutableStateFlow(ConnectionMode.WIFI_UDP)
    val currentMode: StateFlow<ConnectionMode> = _currentMode.asStateFlow()

    /** État de connexion du transport actif. */
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

    /** Explication détaillée de l'état du transport actif (diagnostic utilisateur). */
    val statusDetail: StateFlow<String> = combine(
        _currentMode,
        wifiClient.statusDetail,
        btClient.statusDetail,
        usbClient.statusDetail
    ) { mode, wifiDetail, btDetail, usbDetail ->
        when (mode) {
            ConnectionMode.WIFI_UDP -> wifiDetail
            ConnectionMode.BLUETOOTH -> btDetail
            ConnectionMode.USB_ADB -> usbDetail
        }
    }.stateIn(scope, SharingStarted.Eagerly, "")

    /** Latence réelle mesurée par aller-retour (PING/PONG) sur le transport actif. */
    val latencyMs: StateFlow<Long> = combine(
        _currentMode,
        wifiClient.latencyMs,
        btClient.latencyMs,
        usbClient.latencyMs
    ) { mode, wifiLat, btLat, usbLat ->
        when (mode) {
            ConnectionMode.WIFI_UDP -> wifiLat
            ConnectionMode.BLUETOOTH -> btLat
            ConnectionMode.USB_ADB -> usbLat
        }
    }.stateIn(scope, SharingStarted.Eagerly, -1L)

    private val _currentState = MutableStateFlow(GamepadState())
    val currentState: StateFlow<GamepadState> = _currentState.asStateFlow()

    // Télémétrie d'émission
    private val packetCounter = AtomicInteger(0)
    private val sequenceCounter = AtomicInteger(0)

    private val _totalPacketsSent = MutableStateFlow(0)
    val totalPacketsSent: StateFlow<Int> = _totalPacketsSent.asStateFlow()

    private val _packetsPerSecond = MutableStateFlow(0)
    val packetsPerSecond: StateFlow<Int> = _packetsPerSecond.asStateFlow()

    // Mode Test / Simulation : boucle locale sans réseau
    private val _testModeEnabled = MutableStateFlow(false)
    val testModeEnabled: StateFlow<Boolean> = _testModeEnabled.asStateFlow()

    /** Signal « l'état a changé, émet tout de suite » (canal conflaté : 1 seul en attente). */
    private val changeSignal = Channel<Unit>(Channel.CONFLATED)

    @Volatile
    private var sendIntervalMs: Long = DEFAULT_SEND_INTERVAL_MS

    private var senderJob: Job? = null
    private var telemetryJob: Job? = null

    init {
        startSenderLoop()
        startTelemetryLoop()
        LogRepository.info(
            "NEMPSP",
            "Gestionnaire de connexion démarré (fréquence cible ${sendIntervalMs}ms)",
            ConnectionMode.WIFI_UDP
        )
    }

    companion object {
        const val DEFAULT_SEND_INTERVAL_MS: Long = 16L
        private const val MIN_SEND_INTERVAL_MS: Long = 4L
        private const val MAX_SEND_INTERVAL_MS: Long = 100L
    }

    fun setMode(mode: ConnectionMode) {
        if (_currentMode.value == mode) return
        _currentMode.value = mode
        LogRepository.info("MODE", "Mode de communication : ${mode.displayName}", mode)
    }

    /** Fréquence du battement de cœur d'émission, pilotée par `LayoutConfig.autoSendRateMs`. */
    fun updateSendRate(rateMs: Long) {
        sendIntervalMs = rateMs.coerceIn(MIN_SEND_INTERVAL_MS, MAX_SEND_INTERVAL_MS)
    }

    fun setTestMode(enabled: Boolean) {
        _testModeEnabled.value = enabled
        if (enabled) {
            LogRepository.success(
                "TEST",
                "Mode Test activé : les appuis sont comptés localement, rien n'est envoyé sur le réseau",
                _currentMode.value
            )
        } else {
            LogRepository.info("TEST", "Mode Test désactivé", _currentMode.value)
        }
        changeSignal.trySend(Unit)
    }

    fun onButtonChanged(button: PspButton, pressed: Boolean) {
        val updated = _currentState.value.withButton(button, pressed)
        _currentState.value = updated
        changeSignal.trySend(Unit)

        val maskHex = Integer.toHexString(updated.buttonsMask)
        if (pressed) {
            LogRepository.packet("BTN", "${button.symbol} [${button.label}] APPUYÉ (masque 0x$maskHex)", _currentMode.value)
        } else {
            LogRepository.packet("BTN", "${button.symbol} [${button.label}] RELÂCHÉ", _currentMode.value)
        }
    }

    fun onAnalogChanged(x: Float, y: Float) {
        _currentState.value = _currentState.value.withAnalog(x, y)
        changeSignal.trySend(Unit)
    }

    /**
     * Unique point d'émission : réveillé immédiatement à chaque changement d'état, sinon à
     * la fréquence [sendIntervalMs]. Toutes les écritures socket viennent de cette coroutine,
     * ce qui garantit l'ordre des paquets et l'unicité des numéros de séquence.
     */
    private fun startSenderLoop() {
        senderJob?.cancel()
        senderJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                withTimeoutOrNull(sendIntervalMs) { changeSignal.receive() }
                sendCurrentState()
            }
        }
    }

    private fun sendCurrentState() {
        val testMode = _testModeEnabled.value
        if (!testMode && connectionStatus.value != ConnectionStatus.CONNECTED) return

        val nextSequence = sequenceCounter.incrementAndGet() and 0xFFFF
        val state = _currentState.value.copy(sequenceNumber = nextSequence)

        val sent = if (testMode) {
            true
        } else {
            when (_currentMode.value) {
                ConnectionMode.WIFI_UDP -> wifiClient.sendGamepadState(state)
                ConnectionMode.BLUETOOTH -> btClient.sendGamepadState(state)
                ConnectionMode.USB_ADB -> usbClient.sendGamepadState(state)
            }
        }

        if (sent) {
            _totalPacketsSent.value = packetCounter.incrementAndGet()
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch(Dispatchers.Default) {
            var lastCount = 0
            while (isActive) {
                delay(1000)
                val current = packetCounter.get()
                _packetsPerSecond.value = (current - lastCount).coerceAtLeast(0)
                lastCount = current
            }
        }
    }

    /** Libère les ressources réseau (appelé quand l'activité disparaît définitivement). */
    fun release() {
        senderJob?.cancel()
        telemetryJob?.cancel()
        changeSignal.close()
        wifiClient.disconnect()
        btClient.disconnect()
        usbClient.disconnect()
    }

}

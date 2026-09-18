package com.example.nempsp.server

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.PspButton
import com.example.nempsp.injection.InjectionBus
import com.example.nempsp.network.NemPspProtocol
import com.example.nempsp.network.NemPspStreamFramer
import com.example.nempsp.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service de premier plan garantissant que le récepteur NEMPSP continue d'écouter les touches
 * (WiFi UDP, USB/TCP, Bluetooth RFCOMM) pendant que PPSSPP tourne en plein écran.
 *
 * Corrections apportées :
 * - **Vérité de l'état** : `isRunning`/`udpListening`/`tcpListening`/`btListening` ne passent à
 *   `true` qu'après ouverture réelle des sockets, et `startupError` explique tout échec.
 *   Avant, l'écran affichait « EN LIGNE (8989) » même quand le port n'était pas ouvert, donc le
 *   téléphone recevait « port inaccessible » sans comprendre pourquoi.
 * - **Réponses PING/PONG et HELLO/WELCOME** (trames de contrôle du protocole v2) : le client
 *   peut enfin vérifier que le récepteur existe et mesurer une latence réelle.
 * - **WifiLock + WakeLock** : sans eux, Android passe le WiFi en économie d'énergie et le CPU
 *   dort, ce qui fait perdre des paquets ou tuer l'écoute dès que l'écran s'éteint.
 * - **`startForeground` protégé** : sur Android 14+, un type `connectedDevice` sans permission
 *   qualifiante levait une `SecurityException` et faisait mourir le service (donc plus aucun
 *   écouteur UDP) ; on journalise et on continue au lieu de planter.
 * - **Mises à jour d'état atomiques** (`MutableStateFlow.update`) : 4 threads (UDP, TCP, BT,
 *   statistiques) modifiaient le même état en lecture-modification-écriture → pertes de MAJ.
 * - **Compteurs atomiques** et fin du double comptage des paquets reçus.
 * - **Arrêt propre** via `Context.stopService` (plus d'`IllegalStateException` en arrière-plan).
 * - Détection de toutes les adresses IPv4 utiles (un Chromebook expose souvent plusieurs
 *   interfaces, dont le pont du conteneur Android qui n'est PAS joignable en WiFi).
 */
class NemPspForegroundServerService : Service() {

    companion object {
        const val CHANNEL_ID = "nempsp_server_foreground_channel"
        const val NOTIFICATION_ID = 8989
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val ACTION_START = "com.example.nempsp.ACTION_START_SERVER"
        const val ACTION_STOP = "com.example.nempsp.ACTION_STOP_SERVER"
        const val EXTRA_PORT = "com.example.nempsp.EXTRA_PORT"
        const val DEFAULT_PORT = 8989

        // État singleton observable par l'interface
        private val _serverState = MutableStateFlow(ServerStatus())
        val serverState: StateFlow<ServerStatus> = _serverState.asStateFlow()

        fun startService(context: Context, port: Int = DEFAULT_PORT) {
            val intent = Intent(context, NemPspForegroundServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                LogRepository.error(
                    "SERVEUR-SERVICE",
                    "Impossible de démarrer le service récepteur : ${NemPspProtocol.errorName(e)}",
                    ConnectionMode.WIFI_UDP
                )
            }
        }

        fun stopService(context: Context) {
            // stopService() est sûr même application en arrière-plan, alors qu'un
            // startService(ACTION_STOP) levait une IllegalStateException sur Android 8+.
            try {
                context.stopService(Intent(context, NemPspForegroundServerService::class.java))
            } catch (e: Exception) {
                LogRepository.warn(
                    "SERVEUR-SERVICE",
                    "Arrêt du service impossible : ${NemPspProtocol.errorName(e)}",
                    ConnectionMode.WIFI_UDP
                )
            }
        }

        fun injectSimulatedInput(button: PspButton, isPressed: Boolean) {
            val currentButtons = _serverState.value.activeButtons.toMutableList()
            if (isPressed) {
                if (!currentButtons.contains(button)) currentButtons.add(button)
            } else {
                currentButtons.remove(button)
            }

            var mask = 0
            for (btn in currentButtons) {
                mask = mask or btn.mask
            }

            val simulatedPacket = ServerDecodedPacket(
                sequenceNumber = ((_serverState.value.lastDecodedState?.sequenceNumber ?: 0) + 1) % 65536,
                buttonsMask = mask,
                analogX = _serverState.value.analogX,
                analogY = _serverState.value.analogY,
                activeButtons = currentButtons
            )
            onPacketReceivedStatic(simulatedPacket, "Testeur virtuel interne")
        }

        fun onPacketReceivedStatic(decoded: ServerDecodedPacket, source: String) {
            _serverState.update { current ->
                current.copy(
                    lastDecodedState = decoded,
                    lastClientSource = source,
                    activeButtons = decoded.activeButtons,
                    analogX = decoded.analogX,
                    analogY = decoded.analogY,
                    packetsReceived = current.packetsReceived + 1
                )
            }
            // Transmis au service d'accessibilité, qui pose les appuis dans PPSSPP.
            InjectionBus.publish(decoded)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var udpSocket: DatagramSocket? = null
    private var tcpServerSocket: ServerSocket? = null
    private var btServerSocket: BluetoothServerSocket? = null

    private var udpJob: Job? = null
    private var tcpJob: Job? = null
    private var btJob: Job? = null
    private var statsJob: Job? = null

    private val ppsCounter = AtomicInteger(0)
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopServerInternal()
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        val port = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        enterForeground("Démarrage du récepteur NEMPSP...")
        startServerInternal(port)
        return START_STICKY
    }

    private fun startServerInternal(port: Int) {
        if (_serverState.value.isRunning) {
            LogRepository.info("SERVEUR-SERVICE", "Récepteur déjà actif, démarrage ignoré", ConnectionMode.WIFI_UDP)
            return
        }

        val addresses = collectLocalAddresses()
        val primaryIp = addresses.firstOrNull() ?: "127.0.0.1"

        _serverState.update {
            it.copy(
                isRunning = true,
                udpPort = port,
                tcpPort = port,
                localIpAddress = primaryIp,
                allIpAddresses = addresses,
                udpListening = false,
                tcpListening = false,
                btListening = false,
                startupError = null
            )
        }

        acquireLocks()

        LogRepository.info(
            "SERVEUR-SERVICE",
            "Démarrage du récepteur sur le port $port — IP candidates : ${if (addresses.isEmpty()) "aucune" else addresses.joinToString(", ")}",
            ConnectionMode.WIFI_UDP
        )

        udpJob = scope.launch(Dispatchers.IO) { runUdpListener(port) }
        tcpJob = scope.launch(Dispatchers.IO) { runTcpListener(port) }
        btJob = scope.launch(Dispatchers.IO) { runBluetoothListener() }
        statsJob = scope.launch(Dispatchers.Default) { runStatsLoop() }
    }

    private fun stopServerInternal() {
        val wasRunning = _serverState.value.isRunning
        _serverState.update {
            it.copy(
                isRunning = false,
                udpListening = false,
                tcpListening = false,
                btListening = false,
                activeConnectionsCount = 0,
                packetsPerSecond = 0
            )
        }

        udpJob?.cancel()
        tcpJob?.cancel()
        btJob?.cancel()
        statsJob?.cancel()
        udpJob = null
        tcpJob = null
        btJob = null
        statsJob = null

        try { udpSocket?.close() } catch (_: Exception) {}
        try { tcpServerSocket?.close() } catch (_: Exception) {}
        try { btServerSocket?.close() } catch (_: Exception) {}
        udpSocket = null
        tcpServerSocket = null
        btServerSocket = null

        releaseLocks()

        // Plus aucune trame n'arrivera : le service d'injection doit relâcher ses doigts virtuels.
        InjectionBus.reset()

        // Un arrêt demandé alors que rien ne tournait ne doit pas polluer le journal.
        if (wasRunning) {
            LogRepository.warn("SERVEUR-SERVICE", "Récepteur arrêté.", ConnectionMode.WIFI_UDP)
        }
    }

    // ------------------------------------------------------------------
    // Écouteur UDP (WiFi local + découverte + PING/PONG)
    // ------------------------------------------------------------------

    private suspend fun runUdpListener(port: Int) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port))
                soTimeout = 1000 // Permet à la boucle de se terminer proprement à l'arrêt
            }
            udpSocket = socket
            _serverState.update { it.copy(udpListening = true) }
            LogRepository.success("SERVEUR-UDP", "Écoute UDP 0.0.0.0:$port ACTIVE", ConnectionMode.WIFI_UDP)

            val buffer = ByteArray(512)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive && _serverState.value.isRunning) {
                try {
                    socket.receive(packet)
                } catch (_: java.net.SocketTimeoutException) {
                    continue
                }

                val length = packet.length
                if (length <= 0) continue
                val source = "${packet.address.hostAddress}:${packet.port}"

                // 1. Trame de contrôle (handshake / latence)
                if (length >= NemPspProtocol.CONTROL_FRAME_LENGTH && NemPspProtocol.isControlFrame(buffer)) {
                    handleControlDatagram(socket, packet, NemPspProtocol.controlType(buffer), NemPspProtocol.controlPayload(buffer), source)
                    continue
                }

                // 2. Requête de découverte réseau
                val text = String(buffer, 0, length, Charsets.US_ASCII)
                if (text.startsWith(NemPspProtocol.DISCOVERY_REQUEST)) {
                    val reply = "${NemPspProtocol.DISCOVERY_REPLY_PREFIX}:NEMPSP_ANDROID".toByteArray(Charsets.US_ASCII)
                    socket.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                    LogRepository.info("SERVEUR-UDP", "Découverte reçue de $source → réponse envoyée", ConnectionMode.WIFI_UDP)
                    continue
                }

                // 3. Trame manette
                NemPspPacketDecoder.decode(buffer, length)?.let { decoded ->
                    handleDecodedPacket(decoded, "WiFi UDP ($source)")
                }
            }
        } catch (e: Exception) {
            if (_serverState.value.isRunning) {
                val reason = describeBindFailure(e, port, "UDP")
                _serverState.update { it.copy(udpListening = false, startupError = reason) }
                LogRepository.error("SERVEUR-UDP", reason, ConnectionMode.WIFI_UDP)
            }
        } finally {
            _serverState.update { it.copy(udpListening = false) }
        }
    }

    private fun handleControlDatagram(
        socket: DatagramSocket,
        packet: DatagramPacket,
        type: Int,
        payload: Int,
        source: String
    ) {
        try {
            when (type) {
                NemPspProtocol.CTRL_PING -> {
                    val pong = NemPspProtocol.pongFrame(payload)
                    socket.send(DatagramPacket(pong, pong.size, packet.address, packet.port))
                }
                NemPspProtocol.CTRL_HELLO -> {
                    val welcome = NemPspProtocol.welcomeFrame()
                    socket.send(DatagramPacket(welcome, welcome.size, packet.address, packet.port))
                    LogRepository.success(
                        "SERVEUR-UDP",
                        "Client manette détecté à $source (protocole v$payload) — bienvenue envoyé",
                        ConnectionMode.WIFI_UDP
                    )
                }
            }
        } catch (e: Exception) {
            LogRepository.warn("SERVEUR-UDP", "Réponse de contrôle impossible vers $source : ${NemPspProtocol.errorName(e)}", ConnectionMode.WIFI_UDP)
        }
    }

    // ------------------------------------------------------------------
    // Écouteur TCP (USB / adb reverse)
    // ------------------------------------------------------------------

    private suspend fun runTcpListener(port: Int) = withContext(Dispatchers.IO) {
        try {
            val serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port))
            }
            tcpServerSocket = serverSocket
            _serverState.update { it.copy(tcpListening = true) }
            LogRepository.success("SERVEUR-TCP", "Écoute TCP 0.0.0.0:$port ACTIVE (USB/adb reverse)", ConnectionMode.USB_ADB)

            while (isActive && _serverState.value.isRunning) {
                val clientSocket = serverSocket.accept()
                _serverState.update {
                    it.copy(
                        activeConnectionsCount = it.activeConnectionsCount + 1,
                        lastClientSource = "USB TCP (${clientSocket.inetAddress.hostAddress})"
                    )
                }
                LogRepository.info("SERVEUR-TCP", "Client filaire connecté (${clientSocket.inetAddress.hostAddress})", ConnectionMode.USB_ADB)

                scope.launch(Dispatchers.IO) { handleTcpStream(clientSocket) }
            }
        } catch (e: Exception) {
            if (_serverState.value.isRunning) {
                val reason = describeBindFailure(e, port, "TCP")
                _serverState.update { it.copy(tcpListening = false, startupError = reason) }
                LogRepository.error("SERVEUR-TCP", reason, ConnectionMode.USB_ADB)
            }
        } finally {
            _serverState.update { it.copy(tcpListening = false) }
        }
    }

    private fun handleTcpStream(socket: Socket) {
        var output: OutputStream? = null
        try {
            socket.tcpNoDelay = true
            val input: InputStream = socket.getInputStream()
            output = socket.getOutputStream()
            val buffer = ByteArray(512)
            val framer = buildFramer(output, "USB TCP (${socket.inetAddress.hostAddress})")

            while (_serverState.value.isRunning && !socket.isClosed) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) framer.push(buffer, read)
            }
        } catch (_: Exception) {
            // Fermeture du tunnel
        } finally {
            try { socket.close() } catch (_: Exception) {}
            _serverState.update {
                it.copy(activeConnectionsCount = (it.activeConnectionsCount - 1).coerceAtLeast(0))
            }
            LogRepository.info("SERVEUR-TCP", "Client filaire déconnecté", ConnectionMode.USB_ADB)
        }
    }

    // ------------------------------------------------------------------
    // Écouteur Bluetooth RFCOMM (SPP)
    // ------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private suspend fun runBluetoothListener() = withContext(Dispatchers.IO) {
        val adapter: BluetoothAdapter? = runCatching {
            (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        }.getOrNull()

        if (adapter == null) {
            LogRepository.warn("SERVEUR-BT", "Bluetooth absent sur cet appareil : seul WiFi/USB est disponible", ConnectionMode.BLUETOOTH)
            return@withContext
        }
        if (!runCatching { adapter.isEnabled }.getOrDefault(false)) {
            LogRepository.warn("SERVEUR-BT", "Bluetooth désactivé : activez-le pour recevoir les touches en Bluetooth", ConnectionMode.BLUETOOTH)
            return@withContext
        }

        var serverSocket: BluetoothServerSocket? = null
        try {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord("NEMPSP_GAMEPAD_SERVER", SPP_UUID)
            btServerSocket = serverSocket
            _serverState.update { it.copy(btListening = true) }
            LogRepository.success("SERVEUR-BT", "Écoute Bluetooth RFCOMM/SPP ACTIVE", ConnectionMode.BLUETOOTH)

            while (isActive && _serverState.value.isRunning) {
                val clientSocket: BluetoothSocket = serverSocket.accept()
                val deviceName = runCatching { clientSocket.remoteDevice.name }.getOrNull() ?: "appareil Bluetooth"
                LogRepository.info("SERVEUR-BT", "Client Bluetooth connecté : $deviceName", ConnectionMode.BLUETOOTH)
                _serverState.update {
                    it.copy(
                        activeConnectionsCount = it.activeConnectionsCount + 1,
                        lastClientSource = "Bluetooth ($deviceName)"
                    )
                }

                scope.launch(Dispatchers.IO) { handleBluetoothStream(clientSocket, deviceName) }
            }
        } catch (e: SecurityException) {
            val reason = "Permission Bluetooth refusée : le récepteur Bluetooth est indisponible (WiFi/USB restent actifs)"
            _serverState.update { it.copy(btListening = false, startupError = reason) }
            LogRepository.warn("SERVEUR-BT", reason, ConnectionMode.BLUETOOTH)
        } catch (e: Exception) {
            if (_serverState.value.isRunning) {
                LogRepository.warn("SERVEUR-BT", "Écoute Bluetooth arrêtée : ${NemPspProtocol.errorName(e)}", ConnectionMode.BLUETOOTH)
            }
        } finally {
            _serverState.update { it.copy(btListening = false) }
        }
    }

    private fun handleBluetoothStream(socket: BluetoothSocket, deviceName: String) {
        var output: OutputStream? = null
        try {
            val input: InputStream = socket.inputStream
            output = socket.outputStream
            val buffer = ByteArray(512)
            val framer = buildFramer(output, "Bluetooth ($deviceName)")

            while (_serverState.value.isRunning && runCatching { socket.isConnected }.getOrDefault(false)) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) framer.push(buffer, read)
            }
        } catch (_: Exception) {
            // Fermeture du canal
        } finally {
            try { socket.close() } catch (_: Exception) {}
            _serverState.update {
                it.copy(activeConnectionsCount = (it.activeConnectionsCount - 1).coerceAtLeast(0))
            }
            LogRepository.info("SERVEUR-BT", "Client Bluetooth déconnecté ($deviceName)", ConnectionMode.BLUETOOTH)
        }
    }

    /**
     * Découpeur de flux partagé TCP/Bluetooth : trames manette + trames de contrôle
     * (réponse automatique aux PING et HELLO sur le même canal).
     */
    private fun buildFramer(output: OutputStream?, source: String): NemPspStreamFramer = NemPspStreamFramer(
        onGamepadFrame = { frame ->
            NemPspPacketDecoder.decode(frame)?.let { decoded ->
                handleDecodedPacket(decoded, source)
            }
        },
        onControlFrame = { type, payload ->
            when (type) {
                NemPspProtocol.CTRL_PING -> writeQuietly(output, NemPspProtocol.pongFrame(payload))
                NemPspProtocol.CTRL_HELLO -> writeQuietly(output, NemPspProtocol.welcomeFrame())
            }
        }
    )

    private fun writeQuietly(output: OutputStream?, bytes: ByteArray) {
        if (output == null) return
        try {
            output.write(bytes)
            output.flush()
        } catch (_: Exception) {
            // Le canal vient de se fermer : la boucle de lecture le détectera.
        }
    }

    // ------------------------------------------------------------------
    // Traitement des paquets manette
    // ------------------------------------------------------------------

    private fun handleDecodedPacket(decoded: ServerDecodedPacket, source: String) {
        ppsCounter.incrementAndGet()
        onPacketReceivedStatic(decoded, source)
    }

    private suspend fun runStatsLoop() {
        // Une `suspend fun` simple n'a pas de CoroutineScope implicite : `isActive` doit être
        // lu dans le contexte de la coroutine courante.
        while (currentCoroutineContext().isActive && _serverState.value.isRunning) {
            delay(1000)
            val pps = ppsCounter.getAndSet(0)
            _serverState.update { it.copy(packetsPerSecond = pps) }
            updateNotification(buildNotificationText(pps))
        }
    }

    private fun buildNotificationText(pps: Int): String {
        val state = _serverState.value
        return when {
            !state.isRunning -> "Arrêté"
            !state.readyForClient -> "ERREUR : aucun port ouvert (${state.startupError ?: "voir l'application"})"
            pps > 0 -> "Manette active | $pps paquets/s | ${state.lastClientSource}"
            else -> "En écoute sur ${state.localIpAddress}:${state.udpPort} — en attente de la manette"
        }
    }

    // ------------------------------------------------------------------
    // Verrous réseau / CPU
    // ------------------------------------------------------------------

    private fun acquireLocks() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wifiManager.createWifiLock(mode, "nempsp:server-wifi").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Exception) {
            LogRepository.warn("SERVEUR-SERVICE", "WifiLock indisponible : ${NemPspProtocol.errorName(e)}", ConnectionMode.WIFI_UDP)
        }

        try {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nempsp:server-cpu")?.apply {
                setReferenceCounted(false)
                acquire(8 * 60 * 60 * 1000L) // Garde-fou : 8 h maximum
            }
        } catch (e: Exception) {
            LogRepository.warn("SERVEUR-SERVICE", "WakeLock indisponible : ${NemPspProtocol.errorName(e)}", ConnectionMode.WIFI_UDP)
        }
    }

    private fun releaseLocks() {
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {
        }
        wifiLock = null
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    // ------------------------------------------------------------------
    // Notification & premier plan
    // ------------------------------------------------------------------

    private fun enterForeground(statusText: String) {
        val notification = buildNotification(statusText)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // Android 14+ : SecurityException si aucune permission qualifiante n'est accordée.
            // On ne plante pas : les écouteurs démarrent quand même tant que l'app est visible.
            LogRepository.error(
                "SERVEUR-SERVICE",
                "Service non passé au premier plan (${NemPspProtocol.errorName(e)}). " +
                    "Le récepteur peut être tué par Android en arrière-plan : accordez les permissions " +
                    "Bluetooth et Notifications à NEMPSP.",
                ConnectionMode.WIFI_UDP
            )
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {
            }
        }
    }

    private fun stopForegroundCompat() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Récepteur de manette NEMPSP",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient le récepteur actif pendant que PPSSPP tourne"
                setShowBadge(false)
            }
            runCatching {
                getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val contentIntent = PendingIntent.getActivity(this, 0, launchIntent, launchFlags)

        val stopIntent = Intent(this, NemPspForegroundServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, launchFlags)

        val state = _serverState.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎮 NEMPSP — Récepteur de manette")
            .setContentText(statusText)
            .setSubText("Port ${state.udpPort} · IP ${state.localIpAddress}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Arrêter le récepteur", stopPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        runCatching {
            (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                ?.notify(NOTIFICATION_ID, buildNotification(statusText))
        }
    }

    // ------------------------------------------------------------------
    // Adresses IP locales
    // ------------------------------------------------------------------

    /**
     * Renvoie les adresses IPv4 réellement joignables depuis le réseau local, triées par
     * pertinence (WiFi d'abord). Les interfaces du conteneur Android des Chromebook
     * (arc*, 100.64/10) sont écartées : elles ne sont PAS joignables en WiFi, et c'est une
     * cause classique d'« IP affichée mais paquets perdus ».
     */
    private fun collectLocalAddresses(): List<String> {
        val candidates = mutableListOf<Pair<Int, String>>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!runCatching { networkInterface.isUp }.getOrDefault(false)) continue
                val name = (networkInterface.name ?: "").lowercase()
                if (name.startsWith("lo") || name.startsWith("arc") || name.startsWith("vnic") ||
                    name.startsWith("tun") || name.startsWith("rmnet") || name.startsWith("dummy")
                ) {
                    continue
                }
                val priority = when {
                    name.startsWith("wlan") || name.startsWith("wifi") -> 0
                    name.startsWith("eth") -> 1
                    name.startsWith("ap") || name.startsWith("swlan") -> 2
                    else -> 3
                }
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address !is Inet4Address) continue
                    if (address.isLoopbackAddress || address.isLinkLocalAddress) continue
                    val host = address.hostAddress ?: continue
                    if (isCarrierGradeNat(host)) continue
                    if (candidates.none { it.second == host }) {
                        candidates.add(priority to host)
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Repli : API dépréciée mais parfois seule source disponible.
        if (candidates.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    candidates.add(
                        0 to String.format(
                            "%d.%d.%d.%d",
                            ipInt and 0xff,
                            ipInt shr 8 and 0xff,
                            ipInt shr 16 and 0xff,
                            ipInt shr 24 and 0xff
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        return candidates.sortedBy { it.first }.map { it.second }
    }

    private fun isCarrierGradeNat(host: String): Boolean {
        val parts = host.split(".")
        if (parts.size != 4) return false
        val first = parts[0].toIntOrNull() ?: return false
        val second = parts[1].toIntOrNull() ?: return false
        return first == 100 && second in 64..127
    }

    private fun describeBindFailure(error: Exception, port: Int, transport: String): String {
        val detail = NemPspProtocol.errorName(error)
        return if (detail.contains("EADDRINUSE", ignoreCase = true) || detail.contains("in use", ignoreCase = true)) {
            "Port $transport $port déjà utilisé par une autre application. Fermez-la ou changez de port."
        } else {
            "Écoute $transport impossible sur le port $port : $detail"
        }
    }

    override fun onDestroy() {
        stopServerInternal()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

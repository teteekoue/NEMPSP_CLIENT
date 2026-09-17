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
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.PspButton
import com.example.nempsp.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

/**
 * Service d'arrière-plan de premier plan (Foreground Service) garantissant que
 * le serveur NEMPSP continue d'écouter les touches (WiFi UDP, USB ADB TCP, Bluetooth RFCOMM)
 * et de notifier l'utilisateur même quand il bascule dans PPSSPP !
 */
class NemPspForegroundServerService : Service() {

    companion object {
        const val CHANNEL_ID = "nempsp_server_foreground_channel"
        const val NOTIFICATION_ID = 8989
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val ACTION_START = "com.example.nempsp.ACTION_START_SERVER"
        const val ACTION_STOP = "com.example.nempsp.ACTION_STOP_SERVER"

        // Singleton state observable par l'UI
        private val _serverState = MutableStateFlow(ServerStatus())
        val serverState: StateFlow<ServerStatus> = _serverState.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, NemPspForegroundServerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NemPspForegroundServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
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

            val simPacket = ServerDecodedPacket(
                sequenceNumber = ((_serverState.value.lastDecodedState?.sequenceNumber ?: 0) + 1) % 65536,
                buttonsMask = mask,
                analogX = _serverState.value.analogX,
                analogY = _serverState.value.analogY,
                activeButtons = currentButtons
            )
            onPacketReceivedStatic(simPacket, "Testeur Virtuel Interne")
        }

        fun onPacketReceivedStatic(decoded: ServerDecodedPacket, source: String) {
            val total = _serverState.value.packetsReceived + 1
            _serverState.value = _serverState.value.copy(
                lastDecodedState = decoded,
                lastClientSource = source,
                activeButtons = decoded.activeButtons,
                analogX = decoded.analogX,
                analogY = decoded.analogY,
                packetsReceived = total
            )
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

    private var ppsCounter = 0
    private var totalPackets: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServerInternal()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("En attente de connexion... (WiFi/USB/BT)"))
                startServerInternal()
                return START_STICKY
            }
        }
    }

    private fun startServerInternal() {
        if (_serverState.value.isRunning) return

        val localIp = getDeviceLocalIp()
        _serverState.value = _serverState.value.copy(
            isRunning = true,
            udpPort = 8989,
            tcpPort = 8989,
            localIpAddress = localIp
        )

        LogRepository.info("SERVEUR-SERVICE", "Serveur actif en arrière-plan (IP: $localIp)", ConnectionMode.WIFI_UDP)

        // 1. Écouteur UDP (WiFi Local + Discovery Broadcast)
        udpJob = scope.launch(Dispatchers.IO) {
            runUdpListener(8989)
        }

        // 2. Écouteur TCP (USB ADB / Filaire)
        tcpJob = scope.launch(Dispatchers.IO) {
            runTcpListener(8989)
        }

        // 3. Écouteur Bluetooth RFCOMM SPP
        btJob = scope.launch(Dispatchers.IO) {
            runBluetoothListener()
        }

        // 4. Calculateur de débit et mise à jour de la notification
        statsJob = scope.launch(Dispatchers.Default) {
            while (isActive && _serverState.value.isRunning) {
                kotlinx.coroutines.delay(1000)
                val currentPps = ppsCounter
                ppsCounter = 0
                _serverState.value = _serverState.value.copy(
                    packetsPerSecond = currentPps,
                    packetsReceived = totalPackets
                )

                // Mise à jour notification si active
                updateNotification("Actif | $currentPps PPS | ${_serverState.value.lastClientSource}")
            }
        }
    }

    private fun stopServerInternal() {
        _serverState.value = _serverState.value.copy(isRunning = false, activeConnectionsCount = 0)
        udpJob?.cancel()
        tcpJob?.cancel()
        btJob?.cancel()
        statsJob?.cancel()

        try { udpSocket?.close() } catch (_: Exception) {}
        try { tcpServerSocket?.close() } catch (_: Exception) {}
        try { btServerSocket?.close() } catch (_: Exception) {}

        udpSocket = null
        tcpServerSocket = null
        btServerSocket = null

        LogRepository.warn("SERVEUR-SERVICE", "Serveur arrêté.", ConnectionMode.WIFI_UDP)
    }

    private suspend fun runUdpListener(port: Int) = withContext(Dispatchers.IO) {
        try {
            udpSocket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port))
            }
            LogRepository.info("SERVEUR-UDP", "Écoute UDP 0.0.0.0:$port active", ConnectionMode.WIFI_UDP)

            val buffer = ByteArray(512)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive && _serverState.value.isRunning) {
                udpSocket?.receive(packet)

                val data = packet.data.copyOf(packet.length)
                val clientAddress = "${packet.address.hostAddress}:${packet.port}"

                val str = String(data, Charsets.US_ASCII)
                if (str.startsWith("NEMPSP_DISCOVERY_REQUEST")) {
                    val reply = "NEMPSP_SERVER:CHROMEBOOK_ACTIVE".toByteArray(Charsets.US_ASCII)
                    val replyPacket = DatagramPacket(reply, reply.size, packet.address, packet.port)
                    udpSocket?.send(replyPacket)
                    LogRepository.info("SERVEUR-UDP", "Discovery reçu de $clientAddress -> Réponse OK", ConnectionMode.WIFI_UDP)
                    continue
                }

                NemPspPacketDecoder.decode(data)?.let { decoded ->
                    handleDecodedPacket(decoded, "WiFi UDP ($clientAddress)")
                }
            }
        } catch (e: Exception) {
            if (_serverState.value.isRunning) {
                LogRepository.error("SERVEUR-UDP", "Erreur UDP: ${e.message}", ConnectionMode.WIFI_UDP)
            }
        }
    }

    private suspend fun runTcpListener(port: Int) = withContext(Dispatchers.IO) {
        try {
            tcpServerSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port))
            }
            LogRepository.info("SERVEUR-TCP", "Écoute TCP 0.0.0.0:$port active (USB ADB)", ConnectionMode.USB_ADB)

            while (isActive && _serverState.value.isRunning) {
                val clientSocket = tcpServerSocket?.accept() ?: break
                _serverState.value = _serverState.value.copy(
                    activeConnectionsCount = _serverState.value.activeConnectionsCount + 1,
                    lastClientSource = "USB TCP (${clientSocket.inetAddress.hostAddress})"
                )
                LogRepository.info("SERVEUR-TCP", "Nouveau client connecté via USB ADB", ConnectionMode.USB_ADB)

                scope.launch(Dispatchers.IO) {
                    handleTcpStream(clientSocket)
                }
            }
        } catch (e: Exception) {
            if (_serverState.value.isRunning) {
                LogRepository.error("SERVEUR-TCP", "Erreur TCP: ${e.message}", ConnectionMode.USB_ADB)
            }
        }
    }

    private fun handleTcpStream(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            val inputStream: InputStream = socket.getInputStream()
            val buf = ByteArray(128)
            val packetBuf = ByteArray(9)
            var pIndex = 0

            while (socket.isConnected && !socket.isClosed && _serverState.value.isRunning) {
                val bytesRead = inputStream.read(buf)
                if (bytesRead <= 0) break

                for (i in 0 until bytesRead) {
                    val b = buf[i]
                    if (pIndex == 0 && b != 0x4E.toByte()) continue
                    if (pIndex == 1 && b != 0x4D.toByte()) {
                        pIndex = 0
                        continue
                    }
                    packetBuf[pIndex++] = b
                    if (pIndex == 9) {
                        NemPspPacketDecoder.decode(packetBuf)?.let { decoded ->
                            handleDecodedPacket(decoded, "USB ADB (${socket.inetAddress.hostAddress})")
                        }
                        pIndex = 0
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
            _serverState.value = _serverState.value.copy(
                activeConnectionsCount = (_serverState.value.activeConnectionsCount - 1).coerceAtLeast(0)
            )
            LogRepository.info("SERVEUR-TCP", "Client USB déconnecté", ConnectionMode.USB_ADB)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun runBluetoothListener() = withContext(Dispatchers.IO) {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btAdapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

        if (btAdapter == null || !btAdapter.isEnabled) {
            LogRepository.warn("SERVEUR-BT", "Bluetooth désactivé ou absent sur l'appareil", ConnectionMode.BLUETOOTH)
            return@withContext
        }

        try {
            btServerSocket = btAdapter.listenUsingRfcommWithServiceRecord("NEMPSP_GAMEPAD_SERVER", SPP_UUID)
            LogRepository.info("SERVEUR-BT", "Écoute Bluetooth RFCOMM SPP prête", ConnectionMode.BLUETOOTH)

            while (isActive && _serverState.value.isRunning) {
                val clientSocket: BluetoothSocket = btServerSocket?.accept() ?: break
                LogRepository.info("SERVEUR-BT", "Client Bluetooth connecté: ${clientSocket.remoteDevice.name}", ConnectionMode.BLUETOOTH)
                _serverState.value = _serverState.value.copy(
                    activeConnectionsCount = _serverState.value.activeConnectionsCount + 1,
                    lastClientSource = "Bluetooth (${clientSocket.remoteDevice.name})"
                )

                scope.launch(Dispatchers.IO) {
                    handleBluetoothStream(clientSocket)
                }
            }
        } catch (e: Exception) {
            if (_serverState.value.isRunning) {
                LogRepository.warn("SERVEUR-BT", "Écoute Bluetooth arrêtée: ${e.message}", ConnectionMode.BLUETOOTH)
            }
        }
    }

    private fun handleBluetoothStream(socket: BluetoothSocket) {
        try {
            val inputStream: InputStream = socket.inputStream
            val buf = ByteArray(128)
            val packetBuf = ByteArray(9)
            var pIndex = 0

            while (socket.isConnected && _serverState.value.isRunning) {
                val bytesRead = inputStream.read(buf)
                if (bytesRead <= 0) break

                for (i in 0 until bytesRead) {
                    val b = buf[i]
                    if (pIndex == 0 && b != 0x4E.toByte()) continue
                    if (pIndex == 1 && b != 0x4D.toByte()) {
                        pIndex = 0
                        continue
                    }
                    packetBuf[pIndex++] = b
                    if (pIndex == 9) {
                        NemPspPacketDecoder.decode(packetBuf)?.let { decoded ->
                            handleDecodedPacket(decoded, "Bluetooth (${socket.remoteDevice.name})")
                        }
                        pIndex = 0
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
            _serverState.value = _serverState.value.copy(
                activeConnectionsCount = (_serverState.value.activeConnectionsCount - 1).coerceAtLeast(0)
            )
            LogRepository.info("SERVEUR-BT", "Client Bluetooth déconnecté", ConnectionMode.BLUETOOTH)
        }
    }

    private fun handleDecodedPacket(decoded: ServerDecodedPacket, source: String) {
        totalPackets++
        ppsCounter++
        onPacketReceivedStatic(decoded, source)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Serveur Manette PPSSPP (NEMPSP)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient le récepteur actif en arrière-plan pendant que PPSSPP tourne"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, NemPspForegroundServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎮 NEMPSP Serveur Manette Actif")
            .setContentText("Prêt pour PPSSPP : $statusText")
            .setSubText("Port 8989 (WiFi / USB / Bluetooth)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Arrêter Serveur", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun getDeviceLocalIp(): String {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }

            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val element = interfaces.nextElement()
                val addresses = element.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServerInternal()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

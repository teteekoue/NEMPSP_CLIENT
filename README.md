# 🎮 NEMPSP - Manette & Serveur 2-en-1 pour PPSSPP (Chromebook, PC & Android)

> L'application intègre désormais **les deux rôles dans le même APK** :
> 1. **Mode Manette (Client)** : Transforme votre smartphone en manette PSP tactile avec haptique réaliste et touches redimensionnables.
> 2. **Mode Récepteur (Serveur)** : Tourne sur votre Chromebook, PC ou tablette pour écouter les touches en WiFi (UDP), USB ADB (TCP) et Bluetooth, puis les afficher en direct et les transmettre à PPSSPP.
>
> Au démarrage, une fenêtre élégante vous propose de choisir le rôle de l'appareil (avec option de mémorisation modifiable à tout instant).

---

## 📑 Sommaire
1. [Vue d'ensemble de l'écosystème NEMPSP (Application 2-en-1)](#1-vue-densemble-de-lécosystème-nempsp)
2. [Spécification Complète du Protocole Binaire (9 Octets)](#2-spécification-complète-du-protocole-binaire-9-octets)
3. [Modes de Communication Pris en Charge](#3-modes-de-communication-pris-en-charge)
4. [Architecture & Conception de l'APK Serveur](#4-architecture--conception-de-lapk-serveur)
5. [Méthodes d'Injection des Touches dans PPSSPP](#5-méthodes-dinjection-des-touches-dans-ppsspp)
6. [Code Source Clé en Main pour l'APK Serveur (Kotlin)](#6-code-source-clé-en-main-pour-lapk-serveur-kotlin)
7. [Configuration de PPSSPP pour la Reconnaissance des Touches](#7-configuration-de-ppsspp-pour-la-reconnaissance-des-touches)
8. [Guide de Dépannage & Optimisation de la Latence](#8-guide-de-dépannage--optimisation-de-la-latence)

---

## 1. Vue d'ensemble de l'écosystème NEMPSP

```
┌─────────────────────────────────────────────────────────┐
│              SMARTPHONE (CLIENT NEMPSP)                 │
│  - Interface tactile PSP (D-Pad, △○✕□, L/R, Analog)     │
│  - Redimensionnement libre (50% - 180%)                 │
│  - Haptique & animations physiques au toucher           │
│  - Émetteur UDP (WiFi) / TCP (USB) / RFCOMM (Bluetooth) │
└────────────────────────────┬────────────────────────────┘
                             │  Paquets binaires 60-120 Hz
                             │  (9 octets par trame)
                             ▼
┌─────────────────────────────────────────────────────────┐
│            CHROMEBOOK / APPAREIL CIBLE                  │
│  ┌───────────────────────────────────────────────────┐  │
│  │               APK SERVEUR NEMPSP                  │  │
│  │  - Serveur UDP / TCP / Bluetooth RFCOMM           │  │
│  │  - Décodeur de paquets & vérification Checksum    │  │
│  │  - Injecteur d'événements (HID Gamepad / UInput)  │  │
│  └─────────────────────────┬─────────────────────────┘  │
│                            │ Événements Manette Virtuelle│
│                            ▼                            │
│  ┌───────────────────────────────────────────────────┐  │
│  │           ÉMULATEUR PSP (ex: PPSSPP)              │  │
│  │  - Mappé directement sur manette virtuelle        │  │
│  │  - Latence ultra-basse (< 2-5 ms)                 │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

L'application **NEMPSP Client** transforme votre smartphone Android en une manette PSP haut de gamme avec retour haptique réaliste, redimensionnement dynamique des touches, et latence ultra-faible.

Pour contrôler un émulateur PSP (**PPSSPP**) tournant sur Chromebook, tablette ou Android TV, une entité **Serveur** doit recevoir ces paquets et les convertir en entrées joystick/clavier reconnues par le système.

---

## 2. Spécification Complète du Protocole Binaire (9 Octets)

Pour garantir une latence minimale (< 2 ms) et éviter le surcoût de sérialisation JSON en pleine partie, le client transmet un paquet binaire compact de **9 octets** à une cadence de **60 Hz** (toutes les 16 ms) ou sur événement tactile direct.

### Structure du paquet binaire (`toBinaryPacket`) :

| Offset | Champ | Type | Description |
| :--- | :--- | :--- | :--- |
| `[0]` | **Magic Byte 1** | `0x4E` ('N') | Marqueur fixe d'identification du protocole |
| `[1]` | **Magic Byte 2** | `0x4D` ('M') | Marqueur fixe d'identification du protocole |
| `[2..3]` | **Sequence Number** | `UInt16` (LE) | Compteur incrémenté (0 à 65535) pour ordonnancer les paquets |
| `[4..5]` | **Buttons Mask** | `UInt16` (LE) | Masque binaire des 16 touches de la PSP (1 = pressé, 0 = relâché) |
| `[6]` | **Analog X** | `UInt8` (0..255) | Axe horizontal : `0` = gauche max, `128` = centre, `255` = droite max |
| `[7]` | **Analog Y** | `UInt8` (0..255) | Axe vertical : `0` = haut max, `128` = centre, `255` = bas max |
| `[8]` | **XOR Checksum** | `UInt8` | Checksum XOR de contrôle d'intégrité sur les octets `0` à `7` |

### Masque des Touches (`Buttons Mask`) :

Le masque binaire 16 bits encode l'état exact des boutons :

```kotlin
val UP       = 1 shl 0   // 0x0001 (Croix Haut)
val RIGHT    = 1 shl 1   // 0x0002 (Croix Droite)
val DOWN     = 1 shl 2   // 0x0004 (Croix Bas)
val LEFT     = 1 shl 3   // 0x0008 (Croix Gauche)

val TRIANGLE = 1 shl 4   // 0x0010 (Touche Triangle △)
val CIRCLE   = 1 shl 5   // 0x0020 (Touche Rond ○)
val CROSS    = 1 shl 6   // 0x0040 (Touche Croix ✕)
val SQUARE   = 1 shl 7   // 0x0080 (Touche Carré □)

val L        = 1 shl 8   // 0x0100 (Gâchette d'épaule Gauche L)
val R        = 1 shl 9   // 0x0200 (Gâchette d'épaule Droite R)

val SELECT   = 1 shl 10  // 0x0400 (Touche SELECT)
val START    = 1 shl 11  // 0x0800 (Touche START)
val HOME     = 1 shl 12  // 0x1000 (Touche PS / HOME)
val VOL_DOWN = 1 shl 13  // 0x2000 (Volume Moins)
val VOL_UP   = 1 shl 14  // 0x4000 (Volume Plus)
val NOTE     = 1 shl 15  // 0x8000 (Touche Musique ♪)
```

### Formule du Checksum XOR :
```kotlin
var checksum: Byte = 0
for (i in 0 until 8) {
    checksum = (checksum.toInt() xor packet[i].toInt()).toByte()
}
// packet[8] doit être égal à checksum
```

---

## 3. Modes de Communication Pris en Charge

L'APK Serveur doit écouter sur trois canaux selon la méthode de connexion choisie par le joueur :

### A. Mode WiFi Local (UDP) - Port par défaut : `8989`
- **Auto-Discovery Broadcast** :
  - Le client envoie sur l'adresse de broadcast `255.255.255.255:8989` le message ASCII : `NEMPSP_DISCOVERY_REQUEST`.
  - Le serveur doit répondre immédiatement à l'expéditeur : `NEMPSP_SERVER:CHROMEBOOK_ACTIVE`.
- **Flux de données** : Paquets UDP directs de 9 octets.

### B. Mode Filaire USB (Tunnel ADB Reverse) - Port `8989`
- Le client se connecte en TCP standard vers `127.0.0.1:8989`.
- Sur le Chromebook / PC, la redirection de port est configurée via :
  ```bash
  adb reverse tcp:8989 tcp:8989
  ```
- **Flux de données** : Flux TCP streamé (paquets de 9 octets continus).

### C. Mode Bluetooth (RFCOMM / SPP)
- **UUID Service SPP standard** :
  `00001101-0000-1000-8000-00805F9B34FB`
- **Nom du service** : `"NEMPSP_GAMEPAD_SERVER"`
- Le serveur écoute via `BluetoothAdapter.listenUsingRfcommWithServiceRecord(...)`.

---

## 4. Architecture & Conception de l'APK Serveur

Pour créer une application **NEMPSP Server (APK)** propre et autonome, voici les composants requis :

```
nempsp-server/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/example/nempspserver/
│   │       ├── MainActivity.kt               (Interface d'état & configuration)
│   │       ├── service/
│   │       │   └── NemPspServerService.kt    (Foreground Service réseau & input)
│   │       ├── network/
│   │       │   ├── UdpGamepadServer.kt       (Écouteur UDP 8989 + Discovery)
│   │       │   ├── TcpGamepadServer.kt       (Écouteur TCP ADB 8989)
│   │       │   └── BluetoothSppServer.kt     (Écouteur RFCOMM Bluetooth)
│   │       ├── parser/
│   │       │   └── PacketParser.kt           (Décodage binaire 9 octets)
│   │       └── input/
│   │           ├── GamepadInputInjector.kt   (Interface commune d'injection)
│   │           ├── UinputGamepadInjector.kt  (Via /dev/uinput ou Root/Rootless)
│   │           └── AccessibilityInjector.kt  (Via AccessibilityService)
```

### Permissions à déclarer dans `AndroidManifest.xml` :
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Réseau WiFi et Local -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />

    <!-- Bluetooth SPP / RFCOMM -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

    <!-- Service d'arrière-plan pour continuer à recevoir pendant que PPSSPP tourne -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:label="NEMPSP Server"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.Material3.Dark">
        
        <service
            android:name=".service.NemPspServerService"
            android:foregroundServiceType="connectedDevice"
            android:exported="false" />

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 5. Méthodes d'Injection des Touches dans PPSSPP

Pour que PPSSPP réagisse aux commandes reçues depuis le serveur Android, il existe 3 approches techniques :

### Option 1 : Périphérique Manette Virtuelle `/dev/uinput` (La plus performante ⚡)
- Sur Linux/Chromebook (conteneur Crostini ou Android avec permissions uinput) :
- Crée un véritable périphérique HID Gamepad dans le noyau Linux.
- **Avantage** : Reconnue nativement par PPSSPP comme une manette Xbox/PlayStation physique sans aucun mapping complexe.
- **Axes** : `ABS_X`, `ABS_Y` (stick analogique).
- **Boutons** : `BTN_A` (Cross), `BTN_B` (Circle), `BTN_X` (Square), `BTN_Y` (Triangle), `BTN_TL` (L), `BTN_TR` (R), `BTN_START`, `BTN_SELECT`.

### Option 2 : Android `AccessibilityService` ou `INJECT_EVENTS`
- Injecte directement les `KeyEvent` Android (`KEYCODE_BUTTON_A`, `KEYCODE_DPAD_UP`, etc.).
- Idéal si l'APK Serveur et PPSSPP tournent sur le même système Android/Chromebook.

### Option 3 : Bluetooth HID Device Profile (`BluetoothHidDevice`)
- Disponible depuis Android 9 (API 28+).
- L'APK Serveur se déclare comme une vraie manette Bluetooth standard auprès du Chromebook ou du PC hôte !

---

## 6. Code Source Clé en Main pour l'APK Serveur (Kotlin)

### A. Décodeur du Protocole (`PacketParser.kt`)
```kotlin
package com.example.nempspserver.parser

data class DecodedState(
    val sequence: Int,
    val mask: Int,
    val analogX: Float, // -1.0f à +1.0f
    val analogY: Float, // -1.0f à +1.0f
    val activeButtons: List<String>
)

object PacketParser {
    private val BUTTON_LABELS = arrayOf(
        "UP", "RIGHT", "DOWN", "LEFT",
        "TRIANGLE", "CIRCLE", "CROSS", "SQUARE",
        "L", "R", "SELECT", "START",
        "HOME", "VOL-", "VOL+", "NOTE"
    )

    fun parse(packet: ByteArray): DecodedState? {
        if (packet.size < 9) return null
        
        // Vérification Magic 'N' 'M'
        if (packet[0] != 0x4E.toByte() || packet[1] != 0x4D.toByte()) {
            return null
        }

        // Vérification Checksum XOR
        var calcChecksum: Byte = 0
        for (i in 0 until 8) {
            calcChecksum = (calcChecksum.toInt() xor packet[i].toInt()).toByte()
        }
        if (calcChecksum != packet[8]) {
            return null // Paquet corrompu ignoré
        }

        // Lecture Little Endian
        val seq = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
        val mask = (packet[4].toInt() and 0xFF) or ((packet[5].toInt() and 0xFF) shl 8)

        // Conversion Analogique (128 = centre)
        val rawX = packet[6].toInt() and 0xFF
        val rawY = packet[7].toInt() and 0xFF
        val ax = (rawX - 128) / 127.0f
        val ay = (rawY - 128) / 127.0f

        val pressedButtons = mutableListOf<String>()
        for (i in 0 until 16) {
            if ((mask and (1 shl i)) != 0) {
                pressedButtons.add(BUTTON_LABELS[i])
            }
        }

        return DecodedState(
            sequence = seq,
            mask = mask,
            analogX = ax.coerceIn(-1f, 1f),
            analogY = ay.coerceIn(-1f, 1f),
            activeButtons = pressedButtons
        )
    }
}
```

### B. Serveur Réseau UDP & Broadcast Discovery (`UdpGamepadServer.kt`)
```kotlin
package com.example.nempspserver.network

import com.example.nempspserver.parser.DecodedState
import com.example.nempspserver.parser.PacketParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class UdpGamepadServer(
    private val port: Int = 8989,
    private val onStateReceived: (DecodedState) -> Unit
) {
    private var socket: DatagramSocket? = null
    private var isRunning = false

    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port))
            }
            isRunning = true
            val buffer = ByteArray(512)

            while (isRunning && isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket?.receive(packet)

                val data = packet.data.copyOf(packet.length)

                // 1. Réponse à la découverte automatique Broadcast
                val message = String(data, Charsets.US_ASCII)
                if (message.startsWith("NEMPSP_DISCOVERY_REQUEST")) {
                    val reply = "NEMPSP_SERVER:CHROMEBOOK_ACTIVE".toByteArray(Charsets.US_ASCII)
                    val replyPacket = DatagramPacket(reply, reply.size, packet.address, packet.port)
                    socket?.send(replyPacket)
                    continue
                }

                // 2. Décodage du paquet de jeu 9 octets
                PacketParser.parse(data)?.let { state ->
                    onStateReceived(state)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            stop()
        }
    }

    fun stop() {
        isRunning = false
        socket?.close()
        socket = null
    }
}
```

### C. Serveur Bluetooth RFCOMM SPP (`BluetoothSppServer.kt`)
```kotlin
package com.example.nempspserver.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.example.nempspserver.parser.DecodedState
import com.example.nempspserver.parser.PacketParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

class BluetoothSppServer(
    private val bluetoothAdapter: BluetoothAdapter,
    private val onStateReceived: (DecodedState) -> Unit
) {
    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val SERVICE_NAME = "NEMPSP_GAMEPAD_SERVER"
    }

    private var serverSocket: BluetoothServerSocket? = null
    private var isRunning = false

    @SuppressLint("MissingPermission")
    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
            isRunning = true

            while (isRunning && isActive) {
                val clientSocket: BluetoothSocket = serverSocket?.accept() ?: break
                handleClient(clientSocket)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            stop()
        }
    }

    private fun handleClient(socket: BluetoothSocket) {
        val input: InputStream = socket.inputStream
        val buffer = ByteArray(64)
        val packetAcc = ByteArray(9)
        var accIndex = 0

        try {
            while (isRunning) {
                val read = input.read(buffer)
                if (read <= 0) break

                for (i in 0 until read) {
                    val b = buffer[i]
                    if (accIndex == 0 && b != 0x4E.toByte()) continue
                    if (accIndex == 1 && b != 0x4D.toByte()) {
                        accIndex = 0
                        continue
                    }
                    packetAcc[accIndex++] = b
                    if (accIndex == 9) {
                        PacketParser.parse(packetAcc)?.let { state ->
                            onStateReceived(state)
                        }
                        accIndex = 0
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            socket.close()
        }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
    }
}
```

### D. Foreground Service Principal (`NemPspServerService.kt`)
```kotlin
package com.example.nempspserver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.nempspserver.network.BluetoothSppServer
import com.example.nempspserver.network.UdpGamepadServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NemPspServerService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var udpServer: UdpGamepadServer? = null
    private var btServer: BluetoothSppServer? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        startNetworkListeners()
    }

    private fun startNetworkListeners() {
        // 1. Démarrer le serveur UDP (WiFi & Découverte)
        udpServer = UdpGamepadServer(port = 8989) { state ->
            // Transmettre l'état au contrôleur de jeu virtuel
            GamepadDispatcher.dispatch(state)
        }
        scope.launch { udpServer?.start() }

        // 2. Démarrer le serveur Bluetooth RFCOMM si disponible
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btAdapter = btManager?.adapter
        if (btAdapter != null && btAdapter.isEnabled) {
            btServer = BluetoothSppServer(btAdapter) { state ->
                GamepadDispatcher.dispatch(state)
            }
            scope.launch { btServer?.start() }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "nempsp_server_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "NEMPSP Serveur Actif", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NEMPSP Serveur Manette")
            .setContentText("Écoute active sur le port 8989 (WiFi / USB / Bluetooth)")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        udpServer?.stop()
        btServer?.stop()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

## 7. Configuration de PPSSPP pour la Reconnaissance des Touches

Une fois l'APK Serveur lancé, suivez cette procédure simple dans **PPSSPP** pour mapper les touches :

1. **Ouvrez PPSSPP** sur le Chromebook / PC / Android TV.
2. Allez dans **Paramètres** (`Settings`) > **Commandes** (`Controls`).
3. Cliquez sur **Affecter les commandes** (`Control Mapping`).
4. Pour chaque touche PSP affichée à l'écran :
   - Cliquez sur le bouton `+` en face de la commande (ex: `Cross / ✕`).
   - Appuyez sur la touche correspondante sur votre smartphone dans **NEMPSP Client**.
   - PPSSPP détecte instantanément l'entrée et l'assigne au bouton !
5. Répétez pour :
   - `△ (Triangle)`, `○ (Rond)`, `✕ (Croix)`, `□ (Carré)`
   - `Haut`, `Bas`, `Gauche`, `Droite` (D-Pad)
   - `L` et `R` (Gâchettes)
   - `Start` et `Select`
   - `Stick Analogique` (inclinez le stick dans la direction demandée)

---

## 8. Guide de Dépannage & Optimisation de la Latence

| Problème rencontré | Cause probable | Solution recommandée |
| :--- | :--- | :--- |
| **"Non connecté / Recherche..." en WiFi** | Isolation AP sur la box WiFi ou Pare-feu actif | Assurez-vous que les deux appareils sont sur le même réseau WiFi (2.4 GHz ou 5 GHz). Désactivez "l'isolation des clients WiFi" sur le routeur, ou utilisez l'IP manuelle affichée par le serveur. |
| **Latence perçue en WiFi (> 25 ms)** | Interférences WiFi | Privilégiez la bande **5 GHz** ou passez en **Mode Filaire USB** via `adb reverse tcp:8989 tcp:8989` (latence < 1 ms). |
| **Bluetooth non détecté** | Appareil non appairé | Appairez le smartphone et le Chromebook dans les paramètres Android/ChromeOS avant de lancer la connexion dans l'application. |
| **Touches non reconnues dans PPSSPP** | Permission uinput ou Accessibility inactive | Vérifiez que le service du serveur est bien actif en premier plan et que les permissions requises ont été accordées dans les paramètres d'accessibilité. |

---

## 📄 Licence
Ce projet fait partie intégrante de la suite **NEMPSP Pro Controller**. Développé avec Jetpack Compose, Material 3, Coroutines Kotlin et sockets réseau haute performance.

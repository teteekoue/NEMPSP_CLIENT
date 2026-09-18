package com.example.nempsp.repository

import android.content.Context
import com.example.nempsp.model.ConnectionMode

/**
 * Mémorise les paramètres de connexion entre deux sessions.
 *
 * Avant cela, le panneau de connexion repartait systématiquement de « 192.168.1.100 » et du port
 * par défaut à chaque ouverture : il fallait tout retaper, et le mode choisi n'était pas conservé.
 */
class ConnectionPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "nempsp_connection_prefs"
        const val DEFAULT_PORT = 8989
        private const val DEFAULT_IP = "192.168.1.100"
    }

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverIp: String
        get() = prefs.getString("server_ip", DEFAULT_IP) ?: DEFAULT_IP
        set(value) {
            val trimmed = value.trim()
            if (trimmed.isNotEmpty()) prefs.edit().putString("server_ip", trimmed).apply()
        }

    var serverPort: Int
        get() = prefs.getInt("server_port", DEFAULT_PORT)
        set(value) {
            if (value in 1..65535) prefs.edit().putInt("server_port", value).apply()
        }

    var usbPort: Int
        get() = prefs.getInt("usb_port", DEFAULT_PORT)
        set(value) {
            if (value in 1..65535) prefs.edit().putInt("usb_port", value).apply()
        }

    var bluetoothAddress: String?
        get() = prefs.getString("bluetooth_address", null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove("bluetooth_address") else putString("bluetooth_address", value)
            }.apply()
        }

    var connectionMode: ConnectionMode
        get() {
            val name = prefs.getString("connection_mode", null) ?: return ConnectionMode.WIFI_UDP
            return runCatching { ConnectionMode.valueOf(name) }.getOrDefault(ConnectionMode.WIFI_UDP)
        }
        set(value) {
            prefs.edit().putString("connection_mode", value.name).apply()
        }

    /** Port d'écoute du récepteur (mode Serveur). */
    var serverListenPort: Int
        get() = prefs.getInt("listen_port", DEFAULT_PORT)
        set(value) {
            if (value in 1..65535) prefs.edit().putInt("listen_port", value).apply()
        }
}

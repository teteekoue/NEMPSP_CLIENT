package com.example.nempsp.model

enum class ConnectionMode(val displayName: String, val iconName: String) {
    WIFI_UDP("WiFi Réseau Local", "wifi"),
    BLUETOOTH("Bluetooth Sans-fil", "bluetooth"),
    USB_ADB("Câble USB (ADB/Loopback)", "usb")
}

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("Déconnecté"),
    SCANNING("Recherche serveur..."),
    CONNECTING("Connexion en cours..."),
    CONNECTED("Connecté"),
    ERROR("Erreur de connexion")
}

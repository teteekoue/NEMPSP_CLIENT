package com.example.nempsp.model

enum class AppOperationMode(
    val title: String,
    val subtitle: String,
    val badge: String
) {
    CONTROLLER_CLIENT(
        title = "Mode Manette (Client)",
        subtitle = "Transforme ce smartphone en manette tactile PSP réactive (D-Pad, Touches, Stick, Gâchettes)",
        badge = "CLIENT / GAMEPAD"
    ),
    CHROMEBOOK_SERVER(
        title = "Mode Récepteur (Serveur)",
        subtitle = "Reçoit les touches transmises par WiFi, USB ADB ou Bluetooth et les réinjecte vers PPSSPP",
        badge = "SERVEUR / HOST"
    )
}

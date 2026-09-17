package com.example.nempsp.server

import com.example.nempsp.model.PspButton

data class ServerStatus(
    val isRunning: Boolean = false,
    val udpPort: Int = 8989,
    val tcpPort: Int = 8989,
    val localIpAddress: String = "127.0.0.1",
    val activeConnectionsCount: Int = 0,
    val lastClientSource: String = "Aucun",
    val packetsReceived: Long = 0,
    val packetsPerSecond: Int = 0,
    val lastDecodedState: ServerDecodedPacket? = null,
    val activeButtons: List<PspButton> = emptyList(),
    val analogX: Float = 0.0f,
    val analogY: Float = 0.0f
)

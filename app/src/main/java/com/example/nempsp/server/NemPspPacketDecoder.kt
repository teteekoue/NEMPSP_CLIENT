package com.example.nempsp.server

import com.example.nempsp.model.PspButton

data class ServerDecodedPacket(
    val sequenceNumber: Int,
    val buttonsMask: Int,
    val analogX: Float, // -1.0f (left) to +1.0f (right)
    val analogY: Float, // -1.0f (up) to +1.0f (down)
    val activeButtons: List<PspButton>,
    val timestamp: Long = System.currentTimeMillis()
)

object NemPspPacketDecoder {

    /**
     * Decodes a 9-byte NEMPSP binary packet:
     * [0]: 0x4E ('N')
     * [1]: 0x4D ('M')
     * [2..3]: Sequence UInt16 LE
     * [4..5]: Buttons Mask UInt16 LE
     * [6]: Analog X (0..255, 128 = center)
     * [7]: Analog Y (0..255, 128 = center)
     * [8]: XOR Checksum
     */
    fun decode(data: ByteArray, length: Int = data.size): ServerDecodedPacket? {
        if (length < 9) return null

        // Check magic bytes 'N' 'M'
        if (data[0] != 0x4E.toByte() || data[1] != 0x4D.toByte()) {
            return null
        }

        // Verify XOR Checksum
        var checksum: Byte = 0
        for (i in 0 until 8) {
            checksum = (checksum.toInt() xor data[i].toInt()).toByte()
        }
        if (checksum != data[8]) {
            return null // Checksum mismatch
        }

        val seq = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
        val mask = (data[4].toInt() and 0xFF) or ((data[5].toInt() and 0xFF) shl 8)

        val rawX = data[6].toInt() and 0xFF
        val rawY = data[7].toInt() and 0xFF

        val ax = ((rawX - 128) / 127.0f).coerceIn(-1.0f, 1.0f)
        val ay = ((rawY - 128) / 127.0f).coerceIn(-1.0f, 1.0f)

        val active = mutableListOf<PspButton>()
        if ((mask and (1 shl 0)) != 0) active.add(PspButton.UP)
        if ((mask and (1 shl 1)) != 0) active.add(PspButton.RIGHT)
        if ((mask and (1 shl 2)) != 0) active.add(PspButton.DOWN)
        if ((mask and (1 shl 3)) != 0) active.add(PspButton.LEFT)

        if ((mask and (1 shl 4)) != 0) active.add(PspButton.TRIANGLE)
        if ((mask and (1 shl 5)) != 0) active.add(PspButton.CIRCLE)
        if ((mask and (1 shl 6)) != 0) active.add(PspButton.CROSS)
        if ((mask and (1 shl 7)) != 0) active.add(PspButton.SQUARE)

        if ((mask and (1 shl 8)) != 0) active.add(PspButton.L)
        if ((mask and (1 shl 9)) != 0) active.add(PspButton.R)

        if ((mask and (1 shl 10)) != 0) active.add(PspButton.SELECT)
        if ((mask and (1 shl 11)) != 0) active.add(PspButton.START)
        if ((mask and (1 shl 12)) != 0) active.add(PspButton.HOME)
        if ((mask and (1 shl 13)) != 0) active.add(PspButton.VOL_DOWN)
        if ((mask and (1 shl 14)) != 0) active.add(PspButton.VOL_UP)
        if ((mask and (1 shl 15)) != 0) active.add(PspButton.NOTE)

        return ServerDecodedPacket(
            sequenceNumber = seq,
            buttonsMask = mask,
            analogX = ax,
            analogY = ay,
            activeButtons = active
        )
    }
}

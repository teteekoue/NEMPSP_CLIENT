package com.example.nempsp.model

data class GamepadState(
    val buttonsMask: Int = 0,
    val analogX: Float = 0f, // -1.0 to +1.0
    val analogY: Float = 0f, // -1.0 to +1.0
    val sequenceNumber: Int = 0
) {
    val activeButtons: List<PspButton>
        get() = PspButton.fromMask(buttonsMask)

    fun isPressed(button: PspButton): Boolean = (buttonsMask and button.mask) != 0

    fun withButton(button: PspButton, pressed: Boolean): GamepadState {
        val newMask = if (pressed) {
            buttonsMask or button.mask
        } else {
            buttonsMask and button.mask.inv()
        }
        return copy(buttonsMask = newMask)
    }

    fun withAnalog(x: Float, y: Float): GamepadState {
        return copy(
            analogX = x.coerceIn(-1.0f, 1.0f),
            analogY = y.coerceIn(-1.0f, 1.0f)
        )
    }

    /**
     * Compact 9-byte binary packet for ultra-low latency UDP / USB / Bluetooth gaming.
     * [0..1]: Magic 'N' 'M' (0x4E, 0x4D)
     * [2..3]: Sequence number (UInt16 Little Endian)
     * [4..5]: Button mask (UInt16 Little Endian)
     * [6]: Analog X (0..255, 128 is center)
     * [7]: Analog Y (0..255, 128 is center)
     * [8]: XOR checksum
     */
    fun toBinaryPacket(): ByteArray {
        val packet = ByteArray(9)
        packet[0] = 0x4E.toByte() // 'N'
        packet[1] = 0x4D.toByte() // 'M'
        
        val seq = sequenceNumber and 0xFFFF
        packet[2] = (seq and 0xFF).toByte()
        packet[3] = ((seq shr 8) and 0xFF).toByte()

        val mask = buttonsMask and 0xFFFF
        packet[4] = (mask and 0xFF).toByte()
        packet[5] = ((mask shr 8) and 0xFF).toByte()

        // Map -1.0..1.0 to 0..255 (128 center)
        val axByte = ((analogX * 127f).toInt() + 128).coerceIn(0, 255)
        val ayByte = ((analogY * 127f).toInt() + 128).coerceIn(0, 255)
        packet[6] = axByte.toByte()
        packet[7] = ayByte.toByte()

        var checksum: Byte = 0
        for (i in 0 until 8) {
            checksum = (checksum.toInt() xor packet[i].toInt()).toByte()
        }
        packet[8] = checksum
        return packet
    }

    /**
     * Plain JSON format for debugging and web/custom server support
     */
    fun toJsonString(): String {
        return buildString {
            append("{\"type\":\"input\",")
            append("\"seq\":").append(sequenceNumber).append(",")
            append("\"mask\":").append(buttonsMask).append(",")
            append("\"btns\":[")
            val btns = activeButtons
            btns.forEachIndexed { index, pspButton ->
                append("\"").append(pspButton.label).append("\"")
                if (index < btns.size - 1) append(",")
            }
            append("],")
            append("\"ax\":").append(String.format(java.util.Locale.US, "%.3f", analogX)).append(",")
            append("\"ay\":").append(String.format(java.util.Locale.US, "%.3f", analogY))
            append("}\n")
        }
    }
}

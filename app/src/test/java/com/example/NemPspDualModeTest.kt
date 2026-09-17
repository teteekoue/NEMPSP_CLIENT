package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nempsp.model.AppOperationMode
import com.example.nempsp.model.GamepadState
import com.example.nempsp.model.PspButton
import com.example.nempsp.repository.AppModePreferences
import com.example.nempsp.server.NemPspForegroundServerService
import com.example.nempsp.server.NemPspPacketDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NemPspDualModeTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NEMPSP", appName)
    }

    @Test
    fun `verify gamepad state client encoding and server decoding`() {
        var state = GamepadState()
        state = state.withButton(PspButton.CROSS, true)
        state = state.withButton(PspButton.TRIANGLE, true)
        state = state.withButton(PspButton.L, true)
        state = state.withAnalog(0.75f, -0.25f)

        // 1. Client creates 9-byte packet
        val packet = state.toBinaryPacket()
        assertEquals(9, packet.size)
        assertEquals(0x4E.toByte(), packet[0]) // 'N'
        assertEquals(0x4D.toByte(), packet[1]) // 'M'

        // 2. Server decodes 9-byte packet
        val decoded = NemPspPacketDecoder.decode(packet)
        assertNotNull(decoded)
        assertEquals(true, decoded!!.activeButtons.contains(PspButton.CROSS))
        assertEquals(true, decoded.activeButtons.contains(PspButton.TRIANGLE))
        assertEquals(true, decoded.activeButtons.contains(PspButton.L))
        assertEquals(false, decoded.activeButtons.contains(PspButton.CIRCLE))

        // Analog stick verification (-1..+1)
        assertTrue(decoded.analogX > 0.7f && decoded.analogX < 0.8f)
        assertTrue(decoded.analogY < -0.2f && decoded.analogY > -0.3f)
    }

    @Test
    fun `verify app mode preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AppModePreferences(context)
        prefs.clearRememberedChoice()

        assertEquals(null, prefs.getSavedMode())

        prefs.saveMode(AppOperationMode.CHROMEBOOK_SERVER, rememberChoice = true)
        assertEquals(AppOperationMode.CHROMEBOOK_SERVER, prefs.getSavedMode())

        prefs.saveMode(AppOperationMode.CONTROLLER_CLIENT, rememberChoice = false)
        assertEquals(null, prefs.getSavedMode())
    }

    @Test
    fun `verify background server state and virtual button injection`() {
        // Test simulated injection into server state
        NemPspForegroundServerService.injectSimulatedInput(PspButton.CROSS, true)
        var state = NemPspForegroundServerService.serverState.value
        assertTrue(state.activeButtons.contains(PspButton.CROSS))
        assertTrue(state.packetsReceived > 0)

        NemPspForegroundServerService.injectSimulatedInput(PspButton.CROSS, false)
        state = NemPspForegroundServerService.serverState.value
        assertTrue(!state.activeButtons.contains(PspButton.CROSS))
    }

    @Test
    fun `verify parallel end to end communication controller to server via binary protocol`() {
        // Simulate real gamepad input on Controller Client side
        var clientState = GamepadState()
        clientState = clientState.withButton(PspButton.UP, true)
        clientState = clientState.withButton(PspButton.R, true)
        clientState = clientState.withButton(PspButton.SQUARE, true)
        clientState = clientState.withAnalog(-0.85f, 0.45f)

        // 1. Controller encodes to standard 9-byte packet
        val binaryPacket = clientState.toBinaryPacket()
        assertEquals(9, binaryPacket.size)

        // 2. Transmit via WiFi UDP simulation to server decoder
        val serverReceivedPacketUdp = NemPspPacketDecoder.decode(binaryPacket)
        assertNotNull(serverReceivedPacketUdp)
        NemPspForegroundServerService.onPacketReceivedStatic(serverReceivedPacketUdp!!, "WiFi UDP (192.168.1.50:45678)")

        var serverState = NemPspForegroundServerService.serverState.value
        assertEquals("WiFi UDP (192.168.1.50:45678)", serverState.lastClientSource)
        assertTrue(serverState.activeButtons.contains(PspButton.UP))
        assertTrue(serverState.activeButtons.contains(PspButton.R))
        assertTrue(serverState.activeButtons.contains(PspButton.SQUARE))
        assertTrue(!serverState.activeButtons.contains(PspButton.CROSS))
        assertEquals(-0.85f, serverState.analogX, 0.05f)
        assertEquals(0.45f, serverState.analogY, 0.05f)

        // 3. Transmit via USB ADB TCP stream simulation to server decoder
        clientState = clientState.withButton(PspButton.CIRCLE, true)
        val binaryPacketUsb = clientState.toBinaryPacket()
        val serverReceivedPacketUsb = NemPspPacketDecoder.decode(binaryPacketUsb)
        assertNotNull(serverReceivedPacketUsb)
        NemPspForegroundServerService.onPacketReceivedStatic(serverReceivedPacketUsb!!, "USB ADB (127.0.0.1:8989)")

        serverState = NemPspForegroundServerService.serverState.value
        assertEquals("USB ADB (127.0.0.1:8989)", serverState.lastClientSource)
        assertTrue(serverState.activeButtons.contains(PspButton.CIRCLE))
        assertTrue(serverState.activeButtons.contains(PspButton.UP))

        // 4. Transmit via Bluetooth RFCOMM SPP stream simulation
        clientState = clientState.withButton(PspButton.SELECT, true)
        val binaryPacketBt = clientState.toBinaryPacket()
        val serverReceivedPacketBt = NemPspPacketDecoder.decode(binaryPacketBt)
        assertNotNull(serverReceivedPacketBt)
        NemPspForegroundServerService.onPacketReceivedStatic(serverReceivedPacketBt!!, "Bluetooth (Pixel_Phone_Gamepad)")

        serverState = NemPspForegroundServerService.serverState.value
        assertEquals("Bluetooth (Pixel_Phone_Gamepad)", serverState.lastClientSource)
        assertTrue(serverState.activeButtons.contains(PspButton.SELECT))
    }
}

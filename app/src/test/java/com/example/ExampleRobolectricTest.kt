package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nempsp.model.GamepadState
import com.example.nempsp.model.PspButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NEMPSP", appName)
  }

  @Test
  fun `verify gamepad state binary packet`() {
    var state = GamepadState()
    state = state.withButton(PspButton.CROSS, true)
    state = state.withButton(PspButton.TRIANGLE, true)
    state = state.withAnalog(0.5f, -0.5f)

    assertTrue(state.isPressed(PspButton.CROSS))
    assertTrue(state.isPressed(PspButton.TRIANGLE))

    val packet = state.toBinaryPacket()
    assertEquals(9, packet.size)
    assertEquals(0x4E.toByte(), packet[0]) // 'N'
    assertEquals(0x4D.toByte(), packet[1]) // 'M'
  }
}


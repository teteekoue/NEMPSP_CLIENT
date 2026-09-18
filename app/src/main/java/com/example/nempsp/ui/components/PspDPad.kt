package com.example.nempsp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nempsp.model.LayoutConfig
import com.example.nempsp.model.PspButton
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun PspDPad(
    config: LayoutConfig,
    activeButtons: List<PspButton>,
    onButtonChange: (PspButton, Boolean) -> Unit,
    onFeedback: (isRelease: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = config.dpadScale
    val opacity = config.buttonsOpacity
    val baseSize = 160.dp * scale
    val glowIntensity = config.buttonGlowIntensity

    val isUp = activeButtons.contains(PspButton.UP)
    val isDown = activeButtons.contains(PspButton.DOWN)
    val isLeft = activeButtons.contains(PspButton.LEFT)
    val isRight = activeButtons.contains(PspButton.RIGHT)

    // Track currently pressed buttons inside this gesture session
    var currentPressedUp by remember { mutableStateOf(false) }
    var currentPressedDown by remember { mutableStateOf(false) }
    var currentPressedLeft by remember { mutableStateOf(false) }
    var currentPressedRight by remember { mutableStateOf(false) }

    fun updateDirection(offset: Offset, sizePx: Float) {
        val center = sizePx / 2f
        val dx = offset.x - center
        val dy = offset.y - center
        val dist = sqrt(dx * dx + dy * dy)
        val deadzone = sizePx * 0.12f

        var targetUp = false
        var targetDown = false
        var targetLeft = false
        var targetRight = false

        if (dist >= deadzone) {
            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).let { if (it < 0) it + 360 else it }

            targetRight = angle in 337.5..360.0 || angle in 0.0..22.5 || angle in 22.5..67.5 || angle in 292.5..337.5
            targetDown = angle in 22.5..157.5
            targetLeft = angle in 112.5..247.5
            targetUp = angle in 202.5..337.5
        }

        if (targetUp != currentPressedUp) {
            currentPressedUp = targetUp
            onButtonChange(PspButton.UP, targetUp)
            onFeedback(!targetUp)
        }
        if (targetDown != currentPressedDown) {
            currentPressedDown = targetDown
            onButtonChange(PspButton.DOWN, targetDown)
            onFeedback(!targetDown)
        }
        if (targetLeft != currentPressedLeft) {
            currentPressedLeft = targetLeft
            onButtonChange(PspButton.LEFT, targetLeft)
            onFeedback(!targetLeft)
        }
        if (targetRight != currentPressedRight) {
            currentPressedRight = targetRight
            onButtonChange(PspButton.RIGHT, targetRight)
            onFeedback(!targetRight)
        }
    }

    fun releaseAll() {
        if (currentPressedUp) {
            currentPressedUp = false
            onButtonChange(PspButton.UP, false)
            onFeedback(true)
        }
        if (currentPressedDown) {
            currentPressedDown = false
            onButtonChange(PspButton.DOWN, false)
            onFeedback(true)
        }
        if (currentPressedLeft) {
            currentPressedLeft = false
            onButtonChange(PspButton.LEFT, false)
            onFeedback(true)
        }
        if (currentPressedRight) {
            currentPressedRight = false
            onButtonChange(PspButton.RIGHT, false)
            onFeedback(true)
        }
    }

    Box(
        modifier = modifier
            .size(baseSize)
            .testTag("psp_dpad")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    updateDirection(down.position, size.width.toFloat())

                    var pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null || !change.pressed) {
                            // Touch released or pointer lifted
                            break
                        }
                        change.consume()
                        updateDirection(change.position, size.width.toFloat())
                    }
                    releaseAll()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Center background cross well (recessed PSP bed)
        Box(
            modifier = Modifier
                .size(baseSize * 0.96f)
                .clip(CircleShape)
                .background(Color(0xFF090A0F).copy(alpha = opacity))
                .border(1.5.dp, Color(0xFF262B3B), CircleShape)
        )

        // Vertical bar of cross
        Box(
            modifier = Modifier
                .size(width = baseSize * 0.34f, height = baseSize * 0.94f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF202533).copy(alpha = opacity),
                            Color(0xFF141720).copy(alpha = opacity),
                            Color(0xFF202533).copy(alpha = opacity)
                        )
                    )
                )
                .border(1.dp, Color(0xFF3B4359), RoundedCornerShape(8.dp))
        )

        // Horizontal bar of cross
        Box(
            modifier = Modifier
                .size(width = baseSize * 0.94f, height = baseSize * 0.34f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF202533).copy(alpha = opacity),
                            Color(0xFF141720).copy(alpha = opacity),
                            Color(0xFF202533).copy(alpha = opacity)
                        )
                    )
                )
                .border(1.dp, Color(0xFF3B4359), RoundedCornerShape(8.dp))
        )

        // UP Wing
        DPadArrowButton(
            symbol = "▲",
            isPressed = isUp,
            glowIntensity = glowIntensity,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(width = baseSize * 0.32f, height = baseSize * 0.32f)
        )

        // DOWN Wing
        DPadArrowButton(
            symbol = "▼",
            isPressed = isDown,
            glowIntensity = glowIntensity,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-4).dp)
                .size(width = baseSize * 0.32f, height = baseSize * 0.32f)
        )

        // LEFT Wing
        DPadArrowButton(
            symbol = "◀",
            isPressed = isLeft,
            glowIntensity = glowIntensity,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 4.dp)
                .size(width = baseSize * 0.32f, height = baseSize * 0.32f)
        )

        // RIGHT Wing
        DPadArrowButton(
            symbol = "▶",
            isPressed = isRight,
            glowIntensity = glowIntensity,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-4).dp)
                .size(width = baseSize * 0.32f, height = baseSize * 0.32f)
        )

        // Iconic PSP central pivot well
        Box(
            modifier = Modifier
                .size(baseSize * 0.28f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF12151D), Color(0xFF07080B))
                    )
                )
                .border(1.dp, Color(0xFF2C3244), CircleShape)
        )
    }
}

@Composable
private fun DPadArrowButton(
    symbol: String,
    isPressed: Boolean,
    glowIntensity: Float,
    modifier: Modifier = Modifier
) {
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 5f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "elevation"
    )
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scaleFactor)
            .shadow(elevation.dp, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (isPressed) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF00E676), Color(0xFF00796B))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF2B3244), Color(0xFF1A1E29))
                    )
                }
            )
            .border(
                if (isPressed) 2.dp else 1.dp,
                if (isPressed) Color(0xFF69F0AE) else Color(0xFF4A5573),
                RoundedCornerShape(7.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (isPressed) Color.White else Color(0xFFB0BEC5),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

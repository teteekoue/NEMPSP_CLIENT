package com.example.nempsp.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nempsp.model.LayoutConfig
import com.example.nempsp.model.PspButton

@Composable
fun PspActionButtons(
    config: LayoutConfig,
    activeButtons: List<PspButton>,
    onButtonChange: (PspButton, Boolean) -> Unit,
    onFeedback: (isRelease: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = config.actionScale
    val opacity = config.buttonsOpacity
    val baseSize = 160.dp * scale
    val btnSize = 52.dp * scale
    val glowIntensity = config.buttonGlowIntensity

    Box(
        modifier = modifier
            .size(baseSize)
            .testTag("psp_action_buttons"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle recessed background circle bed
        Box(
            modifier = Modifier
                .size(baseSize * 0.94f)
                .clip(CircleShape)
                .background(Color(0xFF090A0F).copy(alpha = opacity * 0.8f))
                .border(1.5.dp, Color(0xFF262B3B), CircleShape)
        )

        // TRIANGLE (Top) - Green
        PspRoundButton(
            button = PspButton.TRIANGLE,
            symbol = "△",
            symbolColor = Color(0xFF00E676),
            isPressed = activeButtons.contains(PspButton.TRIANGLE),
            size = btnSize,
            opacity = opacity,
            glowIntensity = glowIntensity,
            onPressed = { pressed ->
                onButtonChange(PspButton.TRIANGLE, pressed)
                onFeedback(!pressed)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 2.dp)
        )

        // CIRCLE (Right) - Red
        PspRoundButton(
            button = PspButton.CIRCLE,
            symbol = "○",
            symbolColor = Color(0xFFFF3D00),
            isPressed = activeButtons.contains(PspButton.CIRCLE),
            size = btnSize,
            opacity = opacity,
            glowIntensity = glowIntensity,
            onPressed = { pressed ->
                onButtonChange(PspButton.CIRCLE, pressed)
                onFeedback(!pressed)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-2).dp)
        )

        // CROSS (Bottom) - Blue
        PspRoundButton(
            button = PspButton.CROSS,
            symbol = "✕",
            symbolColor = Color(0xFF2979FF),
            isPressed = activeButtons.contains(PspButton.CROSS),
            size = btnSize,
            opacity = opacity,
            glowIntensity = glowIntensity,
            onPressed = { pressed ->
                onButtonChange(PspButton.CROSS, pressed)
                onFeedback(!pressed)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-2).dp)
        )

        // SQUARE (Left) - Pink
        PspRoundButton(
            button = PspButton.SQUARE,
            symbol = "□",
            symbolColor = Color(0xFFFF4081),
            isPressed = activeButtons.contains(PspButton.SQUARE),
            size = btnSize,
            opacity = opacity,
            glowIntensity = glowIntensity,
            onPressed = { pressed ->
                onButtonChange(PspButton.SQUARE, pressed)
                onFeedback(!pressed)
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 2.dp)
        )
    }
}

@Composable
private fun PspRoundButton(
    button: PspButton,
    symbol: String,
    symbolColor: Color,
    isPressed: Boolean,
    size: androidx.compose.ui.unit.Dp,
    opacity: Float,
    glowIntensity: Float,
    onPressed: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Spring physics animation for realistic mechanical tactile rebound
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "btnScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 6f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "btnElev"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scaleFactor)
            .testTag("psp_btn_${button.name.lowercase()}")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressed(true)
                        tryAwaitRelease()
                        onPressed(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Glowing Neon Aura ring when pressed
        if (isPressed) {
            Box(
                modifier = Modifier
                    .size(size * 1.25f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                symbolColor.copy(alpha = 0.55f * glowIntensity),
                                symbolColor.copy(alpha = 0.15f * glowIntensity),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Outer metallic bevel with physical depth
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (isPressed) {
                        Brush.radialGradient(
                            colors = listOf(
                                symbolColor.copy(alpha = 0.65f),
                                Color(0xFF161924)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF384055).copy(alpha = opacity),
                                Color(0xFF181C26).copy(alpha = opacity)
                            )
                        )
                    }
                )
                .border(
                    width = if (isPressed) 2.5.dp else 1.5.dp,
                    color = if (isPressed) symbolColor else Color(0xFF4E5874).copy(alpha = opacity),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Sunken glossy inner cap with high-contrast glowing glyph
            Box(
                modifier = Modifier
                    .size(size * 0.82f)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = if (isPressed) {
                                listOf(
                                    symbolColor.copy(alpha = 0.45f),
                                    Color(0xFF0D1017)
                                )
                            } else {
                                listOf(
                                    Color(0xFF262C3D).copy(alpha = opacity),
                                    Color(0xFF13161F).copy(alpha = opacity)
                                )
                            }
                        )
                    )
                    .border(
                        0.5.dp,
                        if (isPressed) symbolColor.copy(alpha = 0.8f) else Color(0xFF3B445B),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = if (isPressed) Color.White else symbolColor,
                    fontSize = (22 * (size.value / 52f)).sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

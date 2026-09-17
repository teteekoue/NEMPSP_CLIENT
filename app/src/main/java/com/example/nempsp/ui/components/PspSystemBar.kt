package com.example.nempsp.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.nempsp.model.PspButton

@Composable
fun PspSystemBar(
    activeButtons: List<PspButton>,
    opacity: Float,
    scale: Float = 1.0f,
    onButtonChange: (PspButton, Boolean) -> Unit,
    onFeedback: (isRelease: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("psp_system_bar")
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF090B10).copy(alpha = opacity * 0.85f))
            .border(1.dp, Color(0xFF222736), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // HOME / PS
        PspPillButton(
            label = "HOME",
            isPressed = activeButtons.contains(PspButton.HOME),
            opacity = opacity,
            accentColor = Color(0xFF00E5FF),
            onPressed = { pressed ->
                onButtonChange(PspButton.HOME, pressed)
                onFeedback(!pressed)
            }
        )

        // VOL-
        PspPillButton(
            label = "VOL-",
            isPressed = activeButtons.contains(PspButton.VOL_DOWN),
            opacity = opacity,
            onPressed = { pressed ->
                onButtonChange(PspButton.VOL_DOWN, pressed)
                onFeedback(!pressed)
            }
        )

        // VOL+
        PspPillButton(
            label = "VOL+",
            isPressed = activeButtons.contains(PspButton.VOL_UP),
            opacity = opacity,
            onPressed = { pressed ->
                onButtonChange(PspButton.VOL_UP, pressed)
                onFeedback(!pressed)
            }
        )

        // NOTE ♪
        PspPillButton(
            label = "♪",
            isPressed = activeButtons.contains(PspButton.NOTE),
            opacity = opacity,
            onPressed = { pressed ->
                onButtonChange(PspButton.NOTE, pressed)
                onFeedback(!pressed)
            }
        )

        // SELECT
        PspPillButton(
            label = "SELECT",
            isPressed = activeButtons.contains(PspButton.SELECT),
            opacity = opacity,
            accentColor = Color(0xFFFFD54F),
            onPressed = { pressed ->
                onButtonChange(PspButton.SELECT, pressed)
                onFeedback(!pressed)
            }
        )

        // START
        PspPillButton(
            label = "START",
            isPressed = activeButtons.contains(PspButton.START),
            opacity = opacity,
            accentColor = Color(0xFF00E676),
            onPressed = { pressed ->
                onButtonChange(PspButton.START, pressed)
                onFeedback(!pressed)
            }
        )
    }
}

@Composable
private fun PspPillButton(
    label: String,
    isPressed: Boolean,
    opacity: Float,
    accentColor: Color = Color(0xFFB0BEC5),
    onPressed: (Boolean) -> Unit
) {
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "pillScale"
    )
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .scale(scaleFactor)
            .height(28.dp)
            .testTag("psp_pill_${label.lowercase()}")
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
        Box(
            modifier = Modifier
                .shadow(if (isPressed) 0.dp else 2.dp, shape)
                .clip(shape)
                .background(
                    if (isPressed) {
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.65f), Color(0xFF1E2536))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF282F40).copy(alpha = opacity),
                                Color(0xFF151822).copy(alpha = opacity)
                            )
                        )
                    }
                )
                .border(
                    if (isPressed) 1.5.dp else 1.dp,
                    if (isPressed) accentColor else Color(0xFF3B445B).copy(alpha = opacity),
                    shape
                )
                .padding(horizontal = 10.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isPressed) Color.White else accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

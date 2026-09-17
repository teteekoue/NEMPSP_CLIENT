package com.example.nempsp.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
fun PspShoulderTrigger(
    button: PspButton, // L or R
    isPressed: Boolean,
    opacity: Float,
    isLeft: Boolean,
    scale: Float = 1.0f,
    glowIntensity: Float = 0.85f,
    onPressed: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "triggerScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 6f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "triggerElevation"
    )

    val shape = if (isLeft) {
        RoundedCornerShape(topStart = 16.dp, bottomStart = 6.dp, topEnd = 6.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, bottomStart = 16.dp, topEnd = 16.dp, bottomEnd = 6.dp)
    }

    val widthDp = 110.dp * scale
    val heightDp = 44.dp * scale

    Box(
        modifier = modifier
            .width(widthDp)
            .height(heightDp)
            .scale(scaleFactor)
            .testTag("psp_trigger_${button.label.lowercase()}")
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
        // Glowing Neon aura when pressed
        if (isPressed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.6f * glowIntensity),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(elevation.dp, shape)
                .clip(shape)
                .background(
                    if (isPressed) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.9f),
                                Color(0xFF005B99).copy(alpha = 0.95f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF38435C).copy(alpha = opacity),
                                Color(0xFF1B202D).copy(alpha = opacity)
                            )
                        )
                    }
                )
                .border(
                    width = if (isPressed) 2.5.dp else 1.5.dp,
                    color = if (isPressed) Color(0xFF80D8FF) else Color(0xFF536082).copy(alpha = opacity),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = button.label,
                color = if (isPressed) Color.White else Color(0xFFDCE2ED),
                fontSize = (18 * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
    }
}

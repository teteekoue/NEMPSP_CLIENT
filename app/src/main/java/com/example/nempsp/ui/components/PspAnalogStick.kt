package com.example.nempsp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nempsp.model.LayoutConfig
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun PspAnalogStick(
    config: LayoutConfig,
    onAnalogChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = config.analogScale
    val opacity = config.buttonsOpacity
    val baseSize = 130.dp * scale
    val thumbSize = 64.dp * scale

    val coroutineScope = rememberCoroutineScope()
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }

    var rawX by remember { mutableFloatStateOf(0f) }
    var rawY by remember { mutableFloatStateOf(0f) }

    fun updateStick(dx: Float, dy: Float, maxRadius: Float) {
        val dist = sqrt(dx * dx + dy * dy)
        val clampedDist = dist.coerceAtMost(maxRadius)
        val angle = atan2(dy, dx)

        val posX = clampedDist * cos(angle)
        val posY = clampedDist * sin(angle)

        coroutineScope.launch {
            animatedOffsetX.snapTo(posX)
            animatedOffsetY.snapTo(posY)
        }

        val normDist = clampedDist / maxRadius
        if (normDist < config.stickDeadzone) {
            rawX = 0f
            rawY = 0f
            onAnalogChange(0f, 0f)
        } else {
            // Apply deadzone rescaling and sensitivity
            val scaledDist = ((normDist - config.stickDeadzone) / (1f - config.stickDeadzone))
                .coerceIn(0f, 1f) * config.stickSensitivity
            val outX = (scaledDist * cos(angle)).coerceIn(-1f, 1f)
            val outY = (scaledDist * sin(angle)).coerceIn(-1f, 1f)
            rawX = outX
            rawY = outY
            onAnalogChange(outX, outY)
        }
    }

    Box(
        modifier = modifier
            .size(baseSize)
            .testTag("psp_analog_stick"),
        contentAlignment = Alignment.Center
    ) {
        // Outer recessed well
        Box(
            modifier = Modifier
                .size(baseSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0C0E14).copy(alpha = opacity),
                            Color(0xFF1B1F2A).copy(alpha = opacity)
                        )
                    )
                )
                .border(1.5.dp, Color(0xFF333A4D), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Concentric guide rings
            Box(
                modifier = Modifier
                    .size(baseSize * 0.75f)
                    .border(1.dp, Color(0xFF222736), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(baseSize * 0.50f)
                    .border(1.dp, Color(0xFF222736), CircleShape)
            )

            // The moving PSP Textured Thumb "Nub"
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            animatedOffsetX.value.toInt(),
                            animatedOffsetY.value.toInt()
                        )
                    }
                    .size(thumbSize)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF434C63),
                                Color(0xFF232836),
                                Color(0xFF12141C)
                            )
                        )
                    )
                    .border(1.5.dp, Color(0xFF5A6685), CircleShape)
                    .pointerInput(Unit) {
                        val maxRadius = (size.width / 2f) * 0.9f
                        detectDragGestures(
                            onDragStart = { offset ->
                                val center = size.width / 2f
                                updateStick(offset.x - center, offset.y - center, maxRadius)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val curX = animatedOffsetX.value + dragAmount.x
                                val curY = animatedOffsetY.value + dragAmount.y
                                updateStick(curX, curY, maxRadius)
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    animatedOffsetX.animateTo(0f, spring(dampingRatio = 0.6f))
                                }
                                coroutineScope.launch {
                                    animatedOffsetY.animateTo(0f, spring(dampingRatio = 0.6f))
                                }
                                rawX = 0f
                                rawY = 0f
                                onAnalogChange(0f, 0f)
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    animatedOffsetX.animateTo(0f, spring(dampingRatio = 0.6f))
                                }
                                coroutineScope.launch {
                                    animatedOffsetY.animateTo(0f, spring(dampingRatio = 0.6f))
                                }
                                rawX = 0f
                                rawY = 0f
                                onAnalogChange(0f, 0f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // PSP concentric grip rings texture on top of the stick
                Box(
                    modifier = Modifier
                        .size(thumbSize * 0.8f)
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(thumbSize * 0.58f)
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(thumbSize * 0.36f)
                        .border(1.dp, Color(0x55FFFFFF), CircleShape)
                )
                // Center metallic dot
                Box(
                    modifier = Modifier
                        .size(thumbSize * 0.16f)
                        .clip(CircleShape)
                        .background(Color(0xFF7E8CA8))
                )
            }
        }
    }
}

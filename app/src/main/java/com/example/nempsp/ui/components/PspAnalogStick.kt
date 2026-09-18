package com.example.nempsp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.nempsp.model.LayoutConfig
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Stick analogique PSP.
 *
 * Corrections :
 * - **Course réelle** : le rayon utile était calculé sur la taille du *pouce* (64 dp) au lieu du
 *   *puits* (130 dp) → le stick saturait à ~29 dp de course (beaucoup trop nerveux) et le pouce
 *   n'atteignait jamais le bord du puits. La course est désormais « rayon du puits − rayon du pouce ».
 * - **Réglages appliqués en direct** : le geste était figé par `pointerInput(Unit)`, qui capturait
 *   une fois pour toutes le rayon, la zone morte et la sensibilité. Les modifier dans l'éditeur
 *   n'avait donc aucun effet tant qu'on ne quittait pas l'écran. Les valeurs sont maintenant des
 *   clés du `pointerInput`, donc rechargées à chaque changement.
 * - **Réponse immédiate** : `detectDragGestures` attendait le franchissement du « touch slop »
 *   avant de réagir. On suit le doigt dès le premier contact, en position absolue (plus de dérive
 *   ni d'accumulation quand on reste en butée).
 * - **Zone tactile élargie** : le geste est porté par le puits entier, plus seulement par le pouce.
 * - Suppression de l'état mort `rawX`/`rawY` (écrit, jamais lu).
 */
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

    val deadzone = config.stickDeadzone.coerceIn(0f, 0.6f)
    val sensitivity = config.stickSensitivity.coerceIn(0.2f, 3f)

    /**
     * Convertit la position absolue du doigt dans le puits en déflection −1..+1 (zone morte
     * recalée, sensibilité appliquée) et aligne le pouce sur cette position bornée.
     *
     * **Non-suspend** : `awaitEachGesture` s'exécute dans une coroutine à portée restreinte
     * (`AwaitPointerEventScope`), qui interdit d'appeler des fonctions suspendues hors de ce
     * scope — `Animatable.snapTo` en fait partie. On repasse donc par `coroutineScope.launch`,
     * exactement comme pour le retour au centre.
     */
    fun applyPosition(pointerX: Float, pointerY: Float, wellRadiusPx: Float, maxRadius: Float) {
        val dx = pointerX - wellRadiusPx
        val dy = pointerY - wellRadiusPx
        val distance = sqrt(dx * dx + dy * dy)
        val clamped = distance.coerceAtMost(maxRadius)
        val angle = atan2(dy, dx)

        coroutineScope.launch { animatedOffsetX.snapTo(clamped * cos(angle)) }
        coroutineScope.launch { animatedOffsetY.snapTo(clamped * sin(angle)) }

        val normalized = if (maxRadius <= 0f) 0f else clamped / maxRadius
        if (normalized < deadzone) {
            onAnalogChange(0f, 0f)
            return
        }

        // Recalage : la course utile (après zone morte) est remappée sur 0..1
        val scaled = ((normalized - deadzone) / (1f - deadzone)).coerceIn(0f, 1f) * sensitivity
        onAnalogChange(
            (scaled * cos(angle)).coerceIn(-1f, 1f),
            (scaled * sin(angle)).coerceIn(-1f, 1f)
        )
    }

    Box(
        modifier = modifier
            .size(baseSize)
            .testTag("psp_analog_stick"),
        contentAlignment = Alignment.Center
    ) {
        // Puits circulaire du stick : c'est lui qui porte le geste (zone tactile complète)
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
                .border(1.5.dp, Color(0xFF333A4D), CircleShape)
                .pointerInput(baseSize, thumbSize, deadzone, sensitivity) {
                    val wellRadiusPx = size.width / 2f
                    val thumbRadiusPx = thumbSize.toPx() / 2f
                    // Le pouce doit rester dans le puits : course = rayon puits − rayon pouce
                    val maxRadius = (wellRadiusPx - thumbRadiusPx).coerceAtLeast(wellRadiusPx * 0.25f)

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        applyPosition(down.position.x, down.position.y, wellRadiusPx, maxRadius)

                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                // Doigt levé (ou sorti de l'écran) : on libère l'axe
                                break
                            }
                            change.consume()
                            applyPosition(change.position.x, change.position.y, wellRadiusPx, maxRadius)
                        }

                        // Retour élastique au centre
                        coroutineScope.launch { animatedOffsetX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        coroutineScope.launch { animatedOffsetY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        onAnalogChange(0f, 0f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Anneaux de repère concentriques
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

            // Pouce texturé du stick
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
                    .border(1.5.dp, Color(0xFF5A6685), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Stries d'adhérence concentriques
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
                // Point central métallique
                Box(
                    modifier = Modifier
                        .size(thumbSize * 0.16f)
                        .clip(CircleShape)
                        .background(Color(0xFF789CA8))
                )
            }
        }
    }
}

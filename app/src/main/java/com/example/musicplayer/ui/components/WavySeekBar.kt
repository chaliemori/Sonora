package com.example.musicplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavySeekBar(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val clamped = progress.coerceIn(0f, 1f)

    val phase by if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "wavePhase")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phaseAnim"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val newProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val splitX = width * clamped

        val amplitude = if (isPlaying) 5.dp.toPx() else 2.dp.toPx()
        val wavelength = 36.dp.toPx()
        val strokeWidth = 3.5.dp.toPx()

        if (splitX > 2f) {
            val activePath = Path()
            val stepPx = 3f
            var x = 0f
            while (x <= splitX) {
                val angle = ((x / wavelength) * 2f * PI).toFloat() - phase
                val y = centerY + sin(angle) * amplitude
                if (x == 0f) {
                    activePath.moveTo(x, y)
                } else {
                    activePath.lineTo(x, y)
                }
                x += stepPx
            }
            val endAngle = ((splitX / wavelength) * 2f * PI).toFloat() - phase
            activePath.lineTo(splitX, centerY + sin(endAngle) * amplitude)

            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        if (width - splitX > 2f) {
            drawLine(
                color = inactiveColor,
                start = Offset(x = splitX + 6.dp.toPx(), y = centerY),
                end = Offset(x = width, y = centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        val thumbY = if (splitX > 0f) {
            val angle = ((splitX / wavelength) * 2f * PI).toFloat() - phase
            centerY + sin(angle) * amplitude
        } else {
            centerY
        }

        drawLine(
            color = activeColor,
            start = Offset(x = splitX, y = thumbY - 9.dp.toPx()),
            end = Offset(x = splitX, y = thumbY + 9.dp.toPx()),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

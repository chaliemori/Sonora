package com.example.musicplayer.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Единые физические кривые и хелперы анимаций Sonora в стиле Comp_1.mp4:
 * чистая GPU-физика без лишних ре-лэйаутов (Emphasized Deceleration + чёткая упругая отдача).
 */
object SonoraMotion {
    val EmphasizedEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.30f, 0.0f, 0.80f, 0.15f)

    fun <T> emphasizedTween(
        durationMs: Int = 340,
        delayMs: Int = 0
    ): TweenSpec<T> = tween(
        durationMillis = durationMs,
        delayMillis = delayMs,
        easing = EmphasizedEasing
    )

    fun <T> tactileSpring(
        dampingRatio: Float = 0.76f,
        stiffness: Float = 500f
    ) = spring<T>(dampingRatio = dampingRatio, stiffness = stiffness)

    fun <T> softSpring(
        dampingRatio: Float = 0.84f,
        stiffness: Float = 360f
    ) = spring<T>(dampingRatio = dampingRatio, stiffness = stiffness)
}

/**
 * Чёткая пружинная отдача при нажатии без дрожания и перелётов.
 */
fun Modifier.sonoraPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.965f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = SonoraMotion.tactileSpring(),
        label = "sonoraPressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

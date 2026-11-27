package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToLong

/**
 * Animates a Long monetary amount smoothly without Int overflow or Float precision loss
 * (safe for multi-billion Rial values using 53-bit Double interpolation).
 */
@Composable
fun rememberAnimatedAmount(
    targetAmount: Long,
    durationMillis: Int = 520,
    animateOnFirstAppear: Boolean = true
): Long {
    var fromAmount by remember {
        mutableLongStateOf(if (animateOnFirstAppear) 0L else targetAmount)
    }
    var toAmount by remember { mutableLongStateOf(targetAmount) }
    val progress = remember { Animatable(if (animateOnFirstAppear) 0f else 1f) }

    LaunchedEffect(targetAmount) {
        val currentDisplayed = fromAmount + ((toAmount - fromAmount).toDouble() * progress.value).roundToLong()
        if (currentDisplayed == targetAmount && progress.value >= 1f) return@LaunchedEffect
        fromAmount = currentDisplayed
        toAmount = targetAmount
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
    }

    return if (progress.value >= 1f) {
        toAmount
    } else {
        fromAmount + ((toAmount - fromAmount).toDouble() * progress.value).roundToLong()
    }
}

/**
 * Animates a 0f..1f progress bar fill smoothly from 0f on first composition and
 * smoothly transitions whenever targetProgress changes.
 */
@Composable
fun rememberAnimatedProgress(
    targetProgress: Float,
    durationMillis: Int = 620
): Float {
    val clamped = targetProgress.coerceIn(0f, 1f)
    val animated = remember { Animatable(0f) }

    LaunchedEffect(clamped) {
        animated.animateTo(
            targetValue = clamped,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
    }

    return animated.value
}

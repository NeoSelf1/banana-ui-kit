package com.neon.core.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Reusable animation specs aligned with iOS Animation+.swift
 * - fastSpring     ~= 100ms
 * - mediumFast     ~= 200ms
 * - mediumSpring   ~= 300ms
 * Prefer Tween for duration-based subtle UI fades/moves, and Spring for scale/punchy effects.
 */
object HMAnimations {
    // Duration-based tweens (typed helpers for inference)
    fun fadeFastTween() = tween<Float>(durationMillis = 100, easing = FastOutSlowInEasing as Easing)
    fun fadeMediumFastTween() = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing as Easing)
    fun fadeMediumTween() = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing as Easing)

    fun colorFastTween() = tween<Color>(durationMillis = 200, easing = FastOutSlowInEasing as Easing)
    fun colorMediumFastTween() = tween<Color>(durationMillis = 200, easing = FastOutSlowInEasing as Easing)
    fun colorMediumTween() = tween<Color>(durationMillis = 300, easing = FastOutSlowInEasing as Easing)

    fun dpFastTween() = tween<Dp>(durationMillis = 200, easing = FastOutSlowInEasing as Easing)
    fun dpMediumTween() = tween<Dp>(durationMillis = 300, easing = FastOutSlowInEasing as Easing)

    fun floatMediumFastTween() = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing as Easing)

    // Springs for scale/size transitions
    val FastSpringFloat = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val MediumSpringFloat = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
}

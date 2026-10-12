package com.pranshulgg.watchmaster.core.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object NavTransitions {
    private const val DURATION = 300
    private val easing = FastOutSlowInEasing

    fun enter(): EnterTransition =
        slideInHorizontally(tween(DURATION, easing = easing)) { it / 6 } +
            fadeIn(tween(DURATION, easing = easing))

    fun exit(): ExitTransition =
        slideOutHorizontally(tween(DURATION, easing = easing)) { -it / 12 } +
            fadeOut(tween(DURATION / 2))

    fun popEnter(): EnterTransition =
        slideInHorizontally(tween(DURATION, easing = easing)) { -it / 12 } +
            fadeIn(tween(DURATION, easing = easing))

    fun popExit(): ExitTransition =
        slideOutHorizontally(tween(DURATION, easing = easing)) { it / 6 } +
            fadeOut(tween(DURATION / 2))

    fun detailEnter(): EnterTransition =
        fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.94f)

    fun detailExit(): ExitTransition =
        fadeOut(tween(150))

    fun detailPopEnter(): EnterTransition =
        fadeIn(tween(200))

    fun detailPopExit(): ExitTransition =
        fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.94f)
}
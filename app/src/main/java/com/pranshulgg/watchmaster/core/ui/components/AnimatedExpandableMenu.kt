package com.pranshulgg.watchmaster.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pranshulgg.watchmaster.R

/**
 * One item in an [AnimatedExpandableMenuButton]'s popup.
 */
data class FloatingMenuOption(
    val title: String,
    val leading: Int,
    val action: () -> Unit
)

/**
 * A round trigger button (icon swaps between [collapsedIcon] and [expandedIcon]
 * with a fade + scale cross-fade) that expands into a vertical stack of
 * pill-shaped [options], each animating in with a staggered fade + slide + scale —
 * the same dropdown pattern used in MediaActionsFloatingToolbar's "⋮" menu.
 *
 * Fully self-contained: manages its own expanded/collapsed state internally.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedExpandableMenuButton(
    options: List<FloatingMenuOption>,
    modifier: Modifier = Modifier,
    collapsedIcon: Int = R.drawable.add_24px,
    expandedIcon: Int = R.drawable.close_24px,
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val yOffsetPx = remember(density) { with(density) { (-60).dp.roundToPx() } }
    val menuItemContentColor = MaterialTheme.colorScheme.surfaceContainer
    val menuItemContentTextStyle = MaterialTheme.typography.labelLarge

    Box(modifier) {
        FilledIconButton(
            modifier = Modifier.size(48.dp),
            onClick = { expanded = !expanded },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.6f))
                        .togetherWith(
                            fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.6f)
                        )
                },
                label = "MenuIconTransition"
            ) { isExpanded ->
                Symbol(
                    if (isExpanded) expandedIcon else collapsedIcon,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.BottomEnd,
                offset = IntOffset(0, yOffsetPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                var isVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) { isVisible = true }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val totalCount = options.size

                    options.forEachIndexed { index, option ->
                        val staggerDelay = (totalCount - 1 - index) * 45

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(
                                animationSpec = tween(durationMillis = 200, delayMillis = staggerDelay)
                            ) + slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight / 2 },
                                animationSpec = tween(
                                    durationMillis = 280,
                                    delayMillis = staggerDelay,
                                    easing = FastOutSlowInEasing
                                )
                            ) + scaleIn(
                                initialScale = 0.6f,
                                animationSpec = tween(
                                    durationMillis = 280,
                                    delayMillis = staggerDelay,
                                    easing = FastOutSlowInEasing
                                )
                            ),
                            exit = fadeOut(
                                animationSpec = tween(150)
                            ) + slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight / 2 },
                                animationSpec = tween(150)
                            ) + scaleOut(
                                targetScale = 0.6f,
                                animationSpec = tween(150)
                            )
                        ) {
                            Surface(
                                onClick = {
                                    expanded = false
                                    option.action()
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Symbol(option.leading, color = menuItemContentColor)
                                    Text(
                                        text = option.title,
                                        color = menuItemContentColor,
                                        style = menuItemContentTextStyle
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

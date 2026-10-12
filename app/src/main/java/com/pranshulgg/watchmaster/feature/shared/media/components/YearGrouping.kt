package com.pranshulgg.watchmaster.feature.shared.media.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

const val UNKNOWN_YEAR = "Unknown"

data class YearGroup<T>(
    val label: String,
    val entries: List<Pair<T, Instant?>>
)

fun yearLabel(date: Instant?): String {
    if (date == null) return UNKNOWN_YEAR
    val year = date.atZone(ZoneId.systemDefault()).year
    return if (year < 1990) UNKNOWN_YEAR else year.toString()
}

/** Groups by year (newest first, Unknown last). Order inside each year is kept. */
fun <T> groupByYear(entries: List<Pair<T, Instant?>>): List<YearGroup<T>> =
    entries
        .groupBy { yearLabel(it.second) }
        .map { (label, list) -> YearGroup(label, list) }
        .sortedBy { if (it.label == UNKNOWN_YEAR) Int.MAX_VALUE else -it.label.toInt() }

@Composable
fun YearHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    )
}

/**
 * Fast-scroll pill on the right edge. It follows the list as you scroll.
 * Press and drag it to jump; a bubble shows the year you are on.
 */
@Composable
fun YearScrollbar(
    listState: LazyListState,
    labels: List<String>,
    headerIndexes: List<Int>,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) return

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var dragging by remember { mutableStateOf(false) }
    var touchedIndex by remember { mutableIntStateOf(0) }
    var lastJumped by remember { mutableIntStateOf(-1) }

    // Which year is on screen while scrolling normally
    val currentGroup by remember(headerIndexes) {
        derivedStateOf {
            val visibleCount = listState.layoutInfo.visibleItemsInfo.size
            val probe = if (!listState.canScrollForward) {
                Int.MAX_VALUE
            } else {
                listState.firstVisibleItemIndex + visibleCount / 2
            }
            headerIndexes
                .indexOfLast { it <= probe }
                .coerceAtLeast(0)
                .coerceAtMost(labels.size - 1)
        }
    }

    // The year you jumped to stays selected until you scroll the list yourself
    var pinnedIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) pinnedIndex = null
        }
    }

    val highlighted = if (dragging) touchedIndex else (pinnedIndex ?: currentGroup)

    // Show while scrolling or dragging, hide shortly after things stop
    var recentlyActive by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress, dragging) {
        if (listState.isScrollInProgress || dragging) {
            recentlyActive = true
        } else {
            delay(1500)
            recentlyActive = false
        }
    }
    val stripAlpha by animateFloatAsState(
        targetValue = if (recentlyActive || dragging) 1f else 0f,
        label = "stripAlpha"
    )

    val trackBase = MaterialTheme.colorScheme.surfaceContainerHighest
    val trackColor by animateColorAsState(
        targetValue = trackBase.copy(alpha = if (dragging) 0.95f else 0.5f),
        label = "track"
    )

    BoxWithConstraints(modifier = modifier) {
        // Space between dots, shrinking if there are too many years to fit
        val perDot = minOf(20.dp, maxHeight / labels.size)
        val perDotPx = with(LocalDensity.current) { perDot.toPx() }
        val bubbleSize = 48.dp
        val bubbleSizePx = with(LocalDensity.current) { bubbleSize.toPx() }
        val stripHeight = perDot * labels.size

        fun jumpTo(y: Float) {
            val index = (y / perDotPx).toInt().coerceIn(0, labels.size - 1)
            touchedIndex = index
            pinnedIndex = index
            if (index != lastJumped) {
                lastJumped = index
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                // First year goes to the very top so the dropdown shows too
                val target = if (index == 0) 0 else headerIndexes[index]
                scope.launch { listState.scrollToItem(target) }
            }
        }

        if (stripAlpha > 0f) Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(stripHeight)
                .graphicsLayer { alpha = stripAlpha }
        ) {
            // Thin strip of dots on the right edge, floating over the list
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(18.dp)
                    .height(stripHeight)
                    .clip(CircleShape)
                    .background(trackColor)
                    .pointerInput(labels.size, perDotPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            dragging = true
                            lastJumped = -1
                            jumpTo(down.position.y)
                            down.consume()

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                jumpTo(change.position.y)
                                change.consume()
                            } while (event.changes.any { it.pressed })

                            dragging = false
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                labels.forEachIndexed { index, _ ->
                    val selected = index == highlighted
                    Box(
                        modifier = Modifier
                            .height(perDot)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (selected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    }
                                )
                        )
                    }
                }
            }

            // Big year bubble next to the finger, only while dragging
            if (dragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset {
                            IntOffset(
                                0,
                                (touchedIndex * perDotPx + perDotPx / 2f - bubbleSizePx / 2f)
                                    .roundToInt()
                            )
                        }
                        .padding(end = 36.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier.defaultMinSize(bubbleSize, bubbleSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labels[touchedIndex],
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
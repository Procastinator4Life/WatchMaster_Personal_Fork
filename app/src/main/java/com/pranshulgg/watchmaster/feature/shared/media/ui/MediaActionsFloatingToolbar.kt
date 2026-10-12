package com.pranshulgg.watchmaster.feature.shared.media.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.zIndex
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.Symbol
import com.pranshulgg.watchmaster.core.ui.components.TextAlertDialog
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.actionLabel
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.buttonIcon
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.confirmAction
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.dialogMessage

private data class MenuItemOptionList(
    val title: String,
    val leading: Int,
    val action: () -> Unit,
    val isInterruptOption: Boolean = false
)

private data class UiState(
    val showDialog: Boolean = false,
    val expanded: Boolean = false,
    val statusMenuExpanded: Boolean = false
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun MediaActionsFloatingToolbar(
    scrollBehavior: FloatingToolbarScrollBehavior,
    itemStatus: WatchStatus,
    actions: FloatingToolbarMediaActionsParams,
    isPinned: Boolean = false,
    isTv: Boolean = false,
    canMoveToWatchlist: Boolean = true,
) {
    val systemInsets = WindowInsets.systemBars.asPaddingValues()
    val menuItemContentColor = MaterialTheme.colorScheme.surfaceContainer
    val menuItemContentTextStyle = MaterialTheme.typography.labelLarge
    var uiState by remember { mutableStateOf(UiState()) }

    val density = LocalDensity.current
    val yOffsetPx = remember(density) { with(density) { (-60).dp.roundToPx() } }

    // Options that live in the "⋮" overflow menu
    val menuItemOptionList = listOfNotNull(
        MenuItemOptionList(
            if (isPinned) "Unpin" else "Pin",
            R.drawable.keep_24px,
            {
                actions.togglePin()
                SnackbarManager.show(if (!isPinned) "Media pinned" else "Media unpinned")
            }),
        MenuItemOptionList("List", R.drawable.lists_24px, { actions.openLists() }),
        MenuItemOptionList(
            "My Rating",
            R.drawable.star_24px,
            { actions.showRating() }
        ),
        MenuItemOptionList(
            "Change backdrop",
            R.drawable.photo_24px,
            { actions.changeBackdrop?.invoke() }
        ).takeIf { actions.changeBackdrop != null },
        MenuItemOptionList(
            "Delete",
            R.drawable.delete_24px,
            { actions.delete?.invoke() }
        ).takeIf { actions.delete != null }
    )

    // Options that live in the split-button's chevron popup
    val statusMenuOptionList = listOfNotNull(
        MenuItemOptionList(
            "Move to Watchlist",
            R.drawable.bookmark_24px,
            { actions.moveToWatchlist() }
        ).takeIf { itemStatus == WatchStatus.WATCHING && canMoveToWatchlist },
        MenuItemOptionList(
            "Interrupt",
            R.drawable.pause_24px,
            { actions.interruptWatching() }, isInterruptOption = true
        ).takeIf { itemStatus == WatchStatus.WATCHING },
        MenuItemOptionList(
            "Dropped",
            R.drawable.close_24px,
            { actions.dropItem() }
        ).takeIf { itemStatus == WatchStatus.WATCHING },
        MenuItemOptionList(
            "Skip season",
            R.drawable.play_arrow_24px,
            { actions.skipWatching() }
        ).takeIf {
            isTv && itemStatus != WatchStatus.FINISHED && itemStatus != WatchStatus.SKIPPED
        }
    )

    Box(
        Modifier.fillMaxWidth(),
    ) {
        HorizontalFloatingToolbar(
            expandedShadowElevation = 1.dp,
            scrollBehavior = scrollBehavior,
            modifier = Modifier
                .padding(
                    top = ScreenOffset,
                    bottom = systemInsets.calculateBottomPadding() + ScreenOffset
                )
                .align(Alignment.BottomCenter)
                .zIndex(1f),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            expanded = true,
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box {
                        SplitStatusButton(
                            itemStatus = itemStatus,
                            hasStatusOptions = statusMenuOptionList.isNotEmpty(),
                            onMainClick = {
                                if (itemStatus == WatchStatus.WATCHING) {
                                    actions.finishWatching()
                                } else {
                                    uiState = uiState.copy(showDialog = true)
                                }
                            },
                            onChevronClick = {
                                uiState = uiState.copy(statusMenuExpanded = !uiState.statusMenuExpanded)
                            }
                        )

                        if (uiState.statusMenuExpanded) {
                            StatusOptionsPopup(
                                options = statusMenuOptionList,
                                yOffsetPx = yOffsetPx,
                                menuItemContentColor = menuItemContentColor,
                                menuItemContentTextStyle = menuItemContentTextStyle,
                                onDismiss = { uiState = uiState.copy(statusMenuExpanded = false) },
                                onOptionSelected = { option ->
                                    uiState = uiState.copy(statusMenuExpanded = false)
                                    option.action()
                                }
                            )
                        }
                    }

                    Box {
                        FilledIconButton(
                            modifier = Modifier.size(48.dp),
                            onClick = { uiState = uiState.copy(expanded = !uiState.expanded) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                        ) {
                            AnimatedContent(
                                targetState = uiState.expanded,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.6f))
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.6f)
                                        )
                                },
                                label = "MenuIconTransition"
                            ) { isExpanded ->
                                Symbol(
                                    if (isExpanded) R.drawable.close_24px else R.drawable.more_vert_24px,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }

                        if (uiState.expanded) {
                            Popup(
                                alignment = Alignment.BottomEnd,
                                offset = IntOffset(0, yOffsetPx),
                                onDismissRequest = { uiState = uiState.copy(expanded = false) },
                                properties = PopupProperties(focusable = true)
                            ) {
                                var isVisible by remember { mutableStateOf(false) }

                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                    isVisible = true
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    val totalCount = menuItemOptionList.size

                                    menuItemOptionList.forEachIndexed { index, option ->
                                        val staggerDelay = (totalCount - 1 - index) * 45

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isVisible,
                                            enter = fadeIn(
                                                animationSpec = tween(
                                                    durationMillis = 200,
                                                    delayMillis = staggerDelay
                                                )
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
                                                    uiState = uiState.copy(expanded = false)
                                                    option.action()
                                                },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shadowElevation = 4.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(horizontal = 18.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Symbol(
                                                        option.leading,
                                                        color = menuItemContentColor
                                                    )
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
            },
        )
    }

    TextAlertDialog(
        show = uiState.showDialog,
        title = "Watch status",
        message = itemStatus.dialogMessage(isTv = isTv),
        confirmText = "Confirm",
        onConfirm = {
            itemStatus.confirmAction(
                start = { actions.startWatching() },
                reset = { actions.resetWatching() },
                finish = { actions.finishWatching() }
            )
        },
        onDismiss = {
            uiState = uiState.copy(showDialog = false)
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SplitStatusButton(
    itemStatus: WatchStatus,
    hasStatusOptions: Boolean,
    onMainClick: () -> Unit,
    onChevronClick: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.onSurface

    val mainShape = if (hasStatusOptions) {
        RoundedCornerShape(
            topStartPercent = 50,
            bottomStartPercent = 50,
            topEndPercent = 12,
            bottomEndPercent = 12
        )
    } else {
        RoundedCornerShape(percent = 50)
    }

    Row(
        modifier = Modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            onClick = onMainClick,
            shape = mainShape,
            color = containerColor,
            modifier = Modifier.fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = if (hasStatusOptions) 16.dp else 24.dp,
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Symbol(
                    itemStatus.buttonIcon,
                    color = contentColor,
                )
                Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(48.dp)))
                Text(
                    itemStatus.actionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
        }

        if (hasStatusOptions) {
            Surface(
                onClick = onChevronClick,
                shape = RoundedCornerShape(
                    topStartPercent = 12,
                    bottomStartPercent = 12,
                    topEndPercent = 50,
                    bottomEndPercent = 50
                ),
                color = containerColor,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Symbol(
                        R.drawable.keyboard_arrow_down_24px,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusOptionsPopup(
    options: List<MenuItemOptionList>,
    yOffsetPx: Int,
    menuItemContentColor: androidx.compose.ui.graphics.Color,
    menuItemContentTextStyle: androidx.compose.ui.text.TextStyle,
    onDismiss: () -> Unit,
    onOptionSelected: (MenuItemOptionList) -> Unit
) {
    Popup(
        alignment = Alignment.BottomEnd,
        offset = IntOffset(0, yOffsetPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        var isVisible by remember { mutableStateOf(false) }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            isVisible = true
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            val totalCount = options.size

            options.forEachIndexed { index, option ->
                val staggerDelay = (totalCount - 1 - index) * 45

                androidx.compose.animation.AnimatedVisibility(
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
                        onClick = { onOptionSelected(option) },
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
package com.pranshulgg.watchmaster.feature.lists.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.AnimatedExpandableMenuButton
import com.pranshulgg.watchmaster.core.ui.components.FloatingMenuOption
import com.pranshulgg.watchmaster.core.ui.components.Symbol
import com.pranshulgg.watchmaster.core.ui.components.Tooltip

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewListFloatingToolbar(
    selectedTab: Int,
    onItemSelected: (Int) -> Unit,
    scrollBehavior: FloatingToolbarScrollBehavior,
    onAction: (String) -> Unit,
    isPinned: Boolean = false
) {

    val labelList = listOf("Movies", "TV series")
    val unSelectedIcons = listOf(
        R.drawable.movie_24px,
        R.drawable.tv_24px,
    )
    val selectedIcons = listOf(
        R.drawable.movie_filled_24px,
        R.drawable.tv_filled_24px,
    )

    val colorScheme = MaterialTheme.colorScheme

    val systemInsets = WindowInsets.systemBars.asPaddingValues()

    Box(
        Modifier
            .fillMaxWidth()
    ) {
        HorizontalFloatingToolbar(
            expandedShadowElevation = 1.dp,
            scrollBehavior = scrollBehavior,
            modifier = Modifier
                .padding(
                    top = ScreenOffset,
                    bottom = systemInsets.calculateBottomPadding()
                            + ScreenOffset
                )
                .align(Alignment.BottomCenter)
                .zIndex(1f),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            expanded = true,
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    labelList.forEachIndexed { index, label ->
                        Tooltip(
                            label,
                            preferredPosition = TooltipAnchorPosition.Above,
                            spacing = 10.dp
                        ) {
                            ToggleButton(
                                modifier = Modifier.height(48.dp),
                                checked = selectedTab == index,
                                onCheckedChange = { onItemSelected(index) },
                                shapes = ToggleButtonDefaults.shapes(
                                    CircleShape,
                                    CircleShape,
                                    CircleShape
                                ),
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = Color.Transparent,
                                    checkedContainerColor = colorScheme.surfaceContainer
                                ),

                                ) {
                                Crossfade(selectedTab == index) {
                                    if (it) Symbol(
                                        selectedIcons[index],
                                        color = colorScheme.onSurface
                                    ) else Symbol(
                                        unSelectedIcons[index],
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                                AnimatedVisibility(
                                    visible = selectedTab == index,
                                    enter = expandHorizontally(motionScheme.defaultSpatialSpec()),
                                    exit = shrinkHorizontally(motionScheme.defaultSpatialSpec())
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip,
                                        color = if (selectedTab == index) colorScheme.onSurface else colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                                    )
                                }
                            }
                        }
                    }

                    ListActions(
                        onAction = { onAction(it) },
                        isPinned = isPinned
                    )
                }
            },
        )
    }
}

@Composable
private fun ListActions(
    onAction: (String) -> Unit,
    isPinned: Boolean = false
) {
    // Same animated staggered-pill dropdown used in MediaActionsFloatingToolbar
    // and ListEntryScreen's menu button — swapped in here for the plain
    // DropdownMenu that used to live in this spot.
    AnimatedExpandableMenuButton(
        options = listOf(
            FloatingMenuOption(
                title = if (isPinned) "Unpin" else "Pin",
                leading = R.drawable.keep_24px,
                action = { onAction("PIN_ACTION") }
            ),
            FloatingMenuOption(
                title = "Edit",
                leading = R.drawable.edit_24px,
                action = { onAction("EDIT_ACTION") }
            ),
            FloatingMenuOption(
                title = "Delete",
                leading = R.drawable.delete_24px,
                action = { onAction("DELETE_ACTION") }
            ),

        ),
        collapsedIcon = R.drawable.more_vert_24px,
        expandedIcon = R.drawable.close_24px
    )
}
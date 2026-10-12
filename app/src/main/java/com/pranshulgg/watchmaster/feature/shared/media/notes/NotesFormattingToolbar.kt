package com.pranshulgg.watchmaster.feature.shared.media.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.Symbol

private val TOOLBAR_HEIGHT = 52.dp
private val POPUP_GAP = 8.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesFormattingToolbar(
    modifier: Modifier = Modifier,
    onQuote: () -> Unit,
    onCheckbox: () -> Unit,
    onCallout: () -> Unit,
    onHeading: (Int) -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onStrikethrough: () -> Unit,
    onDivider: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    isBoldActive: Boolean = false,
    isItalicActive: Boolean = false,
    isStrikethroughActive: Boolean = false
) {
    var headingMenuExpanded by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val yOffsetPx = remember(density) { with(density) { -(TOOLBAR_HEIGHT + POPUP_GAP).roundToPx() } }

    val toolbarBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface

    // Soft neutral theme highlight (No green)
    val buttonHighlightColor = MaterialTheme.colorScheme.surfaceContainerHighest

    // Light blue circle for the overflow FAB
    val fabBgColor = MaterialTheme.colorScheme.primaryContainer
    val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.height(TOOLBAR_HEIGHT),
            shape = CircleShape,
            color = toolbarBg,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. "Aa" Heading Picker Button
                ToolbarCircleButton(
                    onClick = {
                        headingMenuExpanded = !headingMenuExpanded
                        if (headingMenuExpanded) overflowMenuExpanded = false
                    },
                    isActive = headingMenuExpanded,
                    activeColor = buttonHighlightColor,
                    contentColor = contentColor
                ) {
                    Text("Aa", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = contentColor)
                }

                // 2. Bold Toggle
                ToolbarCircleButton(
                    onClick = {
                        headingMenuExpanded = false
                        overflowMenuExpanded = false
                        onBold()
                    },
                    isActive = isBoldActive,
                    activeColor = buttonHighlightColor,
                    contentColor = contentColor
                ) {
                    Text("B", fontWeight = FontWeight.Black, fontSize = 16.sp, color = contentColor)
                }

                // 3. Italic Toggle
                ToolbarCircleButton(
                    onClick = {
                        headingMenuExpanded = false
                        overflowMenuExpanded = false
                        onItalic()
                    },
                    isActive = isItalicActive,
                    activeColor = buttonHighlightColor,
                    contentColor = contentColor
                ) {
                    Text(
                        "I",
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = contentColor
                    )
                }

                // 4. Strikethrough Toggle
                ToolbarCircleButton(
                    onClick = {
                        headingMenuExpanded = false
                        overflowMenuExpanded = false
                        onStrikethrough()
                    },
                    isActive = isStrikethroughActive,
                    activeColor = buttonHighlightColor,
                    contentColor = contentColor
                ) {
                    Text(
                        "S",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.LineThrough,
                        color = contentColor
                    )
                }

                // 5. Overflow FAB (Three Dots with Light Blue Circle)
                ToolbarCircleButton(
                    onClick = {
                        overflowMenuExpanded = !overflowMenuExpanded
                        if (overflowMenuExpanded) headingMenuExpanded = false
                    },
                    defaultColor = fabBgColor,
                    activeColor = buttonHighlightColor,
                    contentColor = fabContentColor
                ) {
                    Symbol(R.drawable.more_vert_24px, color = fabContentColor)
                }
            }
        }

        // Heading Picker Popup ("Aa")
        if (headingMenuExpanded) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(-60, yOffsetPx),
                onDismissRequest = { headingMenuExpanded = false },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
            ) {
                AnimatedPopupEntrance {
                    HeadingMenuPopupContent(
                        containerColor = toolbarBg,
                        onHeading = { level ->
                            headingMenuExpanded = false
                            onHeading(level)
                        }
                    )
                }
            }
        }

        // Block Elements & History Popup (Three Dots)
        if (overflowMenuExpanded) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(60, yOffsetPx),
                onDismissRequest = { overflowMenuExpanded = false },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
            ) {
                AnimatedPopupEntrance {
                    OverflowMenuPopupContent(
                        containerColor = toolbarBg,
                        onCheckbox = { overflowMenuExpanded = false; onCheckbox() },
                        onCallout = { overflowMenuExpanded = false; onCallout() },
                        onQuote = { overflowMenuExpanded = false; onQuote() },
                        onDivider = { overflowMenuExpanded = false; onDivider() },
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onUndo = onUndo,
                        onRedo = onRedo
                    )
                }
            }
        }
    }
}

/**
 * Wraps a popup's content with a quick fade + scale-in entrance (no stagger,
 * no pills) - just enough motion so the card doesn't feel like it's teleporting
 * in. Kept as a single reusable wrapper so both popups animate identically.
 */
@Composable
private fun AnimatedPopupEntrance(content: @Composable () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 160)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 100)
        )
    ) {
        content()
    }
}

@Composable
private fun HeadingMenuPopupContent(
    containerColor: Color,
    onHeading: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        shadowElevation = 8.dp,
        modifier = Modifier.width(170.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            HeadingRow("Title", MaterialTheme.typography.titleLarge, FontWeight.Bold) { onHeading(1) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            HeadingRow("Heading", MaterialTheme.typography.titleMedium, FontWeight.SemiBold) { onHeading(2) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            HeadingRow("Subheading", MaterialTheme.typography.titleSmall, FontWeight.Medium) { onHeading(3) }
        }
    }
}

@Composable
private fun OverflowMenuPopupContent(
    containerColor: Color,
    onCheckbox: () -> Unit,
    onCallout: () -> Unit,
    onQuote: () -> Unit,
    onDivider: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        shadowElevation = 8.dp,
        modifier = Modifier.width(185.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            PopupMenuItem(
                label = "Checkbox",
                iconRes = R.drawable.check_box_24px,
                onClick = onCheckbox
            )
            PopupMenuItem(
                label = "Callout",
                iconRes = R.drawable.lightbulb_24px,
                onClick = onCallout
            )
            PopupMenuItem(
                label = "Quote",
                iconRes = R.drawable.format_quote_24px,
                onClick = onQuote
            )
            PopupMenuItem(
                label = "Divider",
                iconRes = R.drawable.horizontal_rule_24px,
                onClick = onDivider
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            PopupMenuItem(
                label = "Undo",
                iconRes = R.drawable.undo_24px,
                enabled = canUndo,
                onClick = onUndo
            )
            PopupMenuItem(
                label = "Redo",
                iconRes = R.drawable.redo_24px,
                enabled = canRedo,
                onClick = onRedo
            )
        }
    }
}

@Composable
private fun PopupMenuItem(
    label: String,
    iconRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Symbol(iconRes, color = contentColor)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HeadingRow(
    label: String,
    style: TextStyle,
    weight: FontWeight,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = style,
            fontWeight = weight,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ToolbarCircleButton(
    onClick: () -> Unit,
    isActive: Boolean = false,
    defaultColor: Color = Color.Transparent,
    activeColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        isPressed || isActive -> activeColor
        else -> defaultColor
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
package com.pranshulgg.watchmaster.feature.shared.media.ui

data class FloatingToolbarMediaActionsParams(
    val startWatching: () -> Unit,
    val resetWatching: () -> Unit,
    val finishWatching: () -> Unit,
    val interruptWatching: () -> Unit,
    val skipWatching: () -> Unit,
    val showRating: () -> Unit,
    val delete: (() -> Unit)? = null,
    val togglePin: () -> Unit,
    val openLists: () -> Unit,
    val changeBackdrop: (() -> Unit)? = null,
    val moveToWatchlist: () -> Unit,
    val dropItem: () -> Unit,
)
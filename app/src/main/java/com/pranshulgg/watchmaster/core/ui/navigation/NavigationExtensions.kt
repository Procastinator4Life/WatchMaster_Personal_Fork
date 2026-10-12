package com.pranshulgg.watchmaster.core.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

private var lastNavigationTime = 0L

fun NavController.navigateSafely(
    route: String,
    debounceMs: Long = 500,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
    val now = System.currentTimeMillis()
    if (now - lastNavigationTime >= debounceMs) {
        lastNavigationTime = now
        if (builder != null) navigate(route, builder) else navigate(route)
    }
}

fun NavController.navigateSafely(
    route: Any,
    debounceMs: Long = 500,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
    val now = System.currentTimeMillis()
    if (now - lastNavigationTime >= debounceMs) {
        lastNavigationTime = now
        if (builder != null) navigate(route, builder) else navigate(route)
    }
}
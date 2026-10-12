package com.pranshulgg.watchmaster

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.navigation.compose.rememberNavController
import com.pranshulgg.watchmaster.core.prefs.AppPrefs
import com.pranshulgg.watchmaster.core.prefs.LocalAppPrefs
import com.pranshulgg.watchmaster.core.ui.navigation.AppNavHost
import com.pranshulgg.watchmaster.core.ui.snackbar.LocalSnackbarHostState
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.core.ui.theme.WatchMasterTheme
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.pranshulgg.watchmaster.core.sync.SyncManager
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pranshulgg.watchmaster.feature.shared.WatchlistViewModel

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WatchMasterApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val watchlistViewModel: WatchlistViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val syncScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    watchlistViewModel.refreshShowStatuses()

                    AppPrefs.getSyncFolderUri()?.let { folderUri ->
                        syncScope.launch {
                            SyncManager.checkAndMergeIfNeeded(context, folderUri)
                        }
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    AppPrefs.getSyncFolderUri()?.let { folderUri ->
                        syncScope.launch {
                            SyncManager.writeSnapshot(context, folderUri)
                        }
                    }
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        SnackbarManager.events.collectLatest { event ->
            snackbarHostState.currentSnackbarData?.dismiss()

            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                withDismissAction = event.actionLabel == null,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalAppPrefs provides AppPrefs.state()
    ) {
        val prefs = LocalAppPrefs.current

        val appTheme = when (prefs.appTheme) {
            "Dark" -> true
            "Light" -> false
            "System" -> isSystemInDarkTheme()
            else -> isSystemInDarkTheme()
        }

        WatchMasterTheme(
            darkTheme = appTheme,
            themeVariantType = prefs.themeVariant,
            dynamicColor = prefs.useDynamicColor,
            seedColor = Color(prefs.themeColor.toColorInt())
        ) {
            AppNavHost(
                navController = navController,
                snackbarHostState = snackbarHostState,
                motionScheme = motionScheme,
            )
        }
    }

}
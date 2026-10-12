package com.pranshulgg.watchmaster.feature.movie.detail

import android.annotation.SuppressLint
import android.widget.Space
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.LoadingScreenPlaceholder
import com.pranshulgg.watchmaster.feature.movie.detail.ui.MovieDetailsConfirmationDialog
import com.pranshulgg.watchmaster.feature.movie.detail.ui.MovieDetailsNoteDialog
import com.pranshulgg.watchmaster.feature.movie.detail.ui.MovieDetailsRatingDialog
import com.pranshulgg.watchmaster.feature.movie.detail.ui.MovieWatchProviderSheet
import com.pranshulgg.watchmaster.feature.shared.WatchlistViewModel
import com.pranshulgg.watchmaster.feature.shared.media.ui.FloatingToolbarMediaActionsParams
import com.pranshulgg.watchmaster.feature.shared.media.ui.MediaActionsFloatingToolbar
import com.pranshulgg.watchmaster.feature.tv.detail.ui.TvWatchProviderSheet
import com.pranshulgg.watchmaster.feature.lists.AddToListSheet
import com.pranshulgg.watchmaster.feature.movie.detail.ui.ChangeMovieBackdropSheet

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MovieDetailsScaffold(
    id: Long,
    scrollBehavior: FloatingToolbarScrollBehavior,
    viewModel: MovieDetailsViewModel,
    watchlistViewModel: WatchlistViewModel,
    navController: NavController
) {

    val watchlistFlow = remember(id) { watchlistViewModel.item(id) }
    val watchlistItem by watchlistFlow.collectAsStateWithLifecycle()

    val movieItem = viewModel.state

    val loading = viewModel.loading
    val isMoviePinned = watchlistItem?.isPinned == true

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (loading || movieItem == null) {
        LoadingScreenPlaceholder()
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        bottomBar = {
            MediaActionsFloatingToolbar(
                scrollBehavior,
                watchlistItem?.status ?: WatchStatus.WATCHING,
                actions = FloatingToolbarMediaActionsParams(
                    startWatching = { watchlistViewModel.start(id) },
                    resetWatching = {
                        if (watchlistItem?.isDropped == true) {
                            watchlistViewModel.unDrop(id)
                        } else {
                            watchlistViewModel.reset(id)
                        }
                    },
                    finishWatching = {
                        if ((watchlistItem?.userRating ?: 0.0) > 0.0) {
                            watchlistViewModel.finish(id)
                        } else {
                            viewModel.showRatingDialog(finishAfterRating = true)
                        }
                    },
                    showRating = { viewModel.showRatingDialog(originalRating = watchlistItem?.userRating?.toFloat() ?: 0f) },
                    interruptWatching = { watchlistViewModel.interrupt(id) },
                    skipWatching = {},
                    delete = { viewModel.showConfirmationDialog() },
                    togglePin = { watchlistViewModel.setPinned(id, !isMoviePinned) },
                    openLists = { viewModel.showListsSheet() },
                    changeBackdrop = { viewModel.showChangeBackdropSheet(id) },
                    moveToWatchlist = { watchlistViewModel.moveToWatchlist(id) },
                    dropItem = { watchlistViewModel.drop(id) }
                ),
                isMoviePinned
            )
        },

        ) { _ ->
        MovieDetailsContent(
            movieItem,
            watchlistItem,
            navController,
            scrollBehavior,
            viewModel,
            watchlistViewModel,
        )
    }

    MovieDetailsNoteDialog(viewModel, watchlistViewModel, watchlistItem)
    MovieDetailsRatingDialog(viewModel, watchlistViewModel, watchlistItem)
    MovieDetailsConfirmationDialog(viewModel, watchlistViewModel, watchlistItem, navController)
    MovieWatchProviderSheet(
        sheetState,
        onDismiss = { viewModel.hideWatchProviderSheet() },
        viewModel
    )

    if (viewModel.uiState.value.isListsSheetOpen) {
        AddToListSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            itemId = id,
            onDismiss = { viewModel.hideListsSheet() }
        )
    }

    if (viewModel.uiState.value.isChangeBackdropSheetOpen) {
        ChangeMovieBackdropSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            backdrops = viewModel.backdropOptions,
            loading = viewModel.loadingBackdrops,
            failed = viewModel.backdropLoadFailed,
            onRetry = { viewModel.loadBackdrops(id) },
            onDismiss = { viewModel.hideChangeBackdropSheet() },
            onSelect = { path ->
                viewModel.selectBackdrop(id, path)
                viewModel.hideChangeBackdropSheet()
            }
        )
    }

}
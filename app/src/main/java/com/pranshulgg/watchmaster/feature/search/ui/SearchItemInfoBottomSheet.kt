package com.pranshulgg.watchmaster.feature.search.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.ActionBottomSheet
import com.pranshulgg.watchmaster.core.ui.navigation.NavRoutes
import com.pranshulgg.watchmaster.core.ui.navigation.navigateSafely
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.feature.search.SearchViewModel
import com.pranshulgg.watchmaster.feature.search.components.SearchItemInfoSheetContent
import com.pranshulgg.watchmaster.feature.shared.WatchlistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchItemInfoBottomSheet(
    viewModel: SearchViewModel,
    watchlistViewModel: WatchlistViewModel,
    sheetState: SheetState,
    show: Boolean,
    navController: NavController
) {

    val item = viewModel.uiState.value.selectedItem
    val watchlistItems by watchlistViewModel.watchlist.collectAsState()
    val alreadyInLibrary = item?.let { selected ->
        watchlistItems.any { it.id == selected.id }
    } == true

    // Existing TV shows need their saved seasons so that "In Library" can open
    // the most relevant season instead of adding/replacing anything.
    val librarySeasons by watchlistViewModel
        .seasonsForShow(item?.id ?: Long.MIN_VALUE)
        .collectAsState()

    val tvDetailsList = if (item?.mediaType == "tv") {
        viewModel.seasonData.filter { it.name != "Specials" }
    } else {
        null
    }

    val scope = rememberCoroutineScope()
    val disabled = !alreadyInLibrary &&
        (item?.mediaType != "movie" && tvDetailsList.isNullOrEmpty())

    fun openLibraryItem() {
        val selected = item ?: return

        val route = if (selected.mediaType == "tv") {
            val season = librarySeasons.firstOrNull {
                it.status == WatchStatus.WATCHING ||
                    it.status == WatchStatus.INTERRUPTED
            } ?: librarySeasons.firstOrNull {
                it.status == WatchStatus.WANT_TO_WATCH
            } ?: librarySeasons.firstOrNull()

            season?.let {
                NavRoutes.tvDetail(
                    selected.id,
                    it.seasonNumber
                )
            }
        } else {
            NavRoutes.movieDetail(selected.id)
        }

        if (route != null) {
            viewModel.hideSheet()
            navController.navigateSafely(route) {
                launchSingleTop = true
            }
        } else {
            SnackbarManager.show("This item is in your library, but has no season to open")
        }
    }

    if (show && item != null)
        ActionBottomSheet(
            sheetState = sheetState,
            onCancel = {
                viewModel.hideSheet()
            },
            confirmText = if (alreadyInLibrary) "In Library" else "Add to Library",
            confirmBtnMaxWidth = true,
            isConfirmDisabled = disabled,
            onConfirm = {
                if (alreadyInLibrary) {
                    openLibraryItem()
                } else {
                    scope.launch {
                        viewModel.addToWatchlist(item, tvDetailsList, watchlistViewModel)
                        SnackbarManager.show("Added to library")
                    }
                }
            }
        ) {
            SearchItemInfoSheetContent(
                item = item,
                seasonLoading = viewModel.seasonLoading,
                seasonData = viewModel.seasonData
            )
        }
}

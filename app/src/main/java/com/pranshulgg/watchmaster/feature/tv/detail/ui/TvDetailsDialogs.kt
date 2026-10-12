package com.pranshulgg.watchmaster.feature.tv.detail.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.feature.shared.WatchlistViewModel
import com.pranshulgg.watchmaster.feature.shared.media.components.MediaConfirmationDialogContent
import com.pranshulgg.watchmaster.feature.shared.media.components.MediaNoteDialogContent
import com.pranshulgg.watchmaster.feature.shared.media.components.MediaRatingDialogContent
import com.pranshulgg.watchmaster.feature.tv.detail.TvDetailsViewModel
import com.pranshulgg.watchmaster.feature.shared.media.notes.NotesEditorSheet
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity

@Composable
fun TvDetailsNoteDialog(
    viewModel: TvDetailsViewModel,
    watchlistViewModel: WatchlistViewModel,
    watchlistItem: WatchlistItemEntity?,
) {
    val uiState = viewModel.uiState.value

    watchlistItem?.let { item ->
        NotesEditorSheet(
            show = uiState.showNoteDialog,
            initialNote = item.notes ?: "",
            onDismiss = { viewModel.hideNoteDialog() },
            onSave = { newNote ->
                watchlistViewModel.setNote(
                    item.id,
                    newNote
                )
            }
        )
    }
}

@Composable
fun TvDetailsRatingDialog(
    viewModel: TvDetailsViewModel,
    watchlistViewModel: WatchlistViewModel,
    season: SeasonEntity?,
) {
    val uiState = viewModel.uiState.value

    season?.let { seasonIt ->
        MediaRatingDialogContent(
            uiState.showRatingDialog,
            viewModel::hideRatingDialog,
            isUpdateRating = uiState.isUpdateRating,
            originalRating = uiState.originalRating,
            isTv = true,
            isFinishFlow = uiState.finishAfterRating,
            onConfirm = { rating ->
                watchlistViewModel.setSeasonUserRating(seasonIt.showId, seasonIt.seasonNumber, rating)
                if (uiState.finishAfterRating) {
                    watchlistViewModel.finishSeason(
                        showId = seasonIt.showId,
                        seasonNumber = seasonIt.seasonNumber,
                        lastEpNumber = seasonIt.episodeCount
                    )
                }
                if (uiState.isUpdateRating) SnackbarManager.show("User rating updated")
            },
            onRemoveRating = {
                watchlistViewModel.removeSeasonUserRating(
                    showId = seasonIt.showId,
                    seasonNumber = seasonIt.seasonNumber
                )
            },
            onSkip = {
                if (uiState.finishAfterRating) {
                    watchlistViewModel.finishSeason(
                        showId = seasonIt.showId,
                        seasonNumber = seasonIt.seasonNumber,
                        lastEpNumber = seasonIt.episodeCount
                    )
                }
            }
        )
    }
}

@Composable
fun TvDetailsConfirmationDialog(
    viewModel: TvDetailsViewModel,
    watchlistViewModel: WatchlistViewModel,
    season: SeasonEntity?,
    navController: NavController
) {
    val uiState = viewModel.uiState.value

    season?.let { seasonIt ->
        MediaConfirmationDialogContent(
            uiState.showConfirmationDialog,
            viewModel::hideConfirmationDialog,
            onConfirm = {
                // Always delete the selected season by its composite identity.
                // The repository removes the parent show only when this was the
                // last remaining season.
                watchlistViewModel.deleteSeason(
                    showId = seasonIt.showId,
                    seasonNumber = seasonIt.seasonNumber
                )
                SnackbarManager.show("Season deleted ${seasonIt.name}")
                navController.popBackStack()
            },
            isTv = true
        )
    }
}



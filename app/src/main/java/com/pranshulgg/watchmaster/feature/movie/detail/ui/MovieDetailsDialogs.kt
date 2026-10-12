package com.pranshulgg.watchmaster.feature.movie.detail.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.feature.movie.detail.MovieDetailsViewModel
import com.pranshulgg.watchmaster.feature.shared.WatchlistViewModel
import com.pranshulgg.watchmaster.feature.shared.media.components.MediaConfirmationDialogContent
import com.pranshulgg.watchmaster.feature.shared.media.components.MediaNoteDialogContent
import com.pranshulgg.watchmaster.feature.shared.media.components.MediaRatingDialogContent
import com.pranshulgg.watchmaster.feature.shared.media.notes.NotesEditorSheet

@Composable
fun MovieDetailsNoteDialog(
    viewModel: MovieDetailsViewModel,
    watchlistViewModel: WatchlistViewModel,
    watchlistItem: WatchlistItemEntity?,
) {
    val uiState = viewModel.uiState.value

    watchlistItem?.let { item ->
        NotesEditorSheet(
            show = uiState.showNoteDialog,
            initialNote = item.notes ?: "",
            onDismiss = { viewModel.hideNoteDialog() },
            onSave = { newNote -> watchlistViewModel.setNote(item.id, newNote) }
        )
    }
}

@Composable
fun MovieDetailsRatingDialog(
    viewModel: MovieDetailsViewModel,
    watchlistViewModel: WatchlistViewModel,
    watchlistItem: WatchlistItemEntity?,
) {
    val uiState = viewModel.uiState.value

    watchlistItem?.let { item ->
        MediaRatingDialogContent(
            uiState.showRatingDialog,
            viewModel::hideRatingDialog,
            isUpdateRating = uiState.isUpdateRating,
            originalRating = uiState.originalRating,
            isFinishFlow = uiState.finishAfterRating,
            onConfirm = { rating ->
                watchlistViewModel.setUserRating(item.id, rating)
                if (uiState.finishAfterRating) watchlistViewModel.finish(item.id)
                if (uiState.isUpdateRating) SnackbarManager.show("User rating updated")
            },
            onRemoveRating = {
                watchlistViewModel.removeUserRating(item.id)
            },
            onSkip = {
                if (uiState.finishAfterRating) watchlistViewModel.finish(item.id)
            }
        )
    }
}


@Composable
fun MovieDetailsConfirmationDialog(
    viewModel: MovieDetailsViewModel,
    watchlistViewModel: WatchlistViewModel,
    watchlistItem: WatchlistItemEntity?,
    navController: NavController
) {
    val uiState = viewModel.uiState.value

    watchlistItem?.let { item ->
        MediaConfirmationDialogContent(
            uiState.showConfirmationDialog,
            viewModel::hideConfirmationDialog,
            onConfirm = {
                watchlistViewModel.delete(item.id)
                SnackbarManager.show("Movie deleted ${item.title}")
                navController.popBackStack()
            }
        )
    }
}

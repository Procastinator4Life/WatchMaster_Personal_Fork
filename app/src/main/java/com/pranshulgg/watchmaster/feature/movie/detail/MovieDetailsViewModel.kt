package com.pranshulgg.watchmaster.feature.movie.detail

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.data.CountryWatchProviders
import com.pranshulgg.watchmaster.data.local.entity.MovieBundle
import com.pranshulgg.watchmaster.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import com.pranshulgg.watchmaster.data.ImageItem
import com.pranshulgg.watchmaster.data.repository.WatchlistRepository

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val repo: MovieRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    var state by mutableStateOf<MovieBundle?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    fun load(
        movieId: Long,
        onError: () -> Unit,
        forceFetch: Boolean = false,
        onDone: (changed: Boolean) -> Unit = {}
    ) {
        if (state != null && !forceFetch) return

        val previous = state

        viewModelScope.launch {
            loading = true

            val movie = try {
                repo.getWholeMovieData(movieId, forceFetch)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (forceFetch) {
                    loading = false
                }
                onError()
                return@launch
            }

            state = movie
            loading = false

            onDone(
                previous == null ||
                    previous.copy(cachedAt = 0L) != movie.copy(cachedAt = 0L)
            )
        }
    }

    private val _uiState = mutableStateOf(MovieDetailsUiState())
    val uiState: State<MovieDetailsUiState> = _uiState

    fun showNoteDialog(note: String) {
        _uiState.value = _uiState.value.copy(
            showNoteDialog = true,
            note = note
        )
    }

    fun hideNoteDialog() {
        _uiState.value = _uiState.value.copy(showNoteDialog = false)
    }

    fun updateNoteText(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun showRatingDialog(originalRating: Float = 0f, finishAfterRating: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            showRatingDialog = true,
            isUpdateRating = originalRating > 0f,
            originalRating = originalRating,
            finishAfterRating = finishAfterRating
        )
    }

    fun hideRatingDialog() {
        _uiState.value = _uiState.value.copy(
            showRatingDialog = false,
            isUpdateRating = false,
            originalRating = 0f,
            finishAfterRating = false
        )
    }

    fun showConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showConfirmationDialog = true)
    }

    fun hideConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showConfirmationDialog = false)
    }

    var backdropOptions by mutableStateOf<List<ImageItem>>(emptyList())
        private set

    var loadingBackdrops by mutableStateOf(false)
        private set

    var backdropLoadFailed by mutableStateOf(false)
        private set

    fun loadBackdrops(movieId: Long) {
        viewModelScope.launch {
            loadingBackdrops = true
            backdropLoadFailed = false
            backdropOptions = emptyList()
            try {
                backdropOptions = repo.fetchMovieBackdrops(movieId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                backdropLoadFailed = true
            } finally {
                loadingBackdrops = false
            }
        }
    }

    fun showChangeBackdropSheet(movieId: Long) {
        _uiState.value = _uiState.value.copy(
            isChangeBackdropSheetOpen = true
        )

        loadBackdrops(movieId)
    }

    fun hideChangeBackdropSheet() {
        _uiState.value = _uiState.value.copy(isChangeBackdropSheetOpen = false)
    }

    fun selectBackdrop(movieId: Long, backdropPath: String) {
        viewModelScope.launch {
            try {
                repo.setBackdropOverride(movieId, backdropPath)
                watchlistRepository.updateBackdropOverride(movieId, backdropPath)
                state = state?.copy(backdropOverride = backdropPath)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SnackbarManager.show("Couldn't change the backdrop. Try again")
            }
        }
    }

    fun showWatchProviderSheet(providers: CountryWatchProviders? = null) {
        _uiState.value =
            _uiState.value.copy(isWatchProviderSheetOpen = true, currentWatchProviders = providers)
    }

    fun hideWatchProviderSheet() {
        _uiState.value =
            _uiState.value.copy(isWatchProviderSheetOpen = false)
    }

    fun showListsSheet() {
        _uiState.value = _uiState.value.copy(isListsSheetOpen = true)
    }

    fun hideListsSheet() {
        _uiState.value = _uiState.value.copy(isListsSheetOpen = false)
    }

}
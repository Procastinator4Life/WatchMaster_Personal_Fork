package com.pranshulgg.watchmaster.feature.tv.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.data.CountryWatchProviders
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.TvBundle
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.data.repository.TvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList
import kotlin.coroutines.cancellation.CancellationException
import com.pranshulgg.watchmaster.data.repository.WatchlistRepository


@HiltViewModel
class TvDetailsViewModel @Inject constructor(
    private val repo: TvRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    var state by mutableStateOf<TvBundle?>(null)
        private set

    var loading by mutableStateOf(false)
        private set


    fun load(tvId: Long, onError: () -> Unit) {
        if (state != null) return

        viewModelScope.launch {
            loading = true

            val tv = try {
                repo.getWholeTvData(tvId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onError()
                return@launch
            }

            state = tv

            loading = false
        }
    }

    private var isLoadingEps = false
    fun loadEpisodes(
        tvId: Long,
        seasonNumber: Int
    ) {
        if (isLoadingEps) return

        isLoadingEps = true

        viewModelScope.launch {
            try {
                repo.ensureEpisodesFetched(
                    tvId = tvId,
                    seasonNumber = seasonNumber
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                SnackbarManager.show(
                    "Failed to fetch season data"
                )
            } finally {
                isLoadingEps = false
            }
        }
    }

    fun seasonEpisodes(
        showId: Long,
        seasonNumber: Int
    ): StateFlow<List<TvEpisodeEntity>> {

        return repo
            .getEpisodesForSeason(
                showId = showId,
                seasonNumber = seasonNumber
            )
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    }

    private val _uiState = mutableStateOf(TvDetailsUiState())
    val uiState: State<TvDetailsUiState> = _uiState

    fun hideNoteDialog() {
        _uiState.value = _uiState.value.copy(showNoteDialog = false)
    }

    fun showNoteDialog() {
        _uiState.value = _uiState.value.copy(
            showNoteDialog = true
        )
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

    fun showWatchProviderSheet(providers: CountryWatchProviders? = null) {
        _uiState.value =
            _uiState.value.copy(isWatchProviderSheetOpen = true, currentWatchProviders = providers)
    }

    fun hideWatchProviderSheet() {
        _uiState.value =
            _uiState.value.copy(isWatchProviderSheetOpen = false)
    }

    fun updateEpisodeWatchedDate(
        epId: Long,
        watchedAt: Long,
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        viewModelScope.launch {
            repo.markEpWatched(
                epId = epId,
                showId = showId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
            repo.updateEpisodeWatchedDate(
                epId = epId,
                timestamp = watchedAt
            )
            watchlistRepository.refreshShowStatus(showId)
        }
    }

    fun markEpWatched(
        epId: Long,
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        viewModelScope.launch {
            repo.markEpWatched(
                epId = epId,
                showId = showId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
            watchlistRepository.refreshShowStatus(showId)
        }
    }

    fun markEpUnWatched(
        epId: Long,
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        viewModelScope.launch {
            repo.markEpUnWatched(
                epId = epId,
                showId = showId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
            watchlistRepository.refreshShowStatus(showId)
        }
    }

    fun markAllEpsWatched(
        showId: Long,
        seasonNumber: Int,
        lastEpNumber: Int
    ) {
        viewModelScope.launch {
            repo.markAllEpWatched(
                showId = showId,
                seasonNumber = seasonNumber,
                lastEpNumber = lastEpNumber
            )
        }
    }

    fun markEpWatchedFromCount(
        showId: Long,
        seasonNumber: Int,
        count: Int
    ) {
        viewModelScope.launch {
            repo.markEpWatchedFromCount(
                showId = showId,
                seasonNumber = seasonNumber,
                count = count
            )
        }
    }
    
    // Re-reads the saved copy of the show, with no network call
    fun reloadShow(tvId: Long) {
        viewModelScope.launch {
            try {
                state = repo.getWholeTvData(tvId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun refreshSeasonData(season: SeasonEntity) {
        if (loading) return
        SnackbarManager.show("Refreshing...")
        loading = true
        viewModelScope.launch {
            try {
                repo.refreshSeasonData(season)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SnackbarManager.show(
                    "Refresh failed",
                )
            } finally {
                loading = false
            }
            loadEpisodes(
                tvId = season.showId,
                seasonNumber = season.seasonNumber
            )
        }
    }

    fun showListsSheet() {
        _uiState.value = _uiState.value.copy(isListsSheetOpen = true)
    }

    fun hideListsSheet() {
        _uiState.value = _uiState.value.copy(isListsSheetOpen = false)
    }

    fun showChangeBackdropSheet() {
        _uiState.value = _uiState.value.copy(isChangeBackdropSheetOpen = true)
    }

    fun hideChangeBackdropSheet() {
        _uiState.value = _uiState.value.copy(isChangeBackdropSheetOpen = false)
    }

    suspend fun fetchBackdrops(tvId: Long) = repo.fetchTvBackdrops(tvId)

    fun selectBackdrop(tvId: Long, backdropPath: String) {
        viewModelScope.launch {
            repo.setBackdropOverride(tvId, backdropPath)
            watchlistRepository.updateBackdropOverride(tvId, backdropPath)
            state = state?.copy(backdropOverride = backdropPath)
        }
    }
    
}
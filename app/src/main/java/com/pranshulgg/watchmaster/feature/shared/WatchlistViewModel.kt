package com.pranshulgg.watchmaster.feature.shared

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.data.repository.WatchlistRepository
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: WatchlistRepository
) : ViewModel() {


    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val watchlist = repository
        .getWatchlist()
        .onEach {
            _isLoading.value = false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val seasons = repository
        .getAllSeasons()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val tvEpisodes = repository
        .getAllEpisodes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun start(id: Long) = viewModelScope.launch {
        repository.markStarted(id)
    }

    fun finish(id: Long) = viewModelScope.launch {
        repository.markFinished(id)
    }

    fun interrupt(id: Long) = viewModelScope.launch {
        repository.markInterrupted(id)
    }

    fun reset(id: Long) = viewModelScope.launch {
        repository.markStarted(id)
    }

    fun moveToWatchlist(id: Long) = viewModelScope.launch {
        repository.markWantToWatch(id)
    }

    fun delete(id: Long) = viewModelScope.launch {
        repository.deleteItem(id)
    }

    suspend fun exists(id: Long): Boolean =
        repository.itemExists(id)

    private var lastMetadataRefreshAt = 0L
    private var refreshing = false

    fun refreshShowStatuses() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        if (!refreshing && now - lastMetadataRefreshAt >= 6 * 60 * 60 * 1000L) {
            refreshing = true
            try {
                // Refresh TMDB metadata and episode lists before deriving show
                // statuses, so a newly aired episode can move a show from Finished
                // to Watching in the same resume cycle.
                repository.refreshLibraryMetadata()
                lastMetadataRefreshAt = System.currentTimeMillis()
            } finally {
                refreshing = false
            }
        }

        watchlist.value
            .filter { it.mediaType == "tv" }
            .forEach { item ->
                repository.refreshShowStatus(item.id)
            }
    }

    fun refreshShow(
        id: Long,
        seasonNumber: Int,
        onDone: (Boolean?) -> Unit = {}
    ) = viewModelScope.launch {
        val item = repository.getItemById(id).first()
        val changed = if (item == null) null else repository.refreshShowAndCompare(item, seasonNumber)
        onDone(changed)
    }

    fun setFavorite(id: Long, isFavorite: Boolean) = viewModelScope.launch {
        repository.updateFavorite(id, isFavorite)
    }

    fun setPinned(id: Long, isPinned: Boolean) = viewModelScope.launch {
        repository.updatePinned(id, isPinned)
    }

    fun setUserRating(id: Long, rating: Double) = viewModelScope.launch {
        repository.updateUserRating(id, rating)
    }

    fun removeUserRating(id: Long) = viewModelScope.launch {
        repository.clearUserRating(id)
    }

    fun setNote(id: Long, note: String) = viewModelScope.launch {
        repository.updateNote(id, note)
    }


    private val itemCache = mutableMapOf<Long, StateFlow<WatchlistItemEntity?>>()

    fun item(id: Long): StateFlow<WatchlistItemEntity?> {
        return itemCache.getOrPut(id) {
            repository.getItemById(id)
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null
                )
        }
    }

    private val seasonsCache = mutableMapOf<Long, StateFlow<List<SeasonEntity>>>()

    fun seasonsForShow(showId: Long): StateFlow<List<SeasonEntity>> {
        return seasonsCache.getOrPut(showId) {
            repository.getSeasonsForShow(showId)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
                )
        }
    }

    fun startSeason(showId: Long, seasonNumber: Int) = viewModelScope.launch {
        val started = repository.markSeasonStarted(
            showId = showId,
            seasonNumber = seasonNumber
        )

        if (!started) {
            SnackbarManager.show(
                "Finish the season you're currently watching first!"
            )
        }
    }

    fun finishSeason(
        showId: Long,
        seasonNumber: Int,
        lastEpNumber: Int
    ) = viewModelScope.launch {
        repository.markSeasonFinished(
            showId,
            seasonNumber,
            lastEpNumber
        )
    }

    fun interruptSeason(showId: Long, seasonNumber: Int) = viewModelScope.launch {
        repository.markSeasonInterrupted(showId, seasonNumber)
    }

    fun resetSeason(showId: Long, seasonNumber: Int) = viewModelScope.launch {
        repository.resetSeason(showId, seasonNumber)
    }

    fun moveSeasonToWatchlist(showId: Long, seasonNumber: Int) = viewModelScope.launch {
        repository.moveSeasonBackToWatchlist(showId, seasonNumber)
    }

    fun skipSeason(showId: Long, seasonNumber: Int) = viewModelScope.launch {
        repository.markSeasonSkipped(
            showId = showId,
            seasonNumber = seasonNumber
        )
    }

    fun drop(id: Long) = viewModelScope.launch {
        repository.markDropped(id)
    }

    fun dropSeason(showId: Long, seasonNumber: Int) = viewModelScope.launch {
        repository.markSeasonDropped(showId, seasonNumber)
    }

    fun unDrop(id: Long) = viewModelScope.launch {
        repository.unDrop(id)
    }

    fun unDropSeason(showId: Long, seasonNumber: Int) = viewModelScope.launch {
        repository.unDropSeason(showId, seasonNumber)
    }

    fun setSeasonUserRating(
        showId: Long,
        seasonNumber: Int,
        rating: Double
    ) = viewModelScope.launch {
        repository.updateSeasonUserRating(
            showId,
            seasonNumber,
            rating
        )
    }

    fun removeSeasonUserRating(
        showId: Long,
        seasonNumber: Int
    ) = viewModelScope.launch {
        repository.clearSeasonUserRating(showId, seasonNumber)
    }

    fun setSeasonNote(
        showId: Long,
        seasonNumber: Int,
        note: String
    ) = viewModelScope.launch {
        repository.updateSeasonNote(
            showId,
            seasonNumber,
            note
        )
    }

    fun updateSeasonFinishedDate(
        showId: Long,
        seasonNumber: Int,
        finished: Instant?
    ) = viewModelScope.launch {
        repository.updateSeasonFinishedDate(
            showId,
            seasonNumber,
            finished
        )
    }

    fun updateFinishedTvSeasonDate(
        showId: Long,
        seasonNumber: Int,
        finished: Instant?,
        releaseDates: Boolean = false,
        clearEpisodeDates: Boolean = false
    ) = viewModelScope.launch {
        repository.updateFinishedTvSeasonDate(
            showId = showId,
            seasonNumber = seasonNumber,
            finished = finished,
            releaseDates = releaseDates,
            clearEpisodeDates = clearEpisodeDates
        )
    }


    fun updateFinishedDate(id: Long, finished: Instant?) = viewModelScope.launch {
        repository.updateFinishedDate(id, finished)
    }

    fun deleteSeason(
        showId: Long,
        seasonNumber: Int
    ) = viewModelScope.launch {
        repository.deleteSeason(showId, seasonNumber)
    }

}

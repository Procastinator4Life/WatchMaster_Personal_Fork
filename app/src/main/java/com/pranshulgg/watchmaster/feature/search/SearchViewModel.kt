package com.pranshulgg.watchmaster.feature.search

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranshulgg.watchmaster.data.repository.WatchlistRepository
import com.pranshulgg.watchmaster.core.network.TvSeasonDto
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.repository.SearchRepository
import com.pranshulgg.watchmaster.data.repository.TvRepository
import com.pranshulgg.watchmaster.feature.shared.WatchlistViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: SearchRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    var results by mutableStateOf<List<SearchItem>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var showNoResultsSnack by mutableStateOf(false)
        private set

    var seasonData by mutableStateOf<List<TvSeasonDto>>(emptyList())
        private set

    var seasonLoading by mutableStateOf(false)
        private set

    var seasonLoadFailed by mutableStateOf(false)
        private set

    fun onQueryChange(q: String) {
        query = q
    }

    private val mockResults = listOf(
        SearchItem(
            id = 243875,
            title = "Georgie & Mandy's First Marriage",
            posterPath = "/3z2mYFxUkzanb2eeIcVyfJq0G3q.jpg",
            mediaType = "movie",
            releaseDate = "2024-10-17",
            genreIds = listOf(35),
            overview = "Georgie and Mandy raise their young family in Texas while navigating the challenges of adulthood, parenting, and marriage.",
            backdropPath = "/pCr3YYJkkLt9fdGVD7I2Z1l6lzK.jpg"
        ),
        SearchItem(
            id = 12,
            title = "Finding Nemo",
            posterPath = "/eHuGQ10FUzK1mdOY69wF5pGgEf5.jpg",
            mediaType = "movie",
            releaseDate = "2003-05-30",
            genreIds = listOf(16, 10751),
            overview = "Nemo, an adventurous young clownfish, is unexpectedly taken from his Great Barrier Reef home to a dentist's office aquarium. It's up to his worrisome father Marlin and a friendly but forgetful fish Dory to bring Nemo home -- meeting vegetarian sharks, surfer dude turtles, hypnotic jellyfish, hungry seagulls, and more along the way.",
            backdropPath = ""

        ),
        SearchItem(
            id = 888,
            title = "Spider-Man",
            posterPath = null,
            mediaType = "tv",
            releaseDate = "",
            genreIds = listOf(),
            overview = "",
            backdropPath = ""


        ),
        SearchItem(
            id = 557,
            title = "Spider-Man",
            posterPath = "/kjdJntyBeEvqm9w97QGBdxPptzj.jpg",
            mediaType = "movie",
            releaseDate = "2002-05-01",
            genreIds = listOf(28, 878),
            overview = "",
            backdropPath = ""

        ),
        SearchItem(
            id = 557,
            title = "Spider-Man",
            posterPath = "/kjdJntyBeEvqm9w97QGBdxPptzj.jpg",
            mediaType = "movie",
            releaseDate = "2002-05-01",
            genreIds = listOf(28, 878),
            overview = "",
            backdropPath = ""

        ),
    )

//    init {
//        results = mockResults
//        loading = false
//    }

    private val seasonCache = mutableMapOf<Long, List<TvSeasonDto>>()


    fun search(type: SearchType = SearchType.MULTI) {
        if (query.isBlank()) return

        viewModelScope.launch {
            loading = true
            showNoResultsSnack = false
            try {
                val res = repo.search(query, type)
                results = res
                showNoResultsSnack = res.isEmpty()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SnackbarManager.show("Couldn't search. Check your connection and try again")
            } finally {
                loading = false
            }
        }
    }


    fun fetchSeasonData(tvId: Long) {
        seasonCache[tvId]?.let {
            seasonData = it
            seasonLoadFailed = false
            return
        }

        viewModelScope.launch {
            seasonLoading = true
            seasonLoadFailed = false
            try {
                val data = repo.getSeasonData(tvId)
                seasonCache[tvId] = data
                seasonData = data
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                seasonData = emptyList()
                seasonLoadFailed = true
                SnackbarManager.show("Couldn't load seasons. Check your connection")
            } finally {
                seasonLoading = false
            }
        }
    }

    fun addToWatchlist(
        item: SearchItem,
        tvDetails: List<TvSeasonDto>? = null,
        watchlistViewModel: WatchlistViewModel
    ) {
        viewModelScope.launch {
            val exists = watchlistViewModel.exists(item.id)

            // Never re-insert an existing TV show's seasons from search.
            // REPLACE would reset season state and could destroy progress.
            if (exists) return@launch

            if (item.mediaType == "tv") {
                if (seasonLoading) {
                    SnackbarManager.show("Still loading seasons, try again in a moment")
                    return@launch
                }

                if (seasonLoadFailed) {
                    SnackbarManager.show("Couldn't load seasons. Close this and tap the show again")
                    return@launch
                }

                if (tvDetails.isNullOrEmpty()) {
                    SnackbarManager.show("No season info available for this show yet")
                    return@launch
                }
            }

            watchlistRepository.addFromSearch(
                item = item,
                tvDetails = if (item.mediaType == "tv") tvDetails else null
            )
        }
    }

    fun addSeasonToWatchlist(id: Long, tvDetails: List<TvSeasonDto>) {
        viewModelScope.launch {
            watchlistRepository.insertSeason(id, tvDetails)
        }
    }

    suspend fun onSearchItemClick(
        item: SearchItem,
        watchlistViewModel: WatchlistViewModel,
    ) {
        updateSelectedItem(item)
        if (item.mediaType == "tv") {
            fetchSeasonData(item.id)
        }
        showSheet()
    }


    private val _uiState = mutableStateOf(SearchUiState())
    val uiState: MutableState<SearchUiState> = _uiState


    fun updateSelectedItem(item: SearchItem) {
        _uiState.value = _uiState.value.copy(selectedItem = item)
    }

    fun updateSelectedSeasonList(list: List<TvSeasonDto>?) {
        _uiState.value = _uiState.value.copy(selectedSeasonList = list)
    }

    fun showSheet() {
        _uiState.value = _uiState.value.copy(isSheetOpen = true)
    }

    fun hideSheet() {
        _uiState.value = _uiState.value.copy(isSheetOpen = false)
    }
}

package com.pranshulgg.watchmaster.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranshulgg.watchmaster.core.model.Trending
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.data.local.dao.SeasonDao
import com.pranshulgg.watchmaster.data.local.dao.TvBundleDao
import com.pranshulgg.watchmaster.data.local.dao.TvEpisodeDao
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.data.repository.TrendingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import com.pranshulgg.watchmaster.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.map

data class WatchNextItem(
    val showId: Long,
    val showTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String,
    val stillPath: String?,
    val backdropPath: String?,
    val episodeEntity: TvEpisodeEntity,
    val status: WatchStatus = WatchStatus.WATCHING,
    val lastWatchedAt: Long = 0L,
    val isReleased: Boolean = true,
    val isSeasonFinale: Boolean = false
) {
    val displayImagePath: String?
        get() = stillPath?.ifEmpty { null } ?: backdropPath?.ifEmpty { null }
}

data class UpcomingEpisodeCalendarItem(
    val date: LocalDate,
    val showId: Long,
    val showTitle: String,
    val episodeName: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val backdropPath: String?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val trendingRepo: TrendingRepository,
    private val seasonDao: SeasonDao,
    private val tvEpisodeDao: TvEpisodeDao,
    private val tvBundleDao: TvBundleDao,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    var trendingDay by mutableStateOf<List<Trending>>(emptyList())
        private set

    val watchNextItems: StateFlow<List<WatchNextItem>> = seasonDao.getAllSeasons()
        .flatMapLatest { allSeasons ->
            val watchingSeasons = allSeasons.filter { it.status == WatchStatus.WATCHING }
            if (watchingSeasons.isEmpty()) {
                flowOf(emptyList())
            } else {
                val episodeFlows = watchingSeasons.map { season ->
                    tvEpisodeDao.getEpisodesForSeason(
                        showId = season.showId,
                        seasonNumber = season.seasonNumber
                    )
                }

                combine(episodeFlows) { episodesListPerSeason ->
                    val watchNextList = mutableListOf<WatchNextItem>()
                    val today = LocalDate.now()

                    watchingSeasons.forEachIndexed { index, season ->
                        val episodes = episodesListPerSeason[index]

                        // Find the first unwatched episode in order
                        val nextEpisode = episodes.firstOrNull { !it.isWatched }

                        if (nextEpisode != null) {
                            val maxLastWatched = episodes
                                .filter { it.isWatched }
                                .maxOfOrNull { it.lastModifiedAt } ?: 0L

                            // Safely parse air date
                            val airDate = nextEpisode.air_date?.let { dateStr ->
                                runCatching {
                                    LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                                }.getOrNull()
                            }

                            // Strictly verify if episode has aired (Must have a valid airDate on or before today)
                            val isReleased = airDate != null && !airDate.isAfter(today)

                            // REQUIREMENT 1: Only add to Watch Next if the episode is strictly released
                            if (isReleased) {
                                watchNextList.add(
                                    WatchNextItem(
                                        showId = season.showId,
                                        showTitle = season.name,
                                        seasonNumber = season.seasonNumber,
                                        episodeNumber = nextEpisode.episode_number,
                                        episodeName = nextEpisode.name,
                                        stillPath = nextEpisode.still_path,
                                        backdropPath = null,
                                        episodeEntity = nextEpisode,
                                        lastWatchedAt = maxLastWatched,
                                        isReleased = true,
                                        isSeasonFinale = episodes.size > 1 &&
                                            nextEpisode.episode_number ==
                                            episodes.maxOf { it.episode_number }
                                    )
                                )
                            }
                        }
                    }
                    watchNextList
                }
            }
        }

        .flatMapLatest { list ->
            kotlinx.coroutines.flow.flow {
                val watchlistById = watchlistRepository.getAllItemsOnce().associateBy { it.id }

                val updatedList = coroutineScope {
                    list.map { item ->
                        async {
                            val bundle = tvBundleDao.getById(item.showId)
                            item.copy(
                                showTitle = bundle?.name ?: watchlistById[item.showId]?.title ?: item.showTitle,
                                backdropPath = bundle?.backdrop_path ?: watchlistById[item.showId]?.backdropPath ?: item.backdropPath
                            )
                        }
                    }.awaitAll()
                }

                // Keep your logic: Latest episode release / latest watched timestamp at the top!
                val sortedList = updatedList.sortedWith(
                    compareByDescending<WatchNextItem> { item ->
                        val airDateMillis = item.episodeEntity.air_date
                            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                            ?.atStartOfDay(java.time.ZoneId.systemDefault())
                            ?.toInstant()
                            ?.toEpochMilli()
                            ?: 0L

                        // Picks whichever is newer: release date or when you last watched it
                        maxOf(airDateMillis, item.lastWatchedAt)
                    }
                    .thenBy { it.showTitle } // Tie-breaker: If both are equal, keep them alphabetical so they don't jump!
                )

                emit(sortedList)
            }
        }
        
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val upcomingEpisodeCalendar: StateFlow<List<UpcomingEpisodeCalendarItem>> =
        seasonDao.getAllSeasons()
            .flatMapLatest { allSeasons ->
                val activeSeasons = allSeasons.filter { it.status != WatchStatus.FINISHED }

                if (activeSeasons.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val episodeFlows = activeSeasons.map { season ->
                        tvEpisodeDao.getEpisodesForSeason(
                            showId = season.showId,
                            seasonNumber = season.seasonNumber
                        )
                    }

                    combine(episodeFlows) { episodesBySeason ->
                        val today = LocalDate.now()
                        val start = today.plusDays(1) // Starts tomorrow (e.g. Friday)
                        val end = today.plusDays(7)   // Covers the next 7 days (e.g. through next Thursday)

                        // Fetch all show bundles concurrently instead of one-by-one in the loop
                        val watchlistById = watchlistRepository.getAllItemsOnce().associateBy { it.id }
                        val bundlesByShowId = coroutineScope {
                            activeSeasons
                                .map { it.showId }
                                .distinct()
                                .associateWith { showId -> async { tvBundleDao.getById(showId) } }
                                .mapValues { it.value.await() }
                        }

                        val result = mutableListOf<UpcomingEpisodeCalendarItem>()

                        activeSeasons.forEachIndexed { index, season ->
                            val bundle = bundlesByShowId[season.showId]

                            episodesBySeason[index].forEach { episode ->
                                val date = episode.air_date
                                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                                // Excludes 'Today' (date < start) and shows the next 7 days starting tomorrow
                                if (date != null && !date.isBefore(start) && !date.isAfter(end)) {
                                    result += UpcomingEpisodeCalendarItem(
                                        date = date,
                                        showId = season.showId,
                                        showTitle = bundle?.name ?: watchlistById[season.showId]?.title ?: season.name,
                                        episodeName = episode.name,
                                        seasonNumber = season.seasonNumber,
                                        episodeNumber = episode.episode_number,
                                        backdropPath = bundle?.backdrop_path ?: watchlistById[season.showId]?.backdropPath
                                    )
                                }
                            }
                        }

                        result.sortedWith(
                            compareBy<UpcomingEpisodeCalendarItem> { it.date }
                                .thenBy { it.showTitle }
                                .thenBy { it.seasonNumber }
                                .thenBy { it.episodeNumber }
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val upcomingMovies: StateFlow<List<UpcomingMovieItem>> = watchlistRepository.getWatchlist()
        .map { watchlist ->
            val today = LocalDate.now()

            watchlist
                .filter { it.mediaType == "movie" }
                .mapNotNull { entity ->
                    val date = entity.releaseDate
                        ?.let { runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() }

                    if (date != null && !date.isBefore(today)) {
                        UpcomingMovieItem(
                            movieId = entity.id,
                            title = entity.title,
                            posterPath = entity.posterPath,
                            releaseDate = date
                        )
                    } else {
                        null
                    }
                }
                .sortedWith(
                    compareBy<UpcomingMovieItem> { it.releaseDate }
                        .thenBy { it.title } // Tie-breaker for identical movie dates
                )
                .take(10)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val upcomingShows: StateFlow<List<UpcomingShowItem>> = seasonDao.getAllSeasons()
        .flatMapLatest { allSeasons ->
            if (allSeasons.isEmpty()) {
                flowOf(emptyList())
            } else {
                val episodeFlows = allSeasons.map { season ->
                    tvEpisodeDao.getEpisodesForSeason(
                        showId = season.showId,
                        seasonNumber = season.seasonNumber
                    )
                }

                combine(episodeFlows) { episodesBySeason ->
                    val today = LocalDate.now()
                    val candidates = mutableListOf<Pair<com.pranshulgg.watchmaster.data.local.entity.SeasonEntity, LocalDate>>()

                    allSeasons.forEachIndexed { index, season ->
                        if (season.seasonNumber == 0) return@forEachIndexed
                        val episodes = episodesBySeason[index]
                        val firstEpisode = episodes.minByOrNull { it.episode_number }

                        val airDate = firstEpisode?.air_date?.let { dateStr ->
                            runCatching {
                                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                            }.getOrNull()
                        }

                        // Only seasons with a KNOWN date that hasn't already passed
                        if (airDate != null && !airDate.isBefore(today)) {
                            candidates += season to airDate
                        }
                    }

                    candidates
                }
            }
        }
        .flatMapLatest { candidates ->
            kotlinx.coroutines.flow.flow {
                val watchlistById = watchlistRepository.getAllItemsOnce().associateBy { it.id }
                val updated = coroutineScope {
                    candidates.map { (season, date) ->
                        async {
                            val bundle = tvBundleDao.getById(season.showId)
                            UpcomingShowItem(
                                showId = season.showId,
                                title = bundle?.name ?: watchlistById[season.showId]?.title ?: season.name,
                                posterPath = bundle?.poster_path ?: watchlistById[season.showId]?.posterPath,
                                releaseDate = date,
                                seasonNumber = season.seasonNumber
                            )
                        }
                    }.awaitAll()
                }

                emit(
                    updated
                        .sortedWith(
                            compareBy<UpcomingShowItem> { it.releaseDate }
                                .thenBy { it.title }
                        )
                        .distinctBy { it.showId }
                        .take(10)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleEpisodeWatched(episode: TvEpisodeEntity) {
        viewModelScope.launch {
            val isNowWatched = !episode.isWatched
            tvEpisodeDao.updateEpisodeStatus(
                epId = episode.epId,
                watched = isNowWatched,
                timestamp = if (isNowWatched) System.currentTimeMillis() else 0L
            )
        }
    }

    fun getTrending(mediaType: String, page: Int, trendingType: String) {
        if (trendingDay.isNotEmpty()) return

        viewModelScope.launch {
            val trending = try {
                trendingRepo.fetchTrending(mediaType, page, trendingType)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                return@launch
            }

            trendingDay = trending
        }
    }

    fun clearTrending(trendingType: String) {
        viewModelScope.launch {
            trendingRepo.clearTrending(trendingType)
        }
    }
    
    init {
        viewModelScope.launch {
            try {
                watchlistRepository.ensureEpisodesForLibrary()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }
}
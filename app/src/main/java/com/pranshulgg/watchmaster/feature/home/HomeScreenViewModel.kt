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

data class WatchNextItem(
    val showId: Long,
    val showTitle: String,
    val seasonNumber: Int,
    val seasonId: Long,
    val episodeNumber: Int,
    val episodeName: String,
    val stillPath: String?,
    val backdropPath: String?,
    val episodeEntity: TvEpisodeEntity,
    val status: WatchStatus = WatchStatus.WATCHING,
    val lastWatchedAt: Long = 0L,
    val isReleased: Boolean = true
) {
    val displayImagePath: String?
        get() = stillPath?.ifEmpty { null } ?: backdropPath?.ifEmpty { null }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val trendingRepo: TrendingRepository,
    private val seasonDao: SeasonDao,
    private val tvEpisodeDao: TvEpisodeDao,
    private val tvBundleDao: TvBundleDao
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
                    tvEpisodeDao.getEpisodesForSeason(season.seasonId)
                }

                combine(episodeFlows) { episodesListPerSeason ->
                    val watchNextList = mutableListOf<WatchNextItem>()
                    val today = LocalDate.now()

                    watchingSeasons.forEachIndexed { index, season ->
                        val episodes = episodesListPerSeason[index]
                        val nextEpisode = episodes.firstOrNull { !it.isWatched } ?: episodes.firstOrNull()

                        if (nextEpisode != null) {
                            // Find latest watched time across this season
                            val maxLastWatched = episodes
                                .filter { it.isWatched }
                                .maxOfOrNull { it.lastWatchedAt } ?: 0L

                            // Parse air_date ("YYYY-MM-DD") to check if available
                            val isReleased = nextEpisode.air_date?.let { dateStr ->
                                runCatching {
                                    val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                                    !date.isAfter(today)
                                }.getOrDefault(true)
                            } ?: true

                            watchNextList.add(
                                WatchNextItem(
                                    showId = season.showId,
                                    showTitle = season.name,
                                    seasonNumber = season.seasonNumber,
                                    seasonId = season.seasonId,
                                    episodeNumber = nextEpisode.episode_number,
                                    episodeName = nextEpisode.name,
                                    stillPath = nextEpisode.still_path,
                                    backdropPath = season.posterPath,
                                    episodeEntity = nextEpisode,
                                    lastWatchedAt = maxLastWatched,
                                    isReleased = isReleased
                                )
                            )
                        }
                    }
                    watchNextList
                }
            }
        }
        .flatMapLatest { list ->
            kotlinx.coroutines.flow.flow {
                val updatedList = list.map { item ->
                    val bundle = tvBundleDao.getById(item.showId)
                    val realTitle = bundle?.name ?: item.showTitle
                    item.copy(showTitle = realTitle)
                }

                // 1. Released episodes take priority over unreleased
                // 2. Most recently watched show jumps to the front
                val sortedList = updatedList.sortedWith(
                    compareByDescending<WatchNextItem> { it.isReleased }
                        .thenByDescending { it.lastWatchedAt }
                )

                emit(sortedList)
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
}
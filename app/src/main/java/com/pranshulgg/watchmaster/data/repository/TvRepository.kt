package com.pranshulgg.watchmaster.data.repository

import android.util.Log
import androidx.room.Transaction
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.data.local.dao.SeasonDao
import com.pranshulgg.watchmaster.data.local.dao.TvBundleDao
import com.pranshulgg.watchmaster.data.local.dao.TvEpisodeDao
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.TvBundle
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.data.local.entity.toDomain
import com.pranshulgg.watchmaster.data.local.mapper.toDomain
import com.pranshulgg.watchmaster.data.local.mapper.toEntity
import com.pranshulgg.watchmaster.core.network.TmdbApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow

class TvRepository(
    private val api: TmdbApi,
    private val dao: TvBundleDao,
    private val episodeDao: TvEpisodeDao,
    private val seasonDao: SeasonDao
) {

    suspend fun getWholeTvData(tvId: Long, forceFetch: Boolean = false): TvBundle {
        if (!forceFetch) dao.getById(tvId)?.let {
            return it.toDomain()
        }

        // A forced refresh rebuilds the bundle straight from TMDB, which never
        // carries a backdropOverride (that's a purely local choice). Preserve
        // whatever override was already saved so a background metadata refresh
        // can't silently revert the user's manual backdrop selection.
        val existingOverride = dao.getById(tvId)?.backdropOverride

        val response = api.getWholeTvData(tvId)
        val body = response.body() ?: throw Exception("TV not found")
        val domain = body.toDomain().copy(backdropOverride = existingOverride)
        dao.insert(domain.toEntity())

        return domain
    }

    suspend fun fetchSeasonList(tvId: Long): List<com.pranshulgg.watchmaster.core.network.TvSeasonDto> {
        val response = api.getTvSeasons(tvId)
        return response.body()?.seasons ?: emptyList()
    }

    suspend fun deleteCachedTv(id: Long) {
        dao.deleteById(id)
    }

    suspend fun fetchTvBackdrops(tvId: Long): List<com.pranshulgg.watchmaster.core.network.TvBackdropDto> {
        val response = api.getTvImages(tvId)
        return response.body()?.backdrops ?: emptyList()
    }

    suspend fun setBackdropOverride(tvId: Long, backdropPath: String?) {
        dao.updateBackdropOverride(tvId, backdropPath)
    }

    private companion object {
        // Shared by every TvRepository instance, even if more than one is created
        val episodeWriteMutex = Mutex()
    }

    suspend fun fetchEpisodes(
        tvId: Long,
        seasonNumber: Int
    ) {
        // The download itself can run in parallel
        val response = api.getTvSeasonEpisodes(tvId, seasonNumber)
        val body = response.body() ?: error("TV episodes not found")
        val domain = body.episodes

        // Only the look-then-save step is one at a time, so two downloads of the same
        // season can't both save a full set of episodes.
        episodeWriteMutex.withLock {
            // Refreshing TMDB episode metadata must never reset local watch state.
            // Match existing rows by show + season + episode number and reuse their
            // generated epId so Room replaces the metadata instead of creating duplicates.
            val existing = episodeDao.getEpisodesForSeasonOnce(
                showId = tvId,
                seasonNumber = seasonNumber
            ).associateBy { it.episode_number }

            val eps = domain.map { episode ->
                val local = existing[episode.episode_number]
                TvEpisodeEntity(
                    epId = local?.epId ?: 0L,
                    showId = tvId,
                    seasonNumber = seasonNumber,
                    air_date = episode.air_date,
                    episode_number = episode.episode_number,
                    name = episode.name,
                    overview = episode.overview,
                    still_path = episode.still_path,
                    runtime = episode.runtime,
                    isWatched = local?.isWatched ?: false,
                    lastWatchedAt = local?.lastWatchedAt ?: 0L,
                    lastModifiedAt = local?.lastModifiedAt ?: 0L
                )
            }

            episodeDao.insertEpisodes(eps)
            removeDuplicateEpisodes(tvId, seasonNumber)
        }
    }

    // Keeps one row per episode number. Prefers a watched one, then the most recently changed.
    private suspend fun removeDuplicateEpisodes(tvId: Long, seasonNumber: Int) {
        val stored = episodeDao.getEpisodesForSeasonOnce(
            showId = tvId,
            seasonNumber = seasonNumber
        )

        val keepers = stored
            .groupBy { it.episode_number }
            .map { (_, rows) ->
                rows.sortedWith(
                    compareByDescending<TvEpisodeEntity> { it.isWatched }
                        .thenByDescending { it.lastModifiedAt }
                        .thenBy { it.epId }
                ).first()
            }

        if (keepers.size == stored.size) return

        episodeDao.deleteEpisodesForSeason(showId = tvId, seasonNumber = seasonNumber)
        episodeDao.insertEpisodes(keepers)
    }

    suspend fun dedupeEpisodes(tvId: Long, seasonNumber: Int) {
        episodeWriteMutex.withLock { removeDuplicateEpisodes(tvId, seasonNumber) }
    }

    suspend fun updateEpisodeWatchedDate(
        epId: Long,
        timestamp: Long
    ) {
        episodeDao.updateEpisodeWatchedDate(
            epId = epId,
            timestamp = timestamp
        )
    }

    suspend fun bundleCachedAt(tvId: Long): Long? = dao.getById(tvId)?.cachedAt
    
    @Transaction
    suspend fun markEpWatched(
        epId: Long,
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        episodeDao.updateEpisodeStatus(epId, true)

        seasonDao.updateLastEpWatched(
            showId = showId,
            seasonNumber = seasonNumber,
            epNumber = episodeNumber
        )

        val unwatchedCount = episodeDao.getUnwatchedCount(
            showId = showId,
            seasonNumber = seasonNumber
        )

        if (unwatchedCount == 0) {
            seasonDao.updateSeasonStatus(
                showId = showId,
                seasonNumber = seasonNumber,
                status = WatchStatus.FINISHED,
                finished = java.time.Instant.now()
            )
        }
    }

    @Transaction
    suspend fun markEpUnWatched(
        epId: Long,
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        episodeDao.updateEpisodeStatus(epId, false)

        val newLastWatched = (episodeNumber - 1).takeIf { it > 0 }

        seasonDao.updateLastEpWatched(
            showId = showId,
            seasonNumber = seasonNumber,
            epNumber = newLastWatched
        )

        val season = seasonDao.getSeason(
            showId = showId,
            seasonNumber = seasonNumber
        ) ?: return

        if (season.status == WatchStatus.FINISHED) {
            seasonDao.updateSeasonStatus(
                showId = showId,
                seasonNumber = seasonNumber,
                status = WatchStatus.WATCHING
            )
        }
    }

    suspend fun updateLastEpWatched(
        showId: Long,
        seasonNumber: Int,
        epNumber: Int?
    ) {
        seasonDao.updateLastEpWatched(
            showId = showId,
            seasonNumber = seasonNumber,
            epNumber = epNumber
        )
    }

    @Transaction
    suspend fun markAllEpWatched(
        showId: Long,
        seasonNumber: Int,
        lastEpNumber: Int
    ) {
        episodeDao.markAllEpWatched(
            showId = showId,
            seasonNumber = seasonNumber
        )

        seasonDao.updateLastEpWatched(
            showId = showId,
            seasonNumber = seasonNumber,
            epNumber = lastEpNumber
        )

        seasonDao.updateSeasonStatus(
            showId = showId,
            seasonNumber = seasonNumber,
            status = WatchStatus.FINISHED,
            finished = java.time.Instant.now()
        )
    }

    @Transaction
    suspend fun markAllEpUnWatched(
        showId: Long,
        seasonNumber: Int
    ) {
        seasonDao.updateSeasonStatus(
            showId = showId,
            seasonNumber = seasonNumber,
            status = WatchStatus.WANT_TO_WATCH
        )

        seasonDao.updateLastEpWatched(
            showId = showId,
            seasonNumber = seasonNumber,
            epNumber = null
        )

        episodeDao.markAllEpUnWatched(
            showId = showId,
            seasonNumber = seasonNumber
        )
    }

    private val loadingSeasons = mutableSetOf<String>()

    suspend fun refreshEpisodes(
        tvId: Long,
        seasonNumber: Int
    ) {
        fetchEpisodes(
            tvId = tvId,
            seasonNumber = seasonNumber
        )
    }

    suspend fun ensureEpisodesFetched(
        tvId: Long,
        seasonNumber: Int,
    ) {
        val season = seasonDao.getSeason(
            showId = tvId,
            seasonNumber = seasonNumber
        ) ?: return

        val loadingKey = "$tvId:$seasonNumber"

        if (!episodeDao.hasEpisodes(tvId, seasonNumber) &&
            !loadingSeasons.contains(loadingKey)
        ) {
            loadingSeasons.add(loadingKey)

            try {
                fetchEpisodes(
                    tvId = tvId,
                    seasonNumber = seasonNumber
                )
            } finally {
                loadingSeasons.remove(loadingKey)
            }
        } else {
            Log.d(
                "TvRepository",
                "Episodes already fetched for S$seasonNumber of show $tvId"
            )
        }
    }

    fun getEpisodesForSeason(
        showId: Long,
        seasonNumber: Int
    ): Flow<List<TvEpisodeEntity>> {
        return episodeDao.getEpisodesForSeason(
            showId = showId,
            seasonNumber = seasonNumber
        )
    }

    suspend fun markEpWatchedFromCount(
        showId: Long,
        seasonNumber: Int,
        count: Int
    ) {
        episodeDao.markEpWatchedFromCount(
            showId = showId,
            seasonNumber = seasonNumber,
            count = count
        )
    }

    suspend fun refreshSeasonData(season: SeasonEntity) {
        fetchEpisodes(
            tvId = season.showId,
            seasonNumber = season.seasonNumber
        )
}

}
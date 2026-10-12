package com.pranshulgg.watchmaster.data.repository

import com.pranshulgg.watchmaster.data.local.dao.WatchlistDao
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.network.TvSeasonDto
import com.pranshulgg.watchmaster.data.local.dao.SeasonDao
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.feature.search.SearchItem
import kotlinx.coroutines.flow.Flow
import com.pranshulgg.watchmaster.data.local.entity.DeletedItemEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Instant
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

import com.pranshulgg.watchmaster.data.local.dao.TvEpisodeDao

class WatchlistRepository(
    private val dao: WatchlistDao,
    private val seasonDao: SeasonDao,
    private val tvEpisodeDao: TvEpisodeDao,
    private val movieRepository: MovieRepository,
    private val tvRepository: TvRepository
) {

    suspend fun addFromSearch(
        item: SearchItem,
        tvDetails: List<TvSeasonDto>? = null
    ) {
        dao.insert(
            WatchlistItemEntity(
                id = item.id,
                mediaType = item.mediaType,
                title = item.title,
                overview = item.overview,
                posterPath = item.posterPath,
                genreIds = item.genreIds,
                releaseDate = item.releaseDate,
                addedDate = Instant.now(),
                avgRating = item.avg_rating,
                backdropPath = item.backdropPath,
                lastModifiedAt = System.currentTimeMillis(),
            )
        )

        if (item.mediaType == "tv" && tvDetails != null) {
            insertSeason(item.id, tvDetails)
        }
    }

    suspend fun insertSeason(showId: Long, tvDetails: List<TvSeasonDto>) {
        val seasons = tvDetails.map {
            SeasonEntity(
                seasonNumber = it.season_number,
                name = it.name,
                episodeCount = it.episode_count,
                airDate = it.air_date,
                posterPath = it.poster_path,
                showId = showId,
                seasonAddedDate = Instant.now(),
                seasonAvgRating = it.vote_average,
                cachedAt = System.currentTimeMillis()
            )
        }

        seasonDao.insertSeasons(seasons)
    }


    fun getWatchlist() = dao.getAll()
    suspend fun getAllItemsOnce() = dao.getAllOnce()

    private suspend fun <T> attempt(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    suspend fun refreshMetadata(item: WatchlistItemEntity) {
        try {
            if (item.mediaType == "movie") {
                attempt {
                    val movie = movieRepository.getWholeMovieData(item.id, forceFetch = true)
                    dao.updateMetadata(
                        id = item.id,
                        title = movie.title,
                        overview = movie.overview,
                        posterPath = movie.poster_path,
                        backdropPath = movie.backdrop_path,
                        genreIds = movie.genres.map { it.id },
                        releaseDate = item.releaseDate,
                        avgRating = item.avgRating
                    )
                }
            } else if (item.mediaType == "tv") {
                // 1. Show details. A failure here must not stop the episode refresh below
                attempt {
                    val tv = tvRepository.getWholeTvData(item.id, forceFetch = true)
                    dao.updateMetadata(
                        id = item.id,
                        title = tv.name,
                        overview = tv.overview,
                        posterPath = tv.poster_path,
                        backdropPath = tv.backdrop_path,
                        genreIds = tv.genres.map { it.id },
                        releaseDate = item.releaseDate,
                        avgRating = item.avgRating
                    )
                }

                // 2. Season list
                val current = seasonDao.getSeasonsForShowSync(item.id)
                val remote = attempt { tvRepository.fetchSeasonList(item.id) }.orEmpty()

                val seasonsNow = if (remote.isEmpty()) {
                    current
                } else {
                    val currentByNumber = current.associateBy { it.seasonNumber }
                    val merged = remote.map { remoteSeason ->
                        val local = currentByNumber[remoteSeason.season_number]
                        SeasonEntity(
                            showId = item.id,
                            seasonNumber = remoteSeason.season_number,
                            name = remoteSeason.name,
                            episodeCount = remoteSeason.episode_count,
                            airDate = remoteSeason.air_date,
                            posterPath = remoteSeason.poster_path,
                            seasonAddedDate = local?.seasonAddedDate ?: Instant.now(),
                            status = local?.status ?: WatchStatus.WANT_TO_WATCH,
                            seasonStartedDate = local?.seasonStartedDate,
                            seasonFinishedDate = local?.seasonFinishedDate,
                            seasonInterruptedAt = local?.seasonInterruptedAt,
                            lastEpWatched = local?.lastEpWatched,
                            seasonUserRating = local?.seasonUserRating,
                            seasonNotes = local?.seasonNotes,
                            seasonAvgRating = remoteSeason.vote_average,
                            cachedAt = local?.takeIf { it.status == WatchStatus.FINISHED }?.cachedAt
                                ?: System.currentTimeMillis(),
                            lastModifiedAt = local?.lastModifiedAt ?: 0L
                        )
                    }
                    seasonDao.insertSeasons(merged)
                    merged
                }

                // 3. Episodes, only for seasons that can still change
                seasonsNow
                    .filter { it.status != WatchStatus.FINISHED && it.status != WatchStatus.SKIPPED }
                    .forEach { season ->
                        attempt {
                            tvRepository.refreshEpisodes(
                                tvId = item.id,
                                seasonNumber = season.seasonNumber
                            )
                        }
                    }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Metadata refresh is best-effort; cached library data remains usable.
        }
    }

    suspend fun refreshLibraryMetadata() {
        val started = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val today = java.time.LocalDate.now()

        fun due(cachedAt: Long?, hours: Long) =
            cachedAt == null || started - cachedAt >= hours * hour

        val active = mutableListOf<WatchlistItemEntity>()
        val settled = mutableListOf<WatchlistItemEntity>()

        dao.getAllOnce().forEach { item ->
            if (item.mediaType == "tv") {
                val seasons = seasonDao.getSeasonsForShowSync(item.id)
                val canChange = seasons.any {
                    it.status != WatchStatus.FINISHED && it.status != WatchStatus.SKIPPED
                }

                if (canChange) {
                    active += item
                } else {
                    val newest = seasons
                        .filter { it.seasonNumber != 0 }
                        .mapNotNull { s ->
                            s.airDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                        }
                        .maxOrNull()
                    val longGone = newest != null && newest.isBefore(today.minusYears(2))

                    if (due(tvRepository.bundleCachedAt(item.id), if (longGone) 24L * 30 else 24L * 7)) {
                        settled += item
                    }
                }
            } else {
                val released = item.releaseDate
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?.let { !it.isAfter(today) }
                    ?: false

                val hours = if (released) 24L * 7 else 12L
                if (due(movieRepository.bundleCachedAt(item.id), hours)) {
                    if (released) settled += item else active += item
                }
            }
        }

        val ordered = active + settled
        val gate = Semaphore(4)

        coroutineScope {
            ordered
                .map { item -> async { gate.withPermit { refreshMetadata(item) } } }
                .awaitAll()
        }

        Log.d(
            "MetadataRefresh",
            "Refreshed ${ordered.size} items (${active.size} active) in " +
                "${System.currentTimeMillis() - started} ms"
        )
    }

    private suspend fun showSnapshot(showId: Long): String {
        val item = dao.getById(showId).first()
        val seasons = seasonDao.getSeasonsForShowSync(showId).sortedBy { it.seasonNumber }

        return buildString {
            append(item?.title).append(item?.overview)
                .append(item?.posterPath).append(item?.backdropPath).append('#')

            seasons.forEach { s ->
                append(s.seasonNumber).append(':').append(s.episodeCount)
                    .append(':').append(s.airDate).append(';')

                tvEpisodeDao.getEpisodesForSeasonOnce(showId, s.seasonNumber).forEach { e ->
                    append(e.episode_number).append(e.name).append(e.overview)
                        .append(e.still_path).append(e.air_date).append('|')
                }
            }
        }
    }

    // true = something changed, false = nothing changed, null = couldn't reach TMDB
    suspend fun refreshShowAndCompare(item: WatchlistItemEntity, seasonNumber: Int): Boolean? {
        try {
            tvRepository.fetchSeasonList(item.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }

        val before = showSnapshot(item.id)

        refreshMetadata(item)

        // Unfinished seasons were refreshed above. Also refresh the season you're viewing
        // if it's finished.
        if (seasonDao.getSeason(item.id, seasonNumber)?.status == WatchStatus.FINISHED) {
            attempt { tvRepository.refreshEpisodes(item.id, seasonNumber) }
        }

        return before != showSnapshot(item.id)
    }

    suspend fun ensureEpisodesForLibrary() {
        dao.getAllOnce()
            .filter { it.mediaType == "tv" }
            .forEach { show ->
                seasonDao.getSeasonsForShowSync(show.id)
                    .filter { it.status != WatchStatus.FINISHED && it.seasonNumber != 0 }
                    .forEach { season ->
                        runCatching {
                            var stored = tvEpisodeDao.getEpisodesForSeasonOnce(
                                show.id, season.seasonNumber
                            )

                            // Download the episode list if it's missing
                            if (stored.isEmpty()) {
                                tvRepository.ensureEpisodesFetched(show.id, season.seasonNumber)
                                stored = tvEpisodeDao.getEpisodesForSeasonOnce(
                                    show.id, season.seasonNumber
                                )
                            }

                            // Clean up an episode that was saved twice
                            if (stored.size != stored.distinctBy { it.episode_number }.size) {
                                tvRepository.dedupeEpisodes(show.id, season.seasonNumber)
                                stored = tvEpisodeDao.getEpisodesForSeasonOnce(
                                    show.id, season.seasonNumber
                                )
                            }

                            // Saved number is ahead of the ticks: tick episodes up to it
                            val saved = season.lastEpWatched ?: 0
                            val last = minOf(saved, stored.size)
                            if (last > 0 && stored.count { it.isWatched } < last) {
                                tvRepository.markEpWatchedFromCount(
                                    show.id, season.seasonNumber, last
                                )
                                stored = tvEpisodeDao.getEpisodesForSeasonOnce(
                                    show.id, season.seasonNumber
                                )
                            }

                            // Ticks are ahead of the saved number: raise the number
                            val highestTicked = stored
                                .filter { it.isWatched }
                                .maxOfOrNull { it.episode_number } ?: 0
                            if (highestTicked > saved) {
                                seasonDao.updateLastEpWatched(
                                    show.id, season.seasonNumber, highestTicked
                                )
                                seasonDao.touchSeason(
                                    show.id, season.seasonNumber, System.currentTimeMillis()
                                )
                            }
                        }
                    }
            }
    }

    suspend fun applyWatchedDates(
        entries: List<TraktDateEntry>,
        dryRun: Boolean
    ): DateImportSummary {
        val cutoff = Instant.parse("1990-01-01T00:00:00Z")
        val cutoffMs = cutoff.toEpochMilli()
        fun missing(i: Instant?) = i == null || i.isBefore(cutoff)

        val movieDates = entries
            .filter { it.t == "movie" }
            .mapNotNull { e -> e.realDate()?.let { e.id to it } }
            .toMap()
        val seasonDates = entries
            .filter { it.t == "tv" && it.s != null }
            .associate { (it.id to it.s!!) to Instant.parse(it.d) }

        var movies = 0
        var seasons = 0
        var episodes = 0
        var noEpisodes = 0

        val items = dao.getAllOnce()

        items.forEach { item ->
            if (item.mediaType == "movie") {
                val date = movieDates[item.id]
                if (date != null &&
                    item.status == WatchStatus.FINISHED &&
                    !item.isDropped &&
                    missing(item.finishedDate)
                ) {
                    movies++
                    if (!dryRun) updateFinishedDate(item.id, date)
                }
            } else if (item.mediaType == "tv") {
                seasonDao.getSeasonsForShowSync(item.id)
                    .filter { it.status == WatchStatus.FINISHED && it.seasonNumber != 0 }
                    .forEach { season ->
                        val date = seasonDates[item.id to season.seasonNumber]
                        if (date != null) {
                            val stored = tvEpisodeDao.getEpisodesForSeasonOnce(
                                item.id, season.seasonNumber
                            )
                            if (stored.isEmpty()) {
                                noEpisodes++
                            } else {
                                val undated = stored.filter {
                                    it.isWatched && it.lastWatchedAt < cutoffMs
                                }
                                val seasonNeeds = missing(season.seasonFinishedDate)

                                if (undated.isNotEmpty() || seasonNeeds) {
                                    seasons++
                                    episodes += undated.size
                                    if (!dryRun) {
                                        if (seasonNeeds) {
                                            updateSeasonFinishedDate(
                                                item.id, season.seasonNumber, date
                                            )
                                        }
                                        undated.forEach {
                                            tvEpisodeDao.updateEpisodeWatchedDateAndStatus(
                                                epId = it.epId,
                                                watched = true,
                                                timestamp = date.toEpochMilli()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }

        val appIds = items.map { it.id }.toSet()
        val notInApp = entries.map { it.id }.distinct().count { it !in appIds }

        return DateImportSummary(movies, seasons, episodes, noEpisodes, notInApp)
    }

    private fun earlier(a: Instant?, b: Instant?): Instant? = when {
        a == null -> b
        b == null -> a
        else -> if (a.isBefore(b)) a else b
    }

    private fun later(a: Instant?, b: Instant?): Instant? = when {
        a == null -> b
        b == null -> a
        else -> if (a.isAfter(b)) a else b
    }

    private suspend fun setImportedSeasonStatus(
        showId: Long,
        seasonNumber: Int,
        status: WatchStatus,
        started: Instant?,
        finished: Instant?
    ) {
        seasonDao.updateSeasonStatus(
            showId = showId,
            seasonNumber = seasonNumber,
            status = status,
            started = started,
            finished = finished
        )
        seasonDao.updateSeasonFinishedDate(
            showId = showId,
            seasonNumber = seasonNumber,
            finished = finished
        )
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
    }

    private suspend fun setImportedShowStatus(
        showId: Long,
        status: WatchStatus,
        started: Instant?,
        finished: Instant?
    ) {
        dao.updateStatus(id = showId, status = status, started = started, finished = finished)
        dao.updateFinishedDate(showId, finished)
        dao.touch(showId, System.currentTimeMillis())
    }

    // A show on the watchlist that nothing has been done with yet
    suspend fun isUntouchedWatchlistShow(id: Long): Boolean {
        val item = dao.getAllOnce().firstOrNull { it.id == id } ?: return false
        if (item.mediaType != "tv" ||
            item.status != WatchStatus.WANT_TO_WATCH ||
            item.isDropped
        ) return false

        val seasons = seasonDao.getSeasonsForShowSync(id)
        if (seasons.any { it.status != WatchStatus.WANT_TO_WATCH }) return false

        return seasons.none { season ->
            tvEpisodeDao.getEpisodesForSeasonOnce(id, season.seasonNumber).any { it.isWatched }
        }
    }

    // Works out each season's status. Nothing is saved except downloaded episode lists.
    suspend fun planTraktProgress(
        showId: Long,
        seasons: List<TvSeasonDto>,
        watched: List<TraktEp>
    ): ProgressPlan {
        val real = seasons
            .filter { it.season_number != 0 && it.episode_count > 0 }
            .sortedBy { it.season_number }
        if (real.isEmpty()) return ProgressPlan(skipReason = "no season info on TMDB")

        val have = watched.groupBy { it.s }.mapValues { (_, list) -> list.map { it.n }.toSet() }

        if (have.keys.any { k -> real.none { it.season_number == k } }) {
            return ProgressPlan(skipReason = "season numbers differ from TMDB")
        }

        val progressed = real.filter { !have[it.season_number].isNullOrEmpty() }
        if (progressed.isEmpty()) return ProgressPlan(skipReason = "nothing watched")

        val complete = mutableSetOf<Int>()
        val partial = mutableListOf<Int>()

        for (season in progressed) {
            val n = season.season_number
            tvRepository.fetchEpisodes(showId, n)
            val episodes = tvRepository.getEpisodesForSeason(showId, n).first()
            if (episodes.isEmpty()) return ProgressPlan(skipReason = "no episode data")

            val h = have[n].orEmpty()
            if (episodes.all { it.episode_number in h }) complete += n else partial += n
        }

        val last = progressed.last().season_number

        if (partial.size > 1) {
            return ProgressPlan(skipReason = "several partly watched seasons")
        }
        if (partial.isNotEmpty() && partial.single() != last) {
            return ProgressPlan(skipReason = "partly watched season isn't the latest")
        }

        val result = real.map { season ->
            val n = season.season_number
            SeasonPlan(
                n,
                when {
                    n in complete -> WatchStatus.FINISHED
                    n in partial -> WatchStatus.WATCHING
                    n < last -> WatchStatus.SKIPPED
                    else -> WatchStatus.WANT_TO_WATCH
                }
            )
        }.toMutableList()

        // Like finishing a season by hand: start the next one if it has already aired
        if (partial.isEmpty()) {
            val next = real.firstOrNull { it.season_number > last }
            if (next != null) {
                tvRepository.fetchEpisodes(showId, next.season_number)
                val firstAir = tvRepository.getEpisodesForSeason(showId, next.season_number)
                    .first()
                    .minByOrNull { it.episode_number }
                    ?.air_date
                val aired = firstAir?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?.let { !it.isAfter(java.time.LocalDate.now()) }
                    ?: false

                if (aired) {
                    val index = result.indexOfFirst { it.number == next.season_number }
                    result[index] = SeasonPlan(next.season_number, WatchStatus.WATCHING)
                }
            }
        }

        return ProgressPlan(seasons = result)
    }

    // The show must already be in the app. Saves the plan from planTraktProgress.
    suspend fun applyTraktProgress(
        showId: Long,
        seasons: List<TvSeasonDto>,
        plan: ProgressPlan,
        watched: List<TraktEp>
    ) {
        if (seasonDao.getSeasonsForShowSync(showId).isEmpty()) insertSeason(showId, seasons)

        val dateOf = watched.associate { (it.s to it.n) to it.d?.let { d -> Instant.parse(d) } }
        val have = watched.groupBy { it.s }.mapValues { (_, list) -> list.map { it.n }.toSet() }
        val now = Instant.now()

        var showFirst: Instant? = null
        var showLatest: Instant? = null

        for (sp in plan.seasons) {
            val n = sp.number
            val watchedHere = have[n].orEmpty()

            if (sp.status == WatchStatus.SKIPPED) {
                setImportedSeasonStatus(showId, n, WatchStatus.SKIPPED, null, null)
                continue
            }
            if (sp.status == WatchStatus.WANT_TO_WATCH) continue

            if (watchedHere.isEmpty()) {
                // The next season, started automatically because it has aired
                setImportedSeasonStatus(showId, n, WatchStatus.WATCHING, now, null)
                continue
            }

            val episodes = tvRepository.getEpisodesForSeason(showId, n).first()
                .sortedBy { it.episode_number }

            if (sp.status == WatchStatus.FINISHED) {
                tvRepository.markAllEpWatched(showId, n, episodes.maxOf { it.episode_number })
            } else {
                episodes.filter { it.episode_number in watchedHere }.forEach {
                    tvRepository.markEpWatched(it.epId, showId, n, it.episode_number)
                }
            }

            var first: Instant? = null
            var latest: Instant? = null
            for (ep in episodes) {
                if (ep.episode_number !in watchedHere) continue
                val d = dateOf[n to ep.episode_number]
                tvRepository.updateEpisodeWatchedDate(ep.epId, d?.toEpochMilli() ?: 0L)
                first = earlier(first, d)
                latest = later(latest, d)
            }

            tvRepository.updateLastEpWatched(
                showId,
                n,
                episodes.filter { it.episode_number in watchedHere }.maxOf { it.episode_number }
            )

            setImportedSeasonStatus(
                showId,
                n,
                sp.status,
                first,
                if (sp.status == WatchStatus.FINISHED) latest else null
            )
            showFirst = earlier(showFirst, first)
            showLatest = later(showLatest, latest)
        }

        if (plan.seasons.any { it.status == WatchStatus.WATCHING }) {
            setImportedShowStatus(showId, WatchStatus.WATCHING, showFirst ?: now, null)
        } else {
            setImportedShowStatus(showId, WatchStatus.FINISHED, showFirst, showLatest)
        }
    }

    fun getAllSeasons() = seasonDao.getAllSeasons()

    fun getAllEpisodes() = tvEpisodeDao.observeAllEpisodes()

    suspend fun markStarted(id: Long) {
        dao.updateStatus(id = id, status = WatchStatus.WATCHING, started = Instant.now())
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun markFinished(id: Long) {
        dao.updateStatus(id = id, status = WatchStatus.FINISHED, finished = Instant.now())
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun markInterrupted(id: Long) {
        dao.updateStatus(id = id, status = WatchStatus.INTERRUPTED, interruptedAt = Instant.now())
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun markWantToWatch(id: Long) {
        dao.updateStatus(id = id, status = WatchStatus.WANT_TO_WATCH)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun resetSeason(showId: Long, seasonNumber: Int) {
        // Untick all episodes and clear saved progress
        tvRepository.markAllEpUnWatched(showId = showId, seasonNumber = seasonNumber)

        // Put the season (and therefore the show) back into Watching
        markSeasonStarted(showId, seasonNumber)
    }

    suspend fun deleteItem(id: Long) {
        dao.insertDeletion(DeletedItemEntity(id = id, deletedAt = System.currentTimeMillis()))
        tvEpisodeDao.deleteEpisodesForShow(id)
        dao.deleteById(id)
        movieRepository.deleteCachedMovie(id)
        tvRepository.deleteCachedTv(id)
    }

    suspend fun deleteFinishedItems() {
        dao.recordFinishedDeletions(System.currentTimeMillis())
        dao.deleteFinished()
    }

    suspend fun deleteSeason(
        showId: Long,
        seasonNumber: Int
    ) {
        tvEpisodeDao.deleteEpisodesForSeason(showId = showId, seasonNumber = seasonNumber)
        seasonDao.deleteSeason(showId = showId, seasonNumber = seasonNumber)

        val remainingSeasons = seasonDao.getSeasonsForShowSync(showId)
        if (remainingSeasons.isEmpty()) {
            dao.insertDeletion(DeletedItemEntity(id = showId, deletedAt = System.currentTimeMillis()))
            dao.deleteById(showId)
            tvRepository.deleteCachedTv(showId)
        } else {
            updateShowStatusFromSeasons(showId)
        }
    }

    suspend fun updateFinishedDate(id: Long, finished: Instant?) {
        dao.updateFinishedDate(id, finished)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun itemExists(id: Long): Boolean = dao.exists(id)

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun updatePinned(id: Long, isPinned: Boolean) {
        dao.updatePinned(id, isPinned)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun updateUserRating(id: Long, rating: Double) {
        dao.updateUserRating(id, rating)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun clearUserRating(id: Long) {
        dao.clearUserRating(id)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun updateNote(id: Long, note: String) {
        dao.setUserNote(id, note)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun updateBackdropOverride(id: Long, backdropOverride: String?) {
        dao.updateBackdropOverride(id, backdropOverride)
        dao.touch(id, System.currentTimeMillis())
    }

    fun getItemById(id: Long): Flow<WatchlistItemEntity?> {
        return dao.getById(id)
    }

    fun getSeasonsForShow(id: Long): Flow<List<SeasonEntity>> {
        return seasonDao.getSeasonForShow(id)
    }

    private suspend fun updateShowStatusFromSeasons(showId: Long) {
        updateShowStatus(showId)
    }

    private suspend fun updateShowStatus(showId: Long) {
        var seasons = seasonDao.getSeasonsForShowSync(showId)
            .sortedBy { it.seasonNumber }

        if (seasons.isEmpty()) return

        val activeSeasons = seasons.filter {
            it.status == WatchStatus.WATCHING || it.status == WatchStatus.INTERRUPTED
        }

        if (activeSeasons.size > 1) {
            val keepActive = activeSeasons.maxByOrNull { season ->
                listOfNotNull(season.seasonStartedDate, season.seasonInterruptedAt).maxOrNull() ?: Instant.MIN
            }

            activeSeasons
                .filter { it != keepActive }
                .forEach { season ->
                    seasonDao.updateSeasonStatus(
                        showId = season.showId,
                        seasonNumber = season.seasonNumber,
                        status = WatchStatus.WANT_TO_WATCH
                    )
                    seasonDao.touchSeason(season.showId, season.seasonNumber, System.currentTimeMillis())
                }

            seasons = seasonDao.getSeasonsForShowSync(showId).sortedBy { it.seasonNumber }
        }

        if (seasons.any { it.status == WatchStatus.WATCHING || it.status == WatchStatus.INTERRUPTED }) {
            dao.updateStatus(id = showId, status = WatchStatus.WATCHING)
            dao.touch(showId, System.currentTimeMillis())
            return
        }

        val hasAiredPendingSeason = seasons
            .filter { it.status == WatchStatus.WANT_TO_WATCH }
            .any { season -> hasFirstEpisodeAired(showId, season) }

        if (hasAiredPendingSeason) {
            dao.updateStatus(id = showId, status = WatchStatus.WATCHING)
            dao.touch(showId, System.currentTimeMillis())
            return
        }

        if (seasons.all { it.status == WatchStatus.FINISHED || it.status == WatchStatus.SKIPPED }) {
            dao.updateStatus(
                id = showId,
                status = if (seasons.all { it.status == WatchStatus.SKIPPED }) WatchStatus.SKIPPED else WatchStatus.FINISHED,
                finished = Instant.now()
            )
            dao.touch(showId, System.currentTimeMillis())
            return
        }

        dao.updateStatus(id = showId, status = WatchStatus.FINISHED, finished = Instant.now())
        dao.touch(showId, System.currentTimeMillis())
    }

    private suspend fun hasFirstEpisodeAired(
        showId: Long,
        season: SeasonEntity
    ): Boolean {
        try {
            tvRepository.ensureEpisodesFetched(tvId = showId, seasonNumber = season.seasonNumber)
        } catch (_: Exception) {
        }

        val firstEpisodeAirDate = tvEpisodeDao.getFirstEpisodeAirDate(showId = showId, seasonNumber = season.seasonNumber)
        val airDate = firstEpisodeAirDate?.takeIf { it.isNotBlank() } ?: return false

        return runCatching {
            !java.time.LocalDate.parse(airDate).isAfter(java.time.LocalDate.now())
        }.getOrDefault(false)
    }

    suspend fun refreshShowStatus(showId: Long) {
        updateShowStatus(showId)
    }

    suspend fun markSeasonStarted(showId: Long, seasonNumber: Int): Boolean {
        val seasons = seasonDao.getSeasonsForShowSync(showId)
        val now = System.currentTimeMillis()

        seasons.filter {
            it.seasonNumber != seasonNumber &&
                (it.status == WatchStatus.WATCHING || it.status == WatchStatus.INTERRUPTED)
        }.forEach { otherSeason ->
            seasonDao.updateSeasonStatus(
                showId = showId,
                seasonNumber = otherSeason.seasonNumber,
                status = WatchStatus.WANT_TO_WATCH
            )
            seasonDao.touchSeason(showId, otherSeason.seasonNumber, now)
        }

        seasonDao.updateSeasonStatus(
            showId = showId,
            seasonNumber = seasonNumber,
            status = WatchStatus.WATCHING,
            started = Instant.now()
        )
        seasonDao.touchSeason(showId, seasonNumber, now)

        updateShowStatusFromSeasons(showId)
        return true
    }

    suspend fun markSeasonFinished(showId: Long, seasonNumber: Int, lastEpNumber: Int) {
        val now = System.currentTimeMillis()

        seasonDao.updateSeasonStatus(
            showId = showId,
            seasonNumber = seasonNumber,
            status = WatchStatus.FINISHED,
            finished = Instant.now()
        )
        seasonDao.touchSeason(showId, seasonNumber, now)

        tvRepository.markAllEpWatched(showId = showId, seasonNumber = seasonNumber, lastEpNumber = lastEpNumber)

        val allSeasons = seasonDao.getSeasonsForShowSync(showId).sortedBy { it.seasonNumber }
        val nextSeason = allSeasons.firstOrNull {
            it.seasonNumber > seasonNumber && it.status == WatchStatus.WANT_TO_WATCH
        }

        if (nextSeason != null && hasFirstEpisodeAired(showId, nextSeason)) {
            seasonDao.updateSeasonStatus(
                showId = showId,
                seasonNumber = nextSeason.seasonNumber,
                status = WatchStatus.WATCHING,
                started = Instant.now()
            )
            seasonDao.touchSeason(showId, nextSeason.seasonNumber, now)
        }

        updateShowStatusFromSeasons(showId)
    }

    suspend fun markSeasonInterrupted(showId: Long, seasonNumber: Int) {
        seasonDao.updateSeasonStatus(
            showId = showId,
            seasonNumber = seasonNumber,
            status = WatchStatus.INTERRUPTED,
            interruptedAt = Instant.now()
        )
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
        updateShowStatusFromSeasons(showId)
    }

    suspend fun markSeasonWantToWatch(showId: Long, seasonNumber: Int) {
        seasonDao.updateSeasonStatus(showId = showId, seasonNumber = seasonNumber, status = WatchStatus.WANT_TO_WATCH)
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
        tvRepository.markAllEpUnWatched(showId = showId, seasonNumber = seasonNumber)
        updateShowStatusFromSeasons(showId)
    }

    suspend fun moveSeasonBackToWatchlist(showId: Long, seasonNumber: Int) {
        val now = System.currentTimeMillis()
        seasonDao.updateSeasonStatus(showId = showId, seasonNumber = seasonNumber, status = WatchStatus.WANT_TO_WATCH)
        seasonDao.touchSeason(showId, seasonNumber, now)
        tvRepository.markAllEpUnWatched(showId = showId, seasonNumber = seasonNumber)
        dao.updateStatus(id = showId, status = WatchStatus.WANT_TO_WATCH)
        dao.touch(showId, now)
    }

    suspend fun markSeasonSkipped(showId: Long, seasonNumber: Int) {
        seasonDao.updateSeasonStatus(showId = showId, seasonNumber = seasonNumber, status = WatchStatus.SKIPPED)
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
        updateShowStatusFromSeasons(showId)
    }

    suspend fun markDropped(id: Long) {
        dao.updateStatus(id = id, status = WatchStatus.FINISHED, finished = Instant.now())
        dao.setDropped(id, true)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun markSeasonDropped(showId: Long, seasonNumber: Int) {
        val now = System.currentTimeMillis()
        seasonDao.updateSeasonStatus(showId = showId, seasonNumber = seasonNumber, status = WatchStatus.SKIPPED)
        seasonDao.touchSeason(showId, seasonNumber, now)
        dao.updateStatus(id = showId, status = WatchStatus.FINISHED, finished = Instant.now())
        dao.setDropped(showId, true)
        dao.touch(showId, now)
    }

    suspend fun unDrop(id: Long) {
        dao.updateStatus(id = id, status = WatchStatus.WANT_TO_WATCH)
        dao.setDropped(id, false)
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun unDropSeason(showId: Long, seasonNumber: Int) {
        dao.updateStatus(id = showId, status = WatchStatus.WANT_TO_WATCH)
        dao.setDropped(showId, false)
        dao.touch(showId, System.currentTimeMillis())
    }

    suspend fun updateSeasonUserRating(showId: Long, seasonNumber: Int, rating: Double) {
        seasonDao.updateSeasonUserRating(showId = showId, seasonNumber = seasonNumber, rating = rating)
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
    }

    suspend fun clearSeasonUserRating(showId: Long, seasonNumber: Int) {
        seasonDao.clearSeasonUserRating(showId = showId, seasonNumber = seasonNumber)
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
    }

    suspend fun updateSeasonNote(showId: Long, seasonNumber: Int, note: String) {
        seasonDao.setSeasonUserNote(showId = showId, seasonNumber = seasonNumber, note = note)
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
    }

    suspend fun updateSeasonFinishedDate(showId: Long, seasonNumber: Int, finished: Instant?) {
        seasonDao.updateSeasonFinishedDate(showId = showId, seasonNumber = seasonNumber, finished = finished)
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
    }

    suspend fun updateFinishedTvSeasonDate(
        showId: Long,
        seasonNumber: Int,
        finished: Instant?,
        releaseDates: Boolean = false,
        clearEpisodeDates: Boolean = false
    ) {
        seasonDao.updateSeasonFinishedDate(
            showId = showId,
            seasonNumber = seasonNumber,
            finished = finished
        )
        seasonDao.touchSeason(showId, seasonNumber, System.currentTimeMillis())
        dao.updateFinishedDate(showId, finished)

        if (releaseDates || clearEpisodeDates) {
            val episodes = tvEpisodeDao.getEpisodesForSeasonOnce(showId, seasonNumber)
            episodes.forEach { episode ->
                val timestamp = if (releaseDates) {
                    episode.air_date?.let { date ->
                        runCatching {
                            java.time.LocalDate.parse(date)
                                .atStartOfDay(java.time.ZoneOffset.UTC)
                                .toInstant()
                                .toEpochMilli()
                        }.getOrDefault(0L)
                    } ?: 0L
                } else {
                    0L
                }
                tvEpisodeDao.updateEpisodeWatchedDateAndStatus(
                    epId = episode.epId,
                    watched = true,
                    timestamp = timestamp
                )
            }
        }
    }
}
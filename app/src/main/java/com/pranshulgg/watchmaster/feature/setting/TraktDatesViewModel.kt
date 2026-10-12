package com.pranshulgg.watchmaster.feature.setting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pranshulgg.watchmaster.data.repository.DateImportSummary
import com.pranshulgg.watchmaster.data.repository.SearchRepository
import com.pranshulgg.watchmaster.data.repository.TraktDateEntry
import com.pranshulgg.watchmaster.data.repository.WatchlistRepository
import com.pranshulgg.watchmaster.feature.search.SearchType
import com.pranshulgg.watchmaster.data.repository.TvRepository
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pranshulgg.watchmaster.data.repository.TraktEp
import com.pranshulgg.watchmaster.data.repository.realDate
import com.pranshulgg.watchmaster.core.model.WatchStatus
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TraktDatesViewModel @Inject constructor(
    private val repo: WatchlistRepository,
    private val searchRepo: SearchRepository,
    private val tvRepo: TvRepository
) : ViewModel() {

    var status by mutableStateOf<String?>(null)
        private set

    var running by mutableStateOf(false)
        private set

    fun clearStatus() {
        status = null
    }

    private fun parse(json: String): List<TraktDateEntry> =
        Gson().fromJson(json, Array<TraktDateEntry>::class.java).toList()

    // Fills in missing dates on items already in the app
    suspend fun run(json: String, dryRun: Boolean): DateImportSummary =
        repo.applyWatchedDates(parse(json), dryRun)

    fun importMissingWatched(json: String) {
        if (running) return
        running = true

        viewModelScope.launch {
            try {
                val moviesResult = addMissingMovies(json)
                val showsResult = addMissingShows(json)
                val progressResult = addInProgressShows(json)
                status = moviesResult + "\n\n" + showsResult + "\n\n" + progressResult
            } finally {
                running = false
            }
        }
    }

    private suspend fun addMissingMovies(json: String): String {
        val movies = parse(json)
            .filter { it.t == "movie" }
            .filter { !repo.itemExists(it.id) }

        var added = 0
        val failed = mutableListOf<String>()

        movies.forEachIndexed { index, entry ->
            status = "Adding movies... ${index + 1} / ${movies.size}"
            val title = entry.n ?: entry.id.toString()
            try {
                val match = searchRepo
                    .search(title, SearchType.MOVIE)
                    .firstOrNull { it.id == entry.id }

                if (match == null) {
                    failed += title
                } else {
                    repo.addFromSearch(match)
                    repo.markFinished(entry.id)
                    repo.updateFinishedDate(entry.id, entry.realDate())
                    added++
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failed += title
            }
        }

        return buildString {
            append("Added $added movies.")
            if (failed.isNotEmpty()) {
                append("\nCouldn't add ${failed.size}, so add these by hand:\n")
                append(failed.take(10).joinToString("\n"))
                if (failed.size > 10) append("\n...and ${failed.size - 10} more")
            }
        }
    }

    private suspend fun addMissingShows(json: String): String {
        val shows = parse(json)
            .filter { it.t == "show" }
            .filter { !repo.itemExists(it.id) }

        var added = 0
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<String>()

        shows.forEachIndexed { index, entry ->
            val title = entry.n ?: entry.id.toString()
            status = "Adding shows... ${index + 1} / ${shows.size}\n$title"

            try {
                val watched = entry.eps.orEmpty()
                val watchedNumbers = watched
                    .groupBy { it.s }
                    .mapValues { (_, list) -> list.map { it.n }.toSet() }
                val dateOf = watched.associate {
                    (it.s to it.n) to it.d?.let { d -> Instant.parse(d) }
                }

                // 1. Cheap check using TMDB's season list
                val seasons = searchRepo.getSeasonData(entry.id)
                val real = seasons.filter {
                    it.season_number != 0 && it.episode_count > 0
                }
                val looksComplete = real.isNotEmpty() && real.all { season ->
                    val have = watchedNumbers[season.season_number] ?: emptySet()
                    (1..season.episode_count).all { it in have }
                }
                if (!looksComplete) {
                    skipped += title
                    return@forEachIndexed
                }

                // 2. Find the show in search, the same way adding by hand does
                val match = searchRepo
                    .search(title, SearchType.TV)
                    .firstOrNull { it.id == entry.id }
                if (match == null) {
                    failed += title
                    return@forEachIndexed
                }

                // 3. Fetch every season's episodes BEFORE saving the show
                val episodesBySeason = real.associate { season ->
                    tvRepo.fetchEpisodes(entry.id, season.season_number)
                    season.season_number to
                        tvRepo.getEpisodesForSeason(entry.id, season.season_number).first()
                }

                val verified = episodesBySeason.all { (sn, episodes) ->
                    val have = watchedNumbers[sn] ?: emptySet()
                    episodes.isNotEmpty() && episodes.all { it.episode_number in have }
                }
                if (!verified) {
                    skipped += title
                    return@forEachIndexed
                }

                // 4. Add the show and mark seasons finished
                repo.addFromSearch(match, seasons)

                var showLatest: Instant? = null
                episodesBySeason.forEach { (sn, episodes) ->
                    tvRepo.markAllEpWatched(
                        showId = entry.id,
                        seasonNumber = sn,
                        lastEpNumber = episodes.maxOf { it.episode_number }
                    )

                    var seasonLatest: Instant? = null
                    episodes.forEach { ep ->
                        val date = dateOf[sn to ep.episode_number]
                        tvRepo.updateEpisodeWatchedDate(
                            ep.epId,
                            date?.toEpochMilli() ?: 0L
                        )
                        if (date != null && (seasonLatest == null || date.isAfter(seasonLatest))) {
                            seasonLatest = date
                        }
                    }

                    repo.updateSeasonFinishedDate(entry.id, sn, seasonLatest)
                    if (seasonLatest != null &&
                        (showLatest == null || seasonLatest!!.isAfter(showLatest))
                    ) {
                        showLatest = seasonLatest
                    }
                }

                repo.markFinished(entry.id)
                repo.updateFinishedDate(entry.id, showLatest)
                added++
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failed += title
            }
        }

        return buildString {
            append("Added $added shows.")
            if (skipped.isNotEmpty()) {
                append("\n${skipped.size} partly watched shows are handled in the next step.")
            }
            if (failed.isNotEmpty()) {
                append("\nCouldn't add ${failed.size}, so add these by hand:\n")
                append(failed.take(10).joinToString("\n"))
                if (failed.size > 10) append("\n...and ${failed.size - 10} more")
            }
        }
    }

    // Returns (total movies in file, movies not yet in the app)
    suspend fun countMissingMovies(json: String): Pair<Int, Int> {
        val movies = parse(json).filter { it.t == "movie" }
        return movies.size to movies.count { !repo.itemExists(it.id) }
    }

    // Returns (shows in file, shows not yet in the app)
    suspend fun countMissingShows(json: String): Pair<Int, Int> {
        val shows = parse(json).filter { it.t == "show" }
        return shows.size to shows.count { !repo.itemExists(it.id) }
    }

    // Returns (watchlist items in file, items not yet in the app)
    suspend fun countMissingWatchlist(json: String): Pair<Int, Int> {
        val items = parse(json).filter { it.t == "wl_movie" || it.t == "wl_show" }
        return items.size to items.count { !repo.itemExists(it.id) }
    }

    fun importWatchlist(json: String) {
        if (running) return
        running = true

        viewModelScope.launch {
            try {
                val items = parse(json)
                    .filter { it.t == "wl_movie" || it.t == "wl_show" }
                    .filter { !repo.itemExists(it.id) }

                var added = 0
                val noSeasons = mutableListOf<String>()
                val failed = mutableListOf<String>()

                items.forEachIndexed { index, entry ->
                    val title = entry.n ?: entry.id.toString()
                    status = "Adding to watchlist... ${index + 1} / ${items.size}\n$title"

                    try {
                        val isShow = entry.t == "wl_show"

                        val match = searchRepo
                            .search(title, if (isShow) SearchType.TV else SearchType.MOVIE)
                            .firstOrNull { it.id == entry.id }

                        if (match == null) {
                            failed += title
                            return@forEachIndexed
                        }

                        if (isShow) {
                            val seasons = searchRepo.getSeasonData(entry.id)
                            if (seasons.isEmpty()) {
                                noSeasons += title
                                return@forEachIndexed
                            }
                            repo.addFromSearch(match, seasons)

                            // Download episodes for upcoming seasons so they show up
                            // in Upcoming Seasons straight away
                            val today = java.time.LocalDate.now()
                            seasons
                                .filter { it.season_number != 0 }
                                .filter { season ->
                                    val date = season.air_date
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                                    date == null || !date.isBefore(today)
                                }
                                .forEach { season ->
                                    try {
                                        tvRepo.ensureEpisodesFetched(entry.id, season.season_number)
                                    } catch (e: Exception) {
                                        if (e is CancellationException) throw e
                                    }
                                }
                        } else {
                            repo.addFromSearch(match)
                        }
                        added++
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        failed += title
                    }
                }

                status = buildString {
                    append("Added $added to your watchlist.")
                    if (noSeasons.isNotEmpty()) {
                        append("\n\nNo season info on TMDB yet, so skipped:\n")
                        append(noSeasons.joinToString("\n"))
                    }
                    if (failed.isNotEmpty()) {
                        append("\n\nCouldn't find ${failed.size}, so add these by hand:\n")
                        append(failed.take(10).joinToString("\n"))
                        if (failed.size > 10) append("\n...and ${failed.size - 10} more")
                    }
                }
            } finally {
                running = false
            }
        }
    }
    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.int(key: String): Int? =
        get(key)?.takeIf { !it.isJsonNull }?.asInt

    private fun JsonObject.tmdbOf(key: String): Long? =
        getAsJsonObject(key)?.getAsJsonObject("ids")?.get("tmdb")
            ?.takeIf { !it.isJsonNull }?.asLong

    private fun goodDate(d: String?) = d != null && (d.take(4).toIntOrNull() ?: 0) >= 1990

    // Turns the raw Trakt files into the one combined JSON the importers read
    fun combine(texts: List<String>): String {
        val epoch = "1970-01-01T00:00:00.000Z"
        val gson = Gson()

        val movieDates = mutableMapOf<Long, String>()
        val movieTitles = mutableMapOf<Long, String>()
        val showTitles = mutableMapOf<Long, String>()
        val episodes = mutableMapOf<Long, MutableMap<Pair<Int, Int>, String?>>()
        val watchlist = mutableListOf<TraktDateEntry>()
        val passthrough = mutableListOf<JsonElement>()
        val seenHistory = mutableSetOf<Long>()
        var recognised = false

        texts.forEach files@{ text ->
            val root = try {
                JsonParser.parseString(text)
            } catch (e: Exception) {
                return@files
            }
            if (!root.isJsonArray || root.asJsonArray.size() == 0) return@files

            val list = root.asJsonArray
            val first = list[0] as? JsonObject ?: return@files
            val objects = list.mapNotNull { it as? JsonObject }

            when {
                // Already a combined file
                first.has("t") -> {
                    recognised = true
                    list.forEach { passthrough += it }
                }

                // watched-history-N.json
                first.has("watched_at") && first.has("type") -> {
                    recognised = true
                    objects.forEach { obj ->
                        val hid = obj.get("id")?.takeIf { !it.isJsonNull }?.asLong
                        if (hid != null && !seenHistory.add(hid)) return@forEach

                        val watchedAt = obj.str("watched_at")
                        when (obj.str("type")) {
                            "movie" -> {
                                val id = obj.tmdbOf("movie") ?: return@forEach
                                obj.getAsJsonObject("movie").str("title")
                                    ?.let { movieTitles.putIfAbsent(id, it) }
                                if (goodDate(watchedAt) && watchedAt!! > (movieDates[id] ?: "")) {
                                    movieDates[id] = watchedAt
                                }
                            }

                            "episode" -> {
                                val id = obj.tmdbOf("show") ?: return@forEach
                                val ep = obj.getAsJsonObject("episode") ?: return@forEach
                                val season = ep.int("season") ?: return@forEach
                                val number = ep.int("number") ?: return@forEach
                                if (season == 0) return@forEach

                                obj.getAsJsonObject("show").str("title")
                                    ?.let { showTitles.putIfAbsent(id, it) }

                                val d = if (goodDate(watchedAt)) watchedAt else null
                                val map = episodes.getOrPut(id) { mutableMapOf() }
                                val key = season to number
                                if (!map.containsKey(key)) {
                                    map[key] = d
                                } else if (d != null) {
                                    val current = map[key]
                                    if (current == null || d > current) map[key] = d
                                }
                            }
                        }
                    }
                }

                // lists-watchlist.json
                first.has("listed_at") -> {
                    recognised = true
                    objects.forEach { obj ->
                        val listed = obj.str("listed_at") ?: return@forEach
                        when (obj.str("type")) {
                            "movie" -> {
                                val id = obj.tmdbOf("movie") ?: return@forEach
                                val title = obj.getAsJsonObject("movie").str("title") ?: id.toString()
                                watchlist += TraktDateEntry(t = "wl_movie", id = id, n = title, d = listed)
                            }

                            "show" -> {
                                val id = obj.tmdbOf("show") ?: return@forEach
                                val title = obj.getAsJsonObject("show").str("title") ?: id.toString()
                                watchlist += TraktDateEntry(t = "wl_show", id = id, n = title, d = listed)
                            }
                        }
                    }
                }

                // watched-movies.json
                first.has("movie") && first.has("last_watched_at") -> {
                    recognised = true
                    objects.forEach { obj ->
                        val id = obj.tmdbOf("movie") ?: return@forEach
                        obj.getAsJsonObject("movie").str("title")?.let { movieTitles[id] = it }
                        val d = obj.str("last_watched_at")
                        if (goodDate(d) && d!! > (movieDates[id] ?: "")) movieDates[id] = d
                    }
                }

                // watched-shows.json (only used for titles)
                first.has("show") && first.has("last_watched_at") -> {
                    recognised = true
                    objects.forEach { obj ->
                        val id = obj.tmdbOf("show") ?: return@forEach
                        obj.getAsJsonObject("show").str("title")?.let { showTitles[id] = it }
                    }
                }
            }
        }

        if (!recognised) throw IllegalStateException("No Trakt data found")

        val out = JsonArray()
        passthrough.forEach { out.add(it) }

        movieTitles.forEach { (id, title) ->
            out.add(
                gson.toJsonTree(
                    TraktDateEntry(t = "movie", id = id, n = title, d = movieDates[id] ?: epoch)
                )
            )
        }

        episodes.forEach { (id, map) ->
            val title = showTitles[id] ?: id.toString()

            // Latest real date per season
            val bySeason = mutableMapOf<Int, String>()
            map.forEach { (key, d) ->
                if (d != null && d > (bySeason[key.first] ?: "")) bySeason[key.first] = d
            }
            bySeason.forEach { (season, d) ->
                out.add(gson.toJsonTree(TraktDateEntry(t = "tv", id = id, s = season, n = title, d = d)))
            }

            val latest = map.values.filterNotNull().maxOrNull() ?: epoch
            val eps = map.entries
                .sortedWith(compareBy({ it.key.first }, { it.key.second }))
                .map { TraktEp(s = it.key.first, n = it.key.second, d = it.value) }
            out.add(gson.toJsonTree(TraktDateEntry(t = "show", id = id, n = title, d = latest, eps = eps)))
        }

        watchlist.sortedBy { it.d }.forEach { out.add(gson.toJsonTree(it)) }

        return out.toString()
    }

    // Returns (shows in file, shows that can be imported)
    suspend fun countProgressCandidates(json: String): Pair<Int, Int> {
        val shows = parse(json).filter { it.t == "show" }
        val usable = shows.count {
            !repo.itemExists(it.id) || repo.isUntouchedWatchlistShow(it.id)
        }
        return shows.size to usable
    }

    private suspend fun addInProgressShows(json: String): String {
        val shows = parse(json)
            .filter { it.t == "show" }
            .filter { !repo.itemExists(it.id) || repo.isUntouchedWatchlistShow(it.id) }

        var finished = 0
        var watching = 0
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<String>()

        shows.forEachIndexed { index, entry ->
            val title = entry.n ?: entry.id.toString()
            status = "Importing shows in progress... ${index + 1} / ${shows.size}\n$title"

            try {
                val watched = entry.eps.orEmpty()
                val seasons = searchRepo.getSeasonData(entry.id)

                // Work everything out BEFORE saving anything
                val plan = repo.planTraktProgress(entry.id, seasons, watched)
                if (plan.skipReason != null) {
                    skipped += "$title (${plan.skipReason})"
                    return@forEachIndexed
                }

                if (!repo.itemExists(entry.id)) {
                    val match = searchRepo
                        .search(title, SearchType.TV)
                        .firstOrNull { it.id == entry.id }
                    if (match == null) {
                        failed += title
                        return@forEachIndexed
                    }
                    repo.addFromSearch(match, seasons)
                }

                repo.applyTraktProgress(entry.id, seasons, plan, watched)

                if (plan.seasons.any { it.status == WatchStatus.WATCHING }) {
                    watching++
                } else {
                    finished++
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failed += title
            }
        }

        return buildString {
            append("Shows in progress: $watching Watching, $finished Finished.")
            if (skipped.isNotEmpty()) {
                append("\nSkipped ${skipped.size}:\n")
                append(skipped.take(15).joinToString("\n"))
            }
            if (failed.isNotEmpty()) {
                append("\nCouldn't add ${failed.size}, so add these by hand:\n")
                append(failed.take(10).joinToString("\n"))
                if (failed.size > 10) append("\n...and ${failed.size - 10} more")
            }
        }
    }
}
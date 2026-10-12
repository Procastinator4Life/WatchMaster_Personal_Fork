package com.pranshulgg.watchmaster.feature.tv.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.EmptyContainerPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.LoadingScreenPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.media.PosterBox
import com.pranshulgg.watchmaster.core.ui.components.media.PosterPlaceholder
import com.pranshulgg.watchmaster.core.ui.navigation.NavRoutes
import com.pranshulgg.watchmaster.core.ui.navigation.navigateSafely
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import com.pranshulgg.watchmaster.core.utils.formatDate
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.pranshulgg.watchmaster.feature.shared.media.components.YearHeader
import com.pranshulgg.watchmaster.feature.shared.media.components.YearScrollbar
import com.pranshulgg.watchmaster.feature.shared.media.components.groupByYear

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TvWatchedGrid(
    isLoading: Boolean,
    items: List<WatchlistItemEntity>,
    seasons: List<SeasonEntity>,
    tvEpisodes: List<TvEpisodeEntity>,
    scrollBehavior: FloatingToolbarScrollBehavior,
    scrollBehaviorTopBar: TopAppBarScrollBehavior,
    navController: NavController,
    onLongActionTvSeasonRequest: (SeasonEntity?, WatchlistItemEntity) -> Unit,
    header: @Composable () -> Unit = {},
) {
    if (isLoading) LoadingScreenPlaceholder()
    if (items.isEmpty()) {
        EmptyContainerPlaceholder(
            R.drawable.movie_info_24px,
            "No completed series",
            description = "You haven’t completed anything yet. Time to start and finish a series!"
        )
        return
    }

    val seasonsByShow = seasons.groupBy { it.showId }

    // Same date the card shows: watched date of the last episode in the latest finished season
    val groups = remember(items, seasons, tvEpisodes) {
        val dated = items.map { item ->
            val latestWatched = seasonsByShow[item.id].orEmpty()
                .filter { it.status == WatchStatus.FINISHED }
                .maxByOrNull { it.seasonNumber }
            val date = latestWatched?.let { season ->
                tvEpisodes
                    .asSequence()
                    .filter {
                        it.showId == item.id &&
                            it.seasonNumber == season.seasonNumber &&
                            it.isWatched
                    }
                    .maxByOrNull { it.episode_number }
            }?.lastWatchedAt
                ?.takeIf { it > 0L }
                ?.let(java.time.Instant::ofEpochMilli)
            item to date
        }

        groupByYear(
            dated.sortedWith(
                compareByDescending<Pair<WatchlistItemEntity, java.time.Instant?>> {
                    it.second ?: java.time.Instant.EPOCH
                }.thenBy { it.first.title.lowercase() }
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val showStrip = groups.size > 1
        val stripWidth = 0.dp

        // Poster minimum width. Smaller number = more columns.
        val columns = ((maxWidth - stripWidth) / 150.dp).toInt().coerceAtLeast(3)

        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        val headerIndexes = remember(groups, columns) {
            var index = 1
            groups.map { group ->
                val start = index
                index += 1 + (group.entries.size + columns - 1) / columns
                start
            }
        }

        val currentGroup by remember(headerIndexes) {
            derivedStateOf {
                headerIndexes
                    .indexOfLast { it <= listState.firstVisibleItemIndex }
                    .coerceAtLeast(0)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior)
                .nestedScroll(scrollBehaviorTopBar.nestedScrollConnection)
                .padding(start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { header() }

            groups.forEach { group ->
                item(key = "year_${group.label}") { YearHeader(group.label) }

                items(group.entries.chunked(columns)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { (item, displayedFinishedDate) ->
                            val showSeasons = seasonsByShow[item.id].orEmpty().sortedBy { it.seasonNumber }
                            val latestWatched = showSeasons
                                .filter { it.status == WatchStatus.FINISHED }
                                .maxByOrNull { it.seasonNumber }
                            val ratingSeason = latestWatched ?: showSeasons.firstOrNull { it.status == WatchStatus.SKIPPED }
                            val targetSeason = latestWatched ?: showSeasons.firstOrNull { it.seasonNumber == 1 } ?: showSeasons.firstOrNull()

                            WatchedTvCard(
                                modifier = Modifier.weight(1f),
                                item = item,
                                rating = ratingSeason?.seasonUserRating,
                                finishedDate = displayedFinishedDate,
                                onClick = {
                                    targetSeason?.let {
                                        navController.navigateSafely(NavRoutes.tvDetail(it.showId, it.seasonNumber))
                                    }
                                },
                                onLongClick = { onLongActionTvSeasonRequest(targetSeason, item) }
                            )
                        }
                        repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            item {
                Spacer(Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + ScreenOffset + 30.dp))
            }
        }

        if (showStrip) {
            YearScrollbar(
                listState = listState,
                labels = groups.map { it.label },
                headerIndexes = headerIndexes,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, bottom = 80.dp)
            )
        }
    }
}

@Composable
private fun WatchedTvCard(
    modifier: Modifier = Modifier,
    item: WatchlistItemEntity,
    rating: Double?,
    finishedDate: java.time.Instant?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val posterHeight = maxWidth * 1.52f
            Box(modifier = Modifier.fillMaxWidth().height(posterHeight)) {
                PosterBox(
                    posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    apiPath = item.posterPath,
                    fillMaxWidth = true,
                    height = posterHeight,
                    cornerRadius = ShapeRadius.Large,
                    placeholder = { PosterPlaceholder(bgColor = MaterialTheme.colorScheme.surfaceBright) },
                    progressIndicatorSize = 28.dp
                )

                if (rating != null && rating > 0.0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 8.dp, end = 10.dp)
                                .height(28.dp)
                        ) {
                            com.pranshulgg.watchmaster.core.ui.components.Symbol(
                                R.drawable.star_24px,
                                size = 16.dp,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                if (rating == 10.0) "10" else rating.toString(),
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = item.title,
            modifier = Modifier.padding(top = 7.dp, start = 2.dp, end = 2.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        finishedDate?.let { date ->
            Text(
                text = date.formatDate(),
                modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
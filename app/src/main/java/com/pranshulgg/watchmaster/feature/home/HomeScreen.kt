package com.pranshulgg.watchmaster.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.core.ui.components.Gap
import com.pranshulgg.watchmaster.core.ui.components.media.PosterBox
import com.pranshulgg.watchmaster.core.ui.components.media.PosterPlaceholder
import com.pranshulgg.watchmaster.core.ui.navigation.NavRoutes
import com.pranshulgg.watchmaster.core.ui.navigation.navigateSafely
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    topPadding: Dp = 0.dp,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    scrollBehaviorTopBar: TopAppBarScrollBehavior? = null,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    // Collect Flow state continuously
    val watchNextList by viewModel.watchNextItems.collectAsState()
    val calendarItems by viewModel.upcomingEpisodeCalendar.collectAsState()
    val upcomingMovies by viewModel.upcomingMovies.collectAsState()
    val upcomingShows by viewModel.upcomingShows.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateRightPadding(LayoutDirection.Ltr)
                )
                .fillMaxSize()
                .then(
                    if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior) else Modifier
                )
                .then(
                    if (scrollBehaviorTopBar != null) {
                        Modifier.nestedScroll(scrollBehaviorTopBar.nestedScrollConnection)
                    } else Modifier
                ),
            contentPadding = PaddingValues(
                top = topPadding,
                bottom = innerPadding.calculateBottomPadding() + 96.dp
            )
        ) {
            item(key = "watch_next") {
                WatchNextSection(
                    items = watchNextList,
                    onItemClick = { item ->
                        navController.navigateSafely(
                            "${NavRoutes.TV_DETAIL_SCREEN}/${item.showId}/${item.seasonNumber}?episodeNumber=${item.episodeNumber}"
                        )
                    }
                )
            }

            // Upcoming Episodes is independent of Watch Next
            item(key = "upcoming_episodes") {
                UpcomingEpisodeCalendar(
                    items = calendarItems,
                    onEpisodeClick = { episode ->
                        navController.navigateSafely(
                            NavRoutes.tvDetail(
                                id = episode.showId,
                                seasonNumber = episode.seasonNumber,
                                episodeNumber = episode.episodeNumber
                            )
                        )
                    }
                )
            }

            // Upcoming Movies Row
            item(key = "upcoming_movies") {
                UpcomingMoviesRow(
                    items = upcomingMovies,
                    onMovieClick = { movie ->
                        navController.navigateSafely("${NavRoutes.MOVIE_DETAIL_SCREEN}/${movie.movieId}")
                    }
                )
            }
            
            // Upcoming Shows Row
            item(key = "upcoming_shows") {
                UpcomingShowsRow(
                    items = upcomingShows,
                    onShowClick = { show ->
                        navController.navigateSafely(
                            NavRoutes.tvDetail(
                                id = show.showId,
                                seasonNumber = show.seasonNumber,
                                episodeNumber = 1
                            )
                        )
                    }
                )
            }

            if (watchNextList.isEmpty() && calendarItems.isEmpty() && upcomingMovies.isEmpty() && upcomingShows.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nothing here yet")
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingEpisodeCalendar(
    items: List<UpcomingEpisodeCalendarItem>,
    onEpisodeClick: (UpcomingEpisodeCalendarItem) -> Unit
) {
    if (items.isEmpty()) return

    val today = LocalDate.now()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE") }

    val (distinctDisplayItems, additionalCountsByShowAndDate) = remember(items) {
        val grouped = items.groupBy { episode ->
            episode.showId to episode.date
        }

        // Calculate additional count (Total minus the main episode shown)
        val additionalCounts = grouped.mapValues { it.value.size - 1 }

        val firstEpisodes = grouped.map { (_, epList) ->
            epList.minByOrNull { it.episodeNumber } ?: epList.first()
        }

        firstEpisodes to additionalCounts
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {

        SectionTitle("Upcoming Episodes")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = distinctDisplayItems,
                key = {
                    "${it.showId}_${it.seasonNumber}_${it.episodeNumber}_${it.date}"
                }
            ) { episode ->

                val additionalCount = additionalCountsByShowAndDate[
                    episode.showId to episode.date
                ] ?: 0

                UpcomingEpisodeCard(
                    episode = episode,
                    today = today,
                    dateFormatter = dateFormatter,
                    additionalCount = additionalCount,
                    onClick = { onEpisodeClick(episode) }
                )
            }
        }
    }
}

@Composable
private fun UpcomingEpisodeCard(
    episode: UpcomingEpisodeCalendarItem,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    additionalCount: Int,
    onClick: () -> Unit
) {
    val imagePath = episode.backdropPath
    val imageUrl = imagePath?.takeIf { it.isNotBlank() }
        ?.let { "https://image.tmdb.org/t/p/w780$it" }

    val dateLabel = when {
        episode.date == today -> "Today"
        episode.date == today.plusDays(1) -> "Tomorrow"
        episode.date.dayOfWeek == today.dayOfWeek && episode.date != today ->
            "Next ${episode.date.format(dateFormatter)}"
        else -> {
            val todayWeek =
                today.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
            val episodeWeek =
                episode.date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
            val todayYear =
                today.get(java.time.temporal.WeekFields.ISO.weekBasedYear())
            val episodeYear =
                episode.date.get(java.time.temporal.WeekFields.ISO.weekBasedYear())

            if (todayWeek == episodeWeek && todayYear == episodeYear) {
                "This ${episode.date.format(dateFormatter)}"
            } else {
                "Next ${episode.date.format(dateFormatter)}"
            }
        }
    }

    val cardWidth = backdropCardWidth()
    val cardHeight = cardWidth * (9f / 16f)

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
        ) {
            PosterBox(
                posterUrl = imageUrl,
                apiPath = imagePath,
                width = cardWidth,
                height = cardHeight,
                fillMaxWidth = true,
                cornerRadius = ShapeRadius.Medium,
                placeholder = {
                    PosterPlaceholder(
                        bgColor = MaterialTheme.colorScheme.surfaceBright
                    )
                },
                progressIndicatorSize = 24.dp
            )

            // New Series / New Season badge
            if (episode.episodeNumber == 1) {
                val badgeText = if (episode.seasonNumber == 1) "New Series" else "New Season"

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Additional episodes badge (+3 when 4 release on the same day)
            if (additionalCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "+$additionalCount",
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Text(
            text = episode.showTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "S${episode.seasonNumber} E${episode.episodeNumber} • $dateLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
fun WatchNextSection(
    items: List<WatchNextItem>,
    onItemClick: (WatchNextItem) -> Unit
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {

        SectionTitle("Watch Next")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.showId }) { item ->
                WatchNextCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
fun WatchNextCard(
    item: WatchNextItem,
    onClick: () -> Unit
) {
    val imagePath = item.displayImagePath
    val fullImageUrl = if (imagePath != null) "https://image.tmdb.org/t/p/w780$imagePath" else ""

    val cardWidth = backdropCardWidth()
    val cardHeight = cardWidth * (9f / 16f)

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onClick() }
    ) {

        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
        ) {
            PosterBox(
                posterUrl = fullImageUrl,
                apiPath = imagePath,
                width = cardWidth,
                height = cardHeight,
                cornerRadius = ShapeRadius.Medium,
                placeholder = {
                    PosterPlaceholder(
                        bgColor = MaterialTheme.colorScheme.surfaceBright
                    )
                },
                progressIndicatorSize = 34.dp
            )

            if (item.isSeasonFinale) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "Season Finale",
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Gap(8.dp)

        Text(
            text = item.showTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Gap(2.dp)

        Text(
            text = "S${item.seasonNumber} E${item.episodeNumber} • ${item.episodeName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun backdropCardWidth(): Dp {
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    return if (isTablet) 380.dp else 260.dp
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 10.dp
        )
    )
}

@Composable
fun posterCardWidth(): Dp {
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    return if (isTablet) 190.dp else 140.dp
}
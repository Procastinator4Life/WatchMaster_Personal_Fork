package com.pranshulgg.watchmaster.feature.tv.detail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.media.MediaChip
import com.pranshulgg.watchmaster.core.ui.components.media.MediaSectionCard
import com.pranshulgg.watchmaster.core.ui.components.media.DatePickerSheet
import java.time.ZoneOffset
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.feature.tv.detail.TvDetailsViewModel
import com.pranshulgg.watchmaster.feature.tv.detail.ui.EpisodeOptionsSheet
import com.pranshulgg.watchmaster.feature.tv.detail.ui.TvDetailsEpisodeInfoSheet
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EpisodesSection(
    episodes: List<TvEpisodeEntity>,
    viewModel: TvDetailsViewModel,
    season: SeasonEntity,
    backdropPath: String?,
    targetEpisodeNumber: Int? = null
) {
    var showSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var currentEp by remember { mutableStateOf<TvEpisodeEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current

    val isTablet =
        configuration.screenWidthDp >= 600

    val carouselItemWidth =
        if (isTablet) 640.dp else 512.dp

    val carouselItemHeight =
        if (isTablet) 275.dp else 180.dp

    val initialEpisodeIndex = remember(episodes, targetEpisodeNumber, season.lastEpWatched) {
        targetEpisodeNumber
            ?.let { target -> episodes.indexOfFirst { it.episode_number == target }.takeIf { it >= 0 } }
            ?: (season.lastEpWatched ?: 0).coerceAtLeast(0)
    }

    val carouselState = rememberCarouselState(
        itemCount = { episodes.count() },
        initialItem = initialEpisodeIndex.coerceIn(0, (episodes.size - 1).coerceAtLeast(0))
    )
    val scope = rememberCoroutineScope()
    val watchedItems = episodes.count { it.isWatched }

    LaunchedEffect(season.status, episodes) {
        if (season.status == WatchStatus.FINISHED && episodes.all { !it.isWatched }) {
            viewModel.markAllEpsWatched(
                showId = season.showId,
                seasonNumber = season.seasonNumber,
                lastEpNumber = episodes.size
            )
        }

        if (season.lastEpWatched != null && season.lastEpWatched > 0 && episodes.all { !it.isWatched }) {
            viewModel.markEpWatchedFromCount(
                showId = season.showId,
                seasonNumber = season.seasonNumber,
                count = season.lastEpWatched
            )
            carouselState.scrollToItem((season.lastEpWatched - 1).coerceAtLeast(0))
        }
    }

    MediaSectionCard(
        title = "Episodes",
        titleIcon = R.drawable.list_alt_24px,
        trailingContent = {
            MediaChip(
                "$watchedItems / ${episodes.size} watched",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shapeRadius = ShapeRadius.Small
            )
        }
    ) {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselItemHeight)
                .padding(top = 10.dp, start = 16.dp, end = 16.dp),
            preferredItemWidth = carouselItemWidth,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(0.dp),
        ) { index ->

            val item = episodes[index]

            // CLEAN SEQUENTIAL UNLOCK RULE:
            // 1. First episode (index 0) is always unlocked.
            // 2. To mark as WATCHED, previous episode must be watched.
            // 3. To mark as UNWATCHED, next episode cannot be watched yet (must unwatch from end of chain).
            val canMarkWatched = index == 0 || episodes[index - 1].isWatched
            val canMarkUnwatched = episodes.getOrNull(index + 1)?.isWatched != true

            Box(
                modifier = Modifier
                    .maskClip(RoundedCornerShape(ShapeRadius.ExtraLarge))
                    .width(carouselItemWidth)
                    .height(carouselItemHeight)
            ) {
                EpisodeItem(
                    item = item,
                    fallbackBackdropPath = backdropPath,
                    onTrailingAction = {
                        currentEp = item
                        showOptionsSheet = true
                    },
                    carouselItemWidth = carouselItemWidth,
                    carouselItemHeight = carouselItemHeight,
                    onItemClick = { clickedItem ->
                        if (carouselState.currentItem != index) {
                            scope.launch {
                                carouselState.animateScrollToItem(index)
                            }
                            return@EpisodeItem
                        }

                        if (season.status == WatchStatus.WANT_TO_WATCH) {
                            SnackbarManager.show("Please mark the season as 'Watching' to track episodes")
                            return@EpisodeItem
                        }

                        if (!clickedItem.isWatched && !canMarkWatched) {
                            SnackbarManager.show("Previous episode not watched")
                            return@EpisodeItem
                        }

                        if (clickedItem.isWatched && !canMarkUnwatched) {
                            SnackbarManager.show("Unwatch subsequent episodes first")
                            return@EpisodeItem
                        }

                        if (clickedItem.isWatched) {
                            viewModel.markEpUnWatched(
                                epId = clickedItem.epId,
                                showId = season.showId,
                                seasonNumber = season.seasonNumber,
                                episodeNumber = clickedItem.episode_number
                            )
                            if (currentEp?.epId == clickedItem.epId) {
                                currentEp = clickedItem.copy(isWatched = false, lastWatchedAt = 0L)
                            }
                        } else {
                            viewModel.markEpWatched(
                                epId = clickedItem.epId,
                                showId = season.showId,
                                seasonNumber = season.seasonNumber,
                                episodeNumber = clickedItem.episode_number
                            )
                        }
                    },
                    onEpisodeInfoClick = {
                        currentEp = item
                        showSheet = true
                    },
                    isActive = index == carouselState.currentItem
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    EpisodeOptionsSheet(
        show = showOptionsSheet,
        episode = currentEp,
        sheetState = optionsSheetState,
        onDismiss = { showOptionsSheet = false },
        onEditWatchedDate = {
            val index = episodes.indexOfFirst { it.epId == currentEp?.epId }
            val liveEp = episodes.getOrNull(index)
            val previousWatched = index <= 0 || episodes[index - 1].isWatched

            when {
                season.status == WatchStatus.WANT_TO_WATCH ->
                    SnackbarManager.show("Please mark the season as 'Watching' to track episodes")

                liveEp != null && !liveEp.isWatched && !previousWatched ->
                    SnackbarManager.show("Previous episode not watched")

                else -> showDatePicker = true
            }
        },
        seasonStatus = season.status
    )

    DatePickerSheet(
        show = showDatePicker,
        initialDate = currentEp?.lastWatchedAt?.takeIf { it > 0L },
        releaseDate = currentEp?.air_date?.let { date ->
            runCatching {
                LocalDate.parse(date)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        },
        id = currentEp?.epId ?: -1L,
        onDateSelected = { epId, dateMillis, _ ->
            currentEp?.let { ep ->
                viewModel.updateEpisodeWatchedDate(
                    epId = epId,
                    watchedAt = dateMillis ?: 0L,
                    showId = season.showId,
                    seasonNumber = season.seasonNumber,
                    episodeNumber = ep.episode_number
                )
            }
            showDatePicker = false
        },
        onDismiss = {
            showDatePicker = false
        }
    )

    TvDetailsEpisodeInfoSheet(
        show = showSheet,
        episode = currentEp,
        onDismiss = { showSheet = false },
        onConfirm = {
            currentEp?.let { ep ->
                if (season.status == WatchStatus.WANT_TO_WATCH) {
                    SnackbarManager.show("Please mark the season as 'Watching' to track episodes")
                    return@let
                }
                if (ep.isWatched) {
                    viewModel.markEpUnWatched(
                        epId = ep.epId,
                        showId = season.showId,
                        seasonNumber = season.seasonNumber,
                        episodeNumber = ep.episode_number
                    )
                    currentEp = ep.copy(isWatched = false, lastWatchedAt = 0L)
                } else {
                    viewModel.markEpWatched(
                        epId = ep.epId,
                        showId = season.showId,
                        seasonNumber = season.seasonNumber,
                        episodeNumber = ep.episode_number
                    )
                }
            }
        },
        sheetState = sheetState
    )
}
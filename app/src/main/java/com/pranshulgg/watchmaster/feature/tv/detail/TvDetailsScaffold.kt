package com.pranshulgg.watchmaster.feature.tv.detail

import android.annotation.SuppressLint

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.LoadingScreenPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.media.PosterBox
import com.pranshulgg.watchmaster.core.ui.navigation.NavRoutes
import com.pranshulgg.watchmaster.core.ui.navigation.navigateSafely
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.feature.shared.WatchlistViewModel
import com.pranshulgg.watchmaster.feature.shared.media.components.WatchListStatusPill
import com.pranshulgg.watchmaster.feature.shared.media.ui.FloatingToolbarMediaActionsParams
import com.pranshulgg.watchmaster.feature.shared.media.ui.MediaActionsFloatingToolbar
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.asStatusDates
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.toWatchListItemStatusUiPill
import com.pranshulgg.watchmaster.feature.tv.detail.ui.TvDetailsConfirmationDialog
import com.pranshulgg.watchmaster.feature.tv.detail.ui.TvDetailsNoteDialog
import com.pranshulgg.watchmaster.feature.tv.detail.ui.TvDetailsRatingDialog
import com.pranshulgg.watchmaster.feature.tv.detail.ui.TvWatchProviderSheet

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import com.pranshulgg.watchmaster.core.ui.components.listItemShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.pranshulgg.watchmaster.feature.lists.AddToListSheet
import com.pranshulgg.watchmaster.feature.tv.detail.ui.ChangeBackdropSheet

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TvDetailsScaffold(
    id: Long,
    seasonNumber: Int,
    targetEpisodeNumber: Int? = null,
    scrollBehavior: FloatingToolbarScrollBehavior,
    viewModel: TvDetailsViewModel,
    watchlistViewModel: WatchlistViewModel,
    navController: NavController
) {
    val loading = viewModel.loading

    val seasons by watchlistViewModel
        .seasonsForShow(id)
        .collectAsState(initial = emptyList())

    val season = seasons.find {
        it.seasonNumber == seasonNumber
    }

    var showSeasonMenu by remember {
        mutableStateOf(false)
    }

    val watchlistFlow = remember(id) {
        watchlistViewModel.item(id)
    }

    val watchlistItem by watchlistFlow
        .collectAsStateWithLifecycle()

    val episodesFlow = remember(id, seasonNumber) {
        viewModel.seasonEpisodes(id, seasonNumber)
    }

    val episodes by episodesFlow
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val isSeriesPinned = watchlistItem?.isPinned == true

    if (loading || season == null) {
        LoadingScreenPlaceholder()
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        bottomBar = {
            MediaActionsFloatingToolbar(
                scrollBehavior,
                season.status,
                actions = FloatingToolbarMediaActionsParams(
                    startWatching = {
                        watchlistViewModel.startSeason(
                            season.showId,
                            season.seasonNumber
                        )
                    },
                    finishWatching = {
                        if ((season.seasonUserRating ?: 0.0) > 0.0) {
                            watchlistViewModel.finishSeason(
                                showId = season.showId,
                                seasonNumber = season.seasonNumber,
                                lastEpNumber = season.episodeCount
                            )
                        } else {
                            viewModel.showRatingDialog(finishAfterRating = true)
                        }
                    },
                    showRating = {
                        viewModel.showRatingDialog(
                            originalRating = season.seasonUserRating?.toFloat() ?: 0f
                        )
                    },
                    interruptWatching = {
                        watchlistViewModel.interruptSeason(
                            season.showId,
                            season.seasonNumber
                        )
                    },
                    skipWatching = {
                        watchlistViewModel.skipSeason(
                            showId = season.showId,
                            seasonNumber = season.seasonNumber
                        )
                    },
                    togglePin = {
                        watchlistItem?.let {
                            watchlistViewModel.setPinned(
                                season.showId,
                                !it.isPinned
                            )
                        }
                    },
                    openLists = { viewModel.showListsSheet() },
                    changeBackdrop = { viewModel.showChangeBackdropSheet() },
                    moveToWatchlist = { watchlistViewModel.moveSeasonToWatchlist(season.showId, season.seasonNumber) },
                    dropItem = { watchlistViewModel.dropSeason(season.showId, season.seasonNumber) },
                    resetWatching = {
                        watchlistItem?.let {
                            if (it.isDropped) {
                                watchlistViewModel.unDropSeason(season.showId, season.seasonNumber)
                            } else {
                                watchlistViewModel.resetSeason(season.showId, season.seasonNumber)
                            }
                        }
                    },
                ),
                isPinned = isSeriesPinned,
                isTv = true,
                canMoveToWatchlist = season.seasonNumber == 1 &&
                    (season.lastEpWatched == null || season.lastEpWatched == 0)
            )
        }
    ) { _ ->
        viewModel.state?.let { tvItem ->
            TvDetailsContent(
                tvItem,
                navController,
                scrollBehavior,
                season,
                viewModel,
                watchlistViewModel,
                episodes,
                targetEpisodeNumber = targetEpisodeNumber,
                onSeasonClick = {
                    showSeasonMenu = true
                }
            )
        }
    }

    /*
     * Season selector bottom sheet
     */
    if (showSeasonMenu) {
        ModalBottomSheet(
            onDismissRequest = {
                showSeasonMenu = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Text(
                    text = "Seasons",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 12.dp
                    )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(
                        items = seasons,
                        key = { _, season -> "${season.showId}_${season.seasonNumber}" }
                    ) { index, availableSeason ->

                        val isOnly = seasons.singleOrNull() == availableSeason
                        val isFirst = index == 0
                        val isLast = index == seasons.lastIndex

                        val shape = listItemShape(
                            isOnly = isOnly,
                            isFirst = isFirst,
                            isLast = isLast
                        )

                        SeasonPickerRow(
                            season = availableSeason,
                            isDropped = watchlistItem?.isDropped == true,
                            shape = shape,
                            onClick = {
                                showSeasonMenu = false

                                navController.navigateSafely(
                                    NavRoutes.tvDetail(
                                        availableSeason.showId,
                                        availableSeason.seasonNumber,
                                    )
                                ) {
                                    popUpTo(navController.currentDestination?.id ?: return@navigateSafely) {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    TvDetailsNoteDialog(
        viewModel,
        watchlistViewModel,
        watchlistItem
    )

    TvDetailsRatingDialog(
        viewModel,
        watchlistViewModel,
        season
    )

    TvDetailsConfirmationDialog(
        viewModel,
        watchlistViewModel,
        season,
        navController
    )

    TvWatchProviderSheet(
        sheetState,
        onDismiss = {
            viewModel.hideWatchProviderSheet()
        },
        viewModel
    )

    if (viewModel.uiState.value.isListsSheetOpen) {
        AddToListSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            itemId = season.showId,
            onDismiss = { viewModel.hideListsSheet() }
        )
    }

    if (viewModel.uiState.value.isChangeBackdropSheetOpen) {
        ChangeBackdropSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            tvId = season.showId,
            onDismiss = { viewModel.hideChangeBackdropSheet() },
            onFetchBackdrops = { id -> viewModel.fetchBackdrops(id) },
            onSelect = { path ->
                viewModel.selectBackdrop(season.showId, path)
                viewModel.hideChangeBackdropSheet()
            }
        )
    }

}

@Composable
private fun SeasonPickerRow(
    season: SeasonEntity,
    isDropped: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit
) {

    val status =
        season.status.toWatchListItemStatusUiPill(
            season.asStatusDates()
        )

    val seasonStatusLabel =
        if (season.status == WatchStatus.WANT_TO_WATCH) {
            "Year: ${
                season.airDate
                    ?.takeIf { it.isNotBlank() }
                    ?.let { date ->
                        runCatching {
                            LocalDate.parse(
                                date,
                                DateTimeFormatter.ISO_LOCAL_DATE
                            ).year
                        }.getOrNull()
                    }
                    ?: "TBA"
            }"
        } else if (isDropped && season.status == WatchStatus.SKIPPED) {
            "Dropped"
        } else {
            status.statusLabel
        }


    val seasonProgress =
        if (season.episodeCount > 0) {
            ((season.lastEpWatched ?: 0) * 100 / season.episodeCount)
                .coerceIn(0, 100)
        } else {
            0
        }

    Surface(
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clipToBounds()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            PosterBox(
                posterUrl =
                    "https://image.tmdb.org/t/p/w154${season.posterPath}",
                apiPath = season.posterPath,
                cornerRadius = ShapeRadius.None,
                height = 100.dp,
                width = 65.dp,
                progressIndicatorSize = 40.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {

                Text(
                    text = season.name,
                    fontWeight = FontWeight.W900,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${season.episodeCount} episodes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Row {
                    WatchListStatusPill(
                        status.containerColor,
                        status.contentColor,
                        seasonStatusLabel,
                        season.status,
                        season.seasonUserRating
                    )

                    if (season.status != WatchStatus.FINISHED) {
                        Spacer(
                            modifier = Modifier.width(3.dp)
                        )

                        SeasonProgressPicker(
                            seasonProgress
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonProgressPicker(
    seasonProgress: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(ShapeRadius.Small)
    ) {
        Text(
            text = "$seasonProgress%",
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 8.dp
            )
        )
    }
}
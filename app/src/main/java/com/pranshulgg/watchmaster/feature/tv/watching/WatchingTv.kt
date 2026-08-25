package com.pranshulgg.watchmaster.feature.tv.watching

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.EmptyContainerPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.LoadingScreenPlaceholder
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.feature.shared.WatchlistViewModel
import com.pranshulgg.watchmaster.feature.tv.components.TvItems


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WatchingTv(
    isLoading: Boolean,
    items: List<WatchlistItemEntity>,
    scrollBehavior: FloatingToolbarScrollBehavior,
    scrollBehaviorTopBar: TopAppBarScrollBehavior,
    navController: NavController,
    onLongActionTvSeasonRequest: (SeasonEntity?, WatchlistItemEntity) -> Unit,
    seasons: List<SeasonEntity>,

    ) {

    val seasonsByShow = remember(seasons) {
        seasons.groupBy { it.showId }
    }

    val activeShowIds = remember(seasonsByShow) {
        seasonsByShow
            .filter { (_, showSeasons) ->
                val hasStartedSeason = showSeasons.any {
                    it.status == WatchStatus.WATCHING ||
                    it.status == WatchStatus.INTERRUPTED ||
                    it.status == WatchStatus.FINISHED
                }

                val allFinished = showSeasons.all {
                    it.status == WatchStatus.FINISHED
                }

                hasStartedSeason && !allFinished
            }
            .keys
    }

    val filteredItems = remember(items, activeShowIds) {
        items.filter { it.id in activeShowIds }
    }

    if (isLoading) {
        LoadingScreenPlaceholder()
    }

    if (filteredItems.isEmpty()) {
        EmptyContainerPlaceholder(
            R.drawable.movie_info_24px,
            "Nothing in progress",
            description = "Looks like you haven’t started watching anything. Find your next binge!"
        )
    }



    val (pinnedItems, normalItems) = remember(filteredItems) {
        filteredItems.partition { it.isPinned }
    }



    TvItems(
        scrollBehavior,
        scrollBehaviorTopBar,
        navController,
        onLongActionTvSeasonRequest,
        pinnedItems,
        normalItems,
        seasons,
    )

}
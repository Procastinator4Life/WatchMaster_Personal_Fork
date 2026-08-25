package com.pranshulgg.watchmaster.feature.tv.finished

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
fun FinishedTv(
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

    val finishedShowIds = remember(seasonsByShow) {
        seasonsByShow
            .filter { (_, showSeasons) ->
                showSeasons.isNotEmpty() &&
                showSeasons.all {
                    it.status == WatchStatus.FINISHED
                }
            }
            .keys
    }

    val filteredItems = remember(items, finishedShowIds) {
        items.filter { it.id in finishedShowIds }
    }

    if (isLoading) {
        LoadingScreenPlaceholder()
    }

    if (filteredItems.isEmpty()) {
        EmptyContainerPlaceholder(
            R.drawable.movie_info_24px,
            "No completed series",
            description = "You haven’t completed anything yet. Time to start and finish a series!"
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
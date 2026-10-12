package com.pranshulgg.watchmaster.feature.tv.watchlist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.EmptyContainerPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.LoadingScreenPlaceholder
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.feature.tv.components.TvItems
import com.pranshulgg.watchmaster.core.model.WatchStatus

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WatchlistTv(
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

    val watchlistShowIds = remember(seasonsByShow) {
        seasonsByShow
            .filter { (_, showSeasons) ->
                showSeasons.all {
                    it.status == WatchStatus.WANT_TO_WATCH
                }
            }
            .keys
    }

    val filteredItems = remember(items, watchlistShowIds) {
        items.filter { it.id in watchlistShowIds }
    }

    val (pinnedItems, normalItems) = remember(filteredItems) {
        filteredItems.partition { it.isPinned }
    }

    if (isLoading) {
        LoadingScreenPlaceholder()
        return
    }

    if (filteredItems.isEmpty()) {
        EmptyContainerPlaceholder(
            R.drawable.movie_info_24px,
            "No shows found",
            description = "Shows you want to watch will show up here. Find something to add!"
        )
        return
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
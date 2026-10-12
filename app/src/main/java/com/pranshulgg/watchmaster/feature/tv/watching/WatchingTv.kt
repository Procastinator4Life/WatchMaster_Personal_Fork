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

    val filteredItems = remember(items) {
        items.filter { it.status == WatchStatus.WATCHING }
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

    val seasonsByShow = remember(seasons) { seasons.groupBy { it.showId } }

    val sortedItems = remember(filteredItems, seasonsByShow) {
        filteredItems.sortedWith(
            compareBy<WatchlistItemEntity> { item ->
                seasonsByShow[item.id]?.any { it.status == WatchStatus.INTERRUPTED } == true
            }.thenByDescending { it.startedDate }
        )
    }

    val (pinnedItems, normalItems) = remember(sortedItems) {
        sortedItems.partition { it.isPinned }
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
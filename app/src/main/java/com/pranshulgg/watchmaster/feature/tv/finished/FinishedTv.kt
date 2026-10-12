package com.pranshulgg.watchmaster.feature.tv.finished

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Box
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.EmptyContainerPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.LoadingScreenPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.media.WatchedFilterDropdown
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.feature.shared.media.components.tv.TvWatchlistRow
import com.pranshulgg.watchmaster.feature.tv.components.TvWatchedGrid

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
    tvEpisodes: List<TvEpisodeEntity>,
) {
    var showDropped by remember { mutableStateOf(false) }

    val filteredItems = remember(items, showDropped) {
        items.filter { it.status == WatchStatus.FINISHED && it.isDropped == showDropped }
    }

    if (isLoading) {
        LoadingScreenPlaceholder()
        return
    }

    val filterDropdown: @Composable () -> Unit = {
        WatchedFilterDropdown(
            showDropped = showDropped,
            onSelect = { showDropped = it },
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        )
    }

    if (filteredItems.isEmpty()) {
        Column {
            filterDropdown()
            EmptyContainerPlaceholder(
                R.drawable.movie_info_24px,
                if (showDropped) "No dropped series" else "No completed series",
                description = if (showDropped)
                    "You haven't dropped any series yet."
                else
                    "You haven't completed anything yet. Time to start and finish a series!"
            )
        }
    } else if (showDropped) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior)
                .nestedScroll(scrollBehaviorTopBar.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Box(Modifier.padding(bottom = 12.dp)) { filterDropdown() }
            }
            itemsIndexed(
                filteredItems,
                key = { _, item -> item.id }
            ) { index, item ->
                TvWatchlistRow(
                    item,
                    index,
                    filteredItems,
                    navController,
                    onLongActionTvSeasonRequest = onLongActionTvSeasonRequest,
                    seasons = seasons
                )
            }
        }
    } else {
        TvWatchedGrid(
            isLoading = false,
            items = filteredItems,
            seasons = seasons,
            tvEpisodes = tvEpisodes,
            scrollBehavior = scrollBehavior,
            scrollBehaviorTopBar = scrollBehaviorTopBar,
            navController = navController,
            onLongActionTvSeasonRequest = onLongActionTvSeasonRequest,
            header = filterDropdown
        )
    }
}
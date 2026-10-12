package com.pranshulgg.watchmaster.feature.movie.finished

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.EmptyContainerPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.LoadingScreenPlaceholder
import com.pranshulgg.watchmaster.core.ui.components.media.WatchedFilterDropdown
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.feature.movie.components.MovieItems
import com.pranshulgg.watchmaster.feature.movie.components.MovieWatchedGrid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FinishedMovies(
    isLoading: Boolean = true,
    items: List<WatchlistItemEntity>,
    scrollBehavior: FloatingToolbarScrollBehavior,
    scrollBehaviorTopBar: TopAppBarScrollBehavior,
    navController: NavController,
    onLongActionMovieRequest: (WatchlistItemEntity) -> Unit
) {
    var showDropped by remember { mutableStateOf(false) }

    val filteredItems = remember(items, showDropped) {
        items.filter { it.status == WatchStatus.FINISHED && it.isDropped == showDropped }
    }

    if (isLoading) {
        LoadingScreenPlaceholder()
        return
    }

    Column {
        val gridHeader: @Composable () -> Unit = {
            WatchedFilterDropdown(
                showDropped = showDropped,
                onSelect = { showDropped = it },
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
        }

        // The dropped list already has 16dp side padding, so no start padding here
        val listHeader: @Composable () -> Unit = {
            WatchedFilterDropdown(
                showDropped = showDropped,
                onSelect = { showDropped = it },
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (filteredItems.isEmpty()) {
            Column {
                gridHeader()
                EmptyContainerPlaceholder(
                    R.drawable.movie_info_24px,
                    if (showDropped) "No dropped movies" else "No completed movies",
                    description = if (showDropped)
                        "You haven't dropped any movies yet."
                    else
                        "You haven't completed anything yet. Time to start and finish a movie!"
                )
            }
        } else if (showDropped) {
            val pinnedItems = filteredItems.filter { it.isPinned }
            val normalItems = filteredItems.filter { !it.isPinned }

            MovieItems(
                scrollBehavior,
                scrollBehaviorTopBar,
                navController,
                onLongActionMovieRequest,
                pinnedItems,
                normalItems,
                header = listHeader
            )
        } else {
            MovieWatchedGrid(
                items = filteredItems,
                scrollBehavior = scrollBehavior,
                scrollBehaviorTopBar = scrollBehaviorTopBar,
                navController = navController,
                onLongActionMovieRequest = onLongActionMovieRequest,
                header = gridHeader
            )
        }
    }
}
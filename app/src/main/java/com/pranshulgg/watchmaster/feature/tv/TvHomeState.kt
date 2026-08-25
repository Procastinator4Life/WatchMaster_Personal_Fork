package com.pranshulgg.watchmaster.feature.tv

import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity

data class TvHomeState(
    val items: List<WatchlistItemEntity> = emptyList(),
    val isLoading: Boolean = false,
    val seasons: List<SeasonEntity> = emptyList(),
)

fun filterTvItems(
    items: List<WatchlistItemEntity>,
    isLoading: Boolean,
    seasonItems: List<SeasonEntity>
): TvHomeState {
    return TvHomeState(
        items = items.filter { it.mediaType != "movie" },
        seasons = seasonItems,
        isLoading = isLoading
    )
}
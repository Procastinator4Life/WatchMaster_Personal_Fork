// Save in: com.pranshulgg.watchmaster.feature.home.model.WatchNextItem.kt
package com.pranshulgg.watchmaster.feature.home.model

import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity

data class WatchNextItem(
    val showId: Long,
    val showTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String,
    val stillPath: String?,
    val backdropPath: String?,
    val episodeEntity: TvEpisodeEntity,
    val status: WatchStatus = WatchStatus.WATCHING,
    val lastWatchedAt: Long = 0L,
    val isReleased: Boolean = true
) {
    val displayImagePath: String?
        get() = stillPath?.ifEmpty { null } ?: backdropPath?.ifEmpty { null }
}
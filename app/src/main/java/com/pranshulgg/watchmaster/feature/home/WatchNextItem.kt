// Save in: com.pranshulgg.watchmaster.feature.home.model.WatchNextItem.kt
package com.pranshulgg.watchmaster.feature.home.model

import com.pranshulgg.watchmaster.core.model.WatchStatus

data class WatchNextItem(
    val showId: Long,
    val showTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String,
    val stillPath: String?,    // Episode image (item.still_path)
    val backdropPath: String?, // Show backdrop fallback
    val status: WatchStatus = WatchStatus.WATCHING
) {
    // Uses episode image if available; falls back to show backdrop
    val displayImagePath: String?
        get() = stillPath?.ifEmpty { null } ?: backdropPath?.ifEmpty { null }
}
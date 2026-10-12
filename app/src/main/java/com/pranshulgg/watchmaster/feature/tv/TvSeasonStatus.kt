package com.pranshulgg.watchmaster.feature.tv

import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity

fun List<SeasonEntity>.statusForShow(showId: Long): WatchStatus {
    val showSeasons = filter { it.showId == showId }

    if (showSeasons.isEmpty()) {
        return WatchStatus.WANT_TO_WATCH
    }

    if (showSeasons.any {
            it.status == WatchStatus.WATCHING ||
            it.status == WatchStatus.INTERRUPTED
        }
    ) {
        return WatchStatus.WATCHING
    }

    // A show with only completed/skipped seasons is completed. A mixture of
    // completed/skipped and a pending season is handled as Watching by the
    // repository after it checks whether that pending season has aired.
    if (showSeasons.all {
            it.status == WatchStatus.FINISHED ||
            it.status == WatchStatus.SKIPPED
        }
    ) {
        return if (showSeasons.all { it.status == WatchStatus.SKIPPED }) {
            WatchStatus.SKIPPED
        } else {
            WatchStatus.FINISHED
        }
    }

    return WatchStatus.WATCHING
}

package com.pranshulgg.watchmaster.feature.tv

import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity

fun List<SeasonEntity>.statusForShow(showId: Long): WatchStatus {
    val showSeasons = filter { it.showId == showId }

    if (showSeasons.isEmpty()) {
        return WatchStatus.WANT_TO_WATCH
    }

    val hasWatching = showSeasons.any {
        it.status == WatchStatus.WATCHING ||
        it.status == WatchStatus.INTERRUPTED
    }

    if (hasWatching) {
        return WatchStatus.WATCHING
    }

    val hasFinished = showSeasons.any {
        it.status == WatchStatus.FINISHED
    }

    val hasPending = showSeasons.any {
        it.status == WatchStatus.WANT_TO_WATCH
    }

    return when {
        hasFinished && hasPending -> WatchStatus.WATCHING
        hasFinished && !hasPending -> WatchStatus.FINISHED
        else -> WatchStatus.WANT_TO_WATCH
    }
}
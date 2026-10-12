package com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus

import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.R

fun WatchStatus.dialogMessage(isTv: Boolean = false): String {

    val finishText: String = if (isTv) "season and mark all episodes as watched" else "movie";
    val resetText: String = if (isTv) "season, mark all episodes as unwatched," else "movie";

    val seasonMovieText: String = if (isTv) "season" else "movie";

    return when (this) {

        WatchStatus.WATCHING ->
            "Are you sure you want to finish this $finishText?"

        WatchStatus.INTERRUPTED ->
            "Do you want to continue watching this $seasonMovieText?"

        WatchStatus.FINISHED ->
            "Doing this will send this season back to 'Watching'. Are you sure you want to reset this season?"

        WatchStatus.SKIPPED ->
            "Do you want to start watching this $seasonMovieText?"

        WatchStatus.WANT_TO_WATCH ->
            "Are you sure you want to start watching this $seasonMovieText?"
    }
}

fun WatchStatus.confirmAction(
    start: () -> Unit,
    reset: () -> Unit,
    finish: () -> Unit,
) {
    when (this) {
        WatchStatus.WATCHING -> finish()
        WatchStatus.FINISHED -> reset()
        WatchStatus.INTERRUPTED,
        WatchStatus.WANT_TO_WATCH,
        WatchStatus.SKIPPED -> start()
    }
}

val WatchStatus.actionLabel: String
    get() = when (this) {
        WatchStatus.WATCHING -> "Mark as finished"
        WatchStatus.INTERRUPTED -> "Continue watching"
        WatchStatus.FINISHED -> "Rewatch"
        WatchStatus.SKIPPED -> "Mark as watching"
        WatchStatus.WANT_TO_WATCH -> "Mark as watching"
    }

val WatchStatus.buttonIcon: Int
    get() = when (this) {
        WatchStatus.WATCHING -> R.drawable.check_24px
        WatchStatus.FINISHED -> R.drawable.restart_alt_24px
        WatchStatus.INTERRUPTED,
        WatchStatus.WANT_TO_WATCH,
        WatchStatus.SKIPPED -> R.drawable.play_arrow_24px
    }

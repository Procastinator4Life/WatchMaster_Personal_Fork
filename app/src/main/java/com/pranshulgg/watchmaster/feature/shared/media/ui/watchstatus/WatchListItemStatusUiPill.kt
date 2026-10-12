package com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.theme.LocalStatusColors
import com.pranshulgg.watchmaster.core.utils.formatDate
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import java.time.Instant


data class WatchListItemStatusUiPill(
    val statusLabel: String,
    val containerColor: Color,
    val contentColor: Color
)

interface WatchStatusDates {
    val startedDate: Instant?
    val finishedDate: Instant?
    val interruptedDate: Instant?
    val addedDate: Instant
}

fun SeasonEntity.asStatusDates() = object : WatchStatusDates {
    override val startedDate = seasonStartedDate
    override val finishedDate = seasonFinishedDate
    override val interruptedDate = seasonInterruptedAt
    override val addedDate = seasonAddedDate
}

fun WatchlistItemEntity.asStatusDates(): WatchStatusDates {
    val item = this
    return object : WatchStatusDates {
        override val startedDate: Instant? = item.startedDate
        override val finishedDate: Instant? = item.finishedDate
        override val interruptedDate: Instant? = item.interruptedAt
        override val addedDate: Instant = item.addedDate
    }
}

@Composable
fun WatchStatus.toWatchListItemStatusUiPill(
    item: WatchStatusDates,
    isDropped: Boolean = false
): WatchListItemStatusUiPill {
    val statusColor = LocalStatusColors.current

    return when (this) {
        WatchStatus.WATCHING -> {
            WatchListItemStatusUiPill(
                statusLabel = item.startedDate?.let { "Started • ${it.formatDate()}" } ?: "Watching",
                containerColor = statusColor.warning.bg,
                contentColor = statusColor.warning.on
            )
        }

        WatchStatus.FINISHED -> {
            if (isDropped) {
                WatchListItemStatusUiPill(
                    statusLabel = "Dropped • ${item.finishedDate?.formatDate()}",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            } else {
                WatchListItemStatusUiPill(
                    statusLabel = "Finished • ${item.finishedDate?.formatDate()}",
                    containerColor = statusColor.success.bg,
                    contentColor = statusColor.success.on
                )
            }
        }

        WatchStatus.INTERRUPTED -> {
            WatchListItemStatusUiPill(
                statusLabel = item.interruptedDate?.let { "On Hold • ${it.formatDate()}" } ?: "On Hold",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        WatchStatus.WANT_TO_WATCH -> {
            WatchListItemStatusUiPill(
                statusLabel = "Pending",
                containerColor = statusColor.pending.bg,
                contentColor = statusColor.pending.on
            )
        }

        WatchStatus.SKIPPED -> {
            WatchListItemStatusUiPill(
                statusLabel = "Skipped",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
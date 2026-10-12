package com.pranshulgg.watchmaster.feature.shared.media.components.tv

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.media.PosterBox
import com.pranshulgg.watchmaster.core.ui.components.listItemShape
import com.pranshulgg.watchmaster.core.ui.navigation.NavRoutes
import com.pranshulgg.watchmaster.core.ui.navigation.navigateSafely
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import androidx.compose.foundation.layout.Spacer
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.asStatusDates
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.toWatchListItemStatusUiPill
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TvWatchlistRow(
    item: WatchlistItemEntity,
    index: Int,
    items: List<WatchlistItemEntity>,
    navController: NavController,
    onLongActionTvSeasonRequest: (SeasonEntity?, WatchlistItemEntity) -> Unit,
    seasons: List<SeasonEntity>,
    showStatusPill: Boolean = true,
) {
    val isOnly = items.singleOrNull() == item
    val isFirst = index == 0
    val isLast = index == items.lastIndex
    val shape = listItemShape(isOnly, isFirst, isLast)

    val titleMaxLines = 2
    val overviewMaxLines = remember { mutableIntStateOf(1) }
    var isReady by remember { mutableStateOf(false) }

    val targetSeason = remember(seasons, item.id) {
        val showSeasons = seasons.filter { it.showId == item.id }.sortedBy { it.seasonNumber }
        showSeasons.firstOrNull { it.status == WatchStatus.WATCHING }
            ?: showSeasons.firstOrNull { it.status == WatchStatus.INTERRUPTED }
            ?: showSeasons.firstOrNull { it.seasonNumber == 1 }
            ?: showSeasons.firstOrNull()
    }

    val effectiveStatus = if (targetSeason?.status == WatchStatus.INTERRUPTED) {
        WatchStatus.INTERRUPTED
    } else {
        item.status
    }

    Surface(
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .alpha(if (isReady) 1f else 0f)
            .combinedClickable(
                onClick = {
                    targetSeason?.let { season ->
                        navController.navigateSafely(
                            NavRoutes.tvDetail(
                                season.showId,
                                season.seasonNumber,
                            )
                        )
                    }
                },
                onLongClick = {
                    onLongActionTvSeasonRequest(null, item)
                },
            ),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clipToBounds()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterBox(
                posterUrl = "https://image.tmdb.org/t/p/w154${item.posterPath}",
                apiPath = item.posterPath,
                cornerRadius = ShapeRadius.None,
                progressIndicatorSize = 40.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.W900,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        val newLines = if (result.lineCount == 1) 2 else 1
                        if (overviewMaxLines.intValue != newLines) {
                            overviewMaxLines.intValue = newLines
                            isReady = true
                        } else {
                            isReady = true
                        }
                    }
                )
                Text(
                    item.overview ?: "No overview found",
                    maxLines = overviewMaxLines.intValue,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(5.dp))
                if (showStatusPill) {
                    
                    val status = effectiveStatus.toWatchListItemStatusUiPill(item.asStatusDates(), isDropped = item.isDropped)
                    val releaseDate = item.releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    val daysUntilRelease = releaseDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

                    val statusLabel = if (effectiveStatus == WatchStatus.WANT_TO_WATCH) {
                        when {
                            daysUntilRelease == null || daysUntilRelease <= 0 -> status.statusLabel
                            daysUntilRelease <= 30 -> "$daysUntilRelease ${if (daysUntilRelease == 1L) "day" else "days"} away"
                            else -> {
                                val months = ceil(daysUntilRelease / 30.0).toInt().coerceAtLeast(1)
                                "$months ${if (months == 1) "month" else "months"} away"
                            }
                        }
                    } else {
                        status.statusLabel
                    }
                    com.pranshulgg.watchmaster.feature.shared.media.components.WatchListStatusPill(
                        status.containerColor,
                        status.contentColor,
                        statusLabel,
                        effectiveStatus,
                        item.userRating
                    )
                }
            }
        }
    }
}
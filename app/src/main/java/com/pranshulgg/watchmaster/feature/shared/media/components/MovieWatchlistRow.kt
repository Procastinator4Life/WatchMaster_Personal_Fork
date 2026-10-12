package com.pranshulgg.watchmaster.feature.shared.media.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.pranshulgg.watchmaster.core.ui.components.listItemShape
import com.pranshulgg.watchmaster.core.ui.components.media.PosterBox
import com.pranshulgg.watchmaster.core.ui.components.media.PosterPlaceholder
import com.pranshulgg.watchmaster.core.ui.navigation.NavRoutes
import com.pranshulgg.watchmaster.core.ui.navigation.navigateSafely
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.asStatusDates
import com.pranshulgg.watchmaster.feature.shared.media.ui.watchstatus.toWatchListItemStatusUiPill
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MovieWatchlistRow(
    item: WatchlistItemEntity,
    index: Int,
    items: List<WatchlistItemEntity>,
    navController: NavController,
    onLongActionMovieRequest: () -> Unit,
    showStatusPill: Boolean = true,
) {

    val isOnly = items.singleOrNull() == item
    val isFirst = index == 0
    val isLast = index == items.lastIndex

    val titleMaxLines = 2
    val overviewMaxLines = remember { mutableIntStateOf(1) }
    var isReady by remember { mutableStateOf(false) }


    val poster = item.posterPath?.let {
        "https://image.tmdb.org/t/p/w154$it"
    }

    val status = item.status.toWatchListItemStatusUiPill(item.asStatusDates(), isDropped = item.isDropped)

    val releaseDate = item.releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val daysUntilRelease = releaseDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    val statusLabel = if (item.status == com.pranshulgg.watchmaster.core.model.WatchStatus.WANT_TO_WATCH) {
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

    val shape = listItemShape(isOnly, isFirst, isLast)


    Surface(
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .alpha(if (isReady) 1f else 0f)
            .combinedClickable(
                onClick = {
                    navController.navigateSafely(NavRoutes.movieDetail(item.id))
                },
                onLongClick = {
                    onLongActionMovieRequest()
                }
            ),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clipToBounds()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(
                space = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp, height = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                PosterBox(
                    posterUrl = poster,
                    apiPath = item.posterPath,
                    width = 80.dp,
                    height = 120.dp,
                    progressIndicatorSize = 40.dp,
                    cornerRadius = ShapeRadius.None
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                    WatchListStatusPill(
                        status.containerColor,
                        status.contentColor,
                        statusLabel,
                        item.status,
                        item.userRating,
                    )
                }
            }
        }
    }


}



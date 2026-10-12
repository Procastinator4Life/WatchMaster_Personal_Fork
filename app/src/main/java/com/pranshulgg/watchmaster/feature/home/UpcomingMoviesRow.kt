package com.pranshulgg.watchmaster.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pranshulgg.watchmaster.core.ui.components.Gap
import com.pranshulgg.watchmaster.core.ui.components.media.PosterBox
import com.pranshulgg.watchmaster.core.ui.components.media.PosterPlaceholder
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class UpcomingMovieItem(
    val movieId: Long,
    val title: String,
    val posterPath: String?,
    val releaseDate: LocalDate?
)

private const val MAX_UPCOMING_MOVIES_SHOWN = 10

@Composable
fun UpcomingMoviesRow(
    items: List<UpcomingMovieItem>,
    onMovieClick: (UpcomingMovieItem) -> Unit
) {
    if (items.isEmpty()) return

    val visibleItems = items.take(MAX_UPCOMING_MOVIES_SHOWN)
    val today = remember { LocalDate.now() }
    val dayOfWeekFormatter = remember { DateTimeFormatter.ofPattern("EEEE") }

    val cardWidth = posterCardWidth()
    val cardHeight = cardWidth * 1.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {

    SectionTitle("Upcoming Movies")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = visibleItems,
                key = { it.movieId }
            ) { movie ->
                UpcomingMovieCard(
                    movie = movie,
                    today = today,
                    dayOfWeekFormatter = dayOfWeekFormatter,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

@Composable
private fun UpcomingMovieCard(
    movie: UpcomingMovieItem,
    today: LocalDate,
    dayOfWeekFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    val posterUrl = movie.posterPath
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""

    val daysAway = movie.releaseDate?.let {
        ChronoUnit.DAYS.between(today, it)
    }

    val isReleasedToday = daysAway == 0L
    val isReleasedTomorrow = daysAway == 1L

    // Updated logic: matches show formatting ("Out Today", "Tomorrow", "This [Day]", or "[X] days away")
    val dateLabel = when {
        isReleasedToday -> "Out Today"
        isReleasedTomorrow -> "Tomorrow"
        daysAway != null && daysAway in 2..7 -> "This ${movie.releaseDate.format(dayOfWeekFormatter)}"
        daysAway != null && daysAway > 7 -> "$daysAway days away"
        else -> "TBA"
    }

    val cardWidth = posterCardWidth()
    val cardHeight = cardWidth * 1.5f

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
        ) {
            PosterBox(
                posterUrl = posterUrl,
                apiPath = movie.posterPath,
                width = cardWidth,
                height = cardHeight,
                cornerRadius = ShapeRadius.Medium,
                placeholder = {
                    PosterPlaceholder(
                        bgColor = MaterialTheme.colorScheme.surfaceBright
                    )
                },
                progressIndicatorSize = 24.dp
            )
        }

        Gap(8.dp)

        Text(
            text = movie.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Gap(2.dp)

        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = if (isReleasedToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isReleasedToday) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
package com.pranshulgg.watchmaster.feature.movie.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.media.MediaChip
import com.pranshulgg.watchmaster.core.ui.components.media.MediaDetailsScreenHeader
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import com.pranshulgg.watchmaster.data.local.entity.MovieBundle
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MovieHeroHeader(
    movie: MovieBundle,
    watchlistItem: WatchlistItemEntity?,
    navController: NavController,
    isFinished: Boolean,
    userRating: Double? = 0.0,
    onUpdateRating: (Double) -> Unit,
    onRemoveRating: () -> Unit = {}
) {
    
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val backdropHeight = if (isTablet) 480.dp else 340.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = backdropUrl(movie, watchlistItem),
            contentDescription = movie.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(backdropHeight),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )

        MediaDetailsScreenHeader(
            navController,
            isFinished,
            userRating,
            onUpdateRating = onUpdateRating,
            onRemoveRating = onRemoveRating
        )
        
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.20f to Color.Transparent,
                            0.34f to MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.10f),
                            0.49f to MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.25f),
                            0.64f to MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                            0.81f to MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
                            1.0f to MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 6.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Movie title
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        offset = Offset(2f, 2f),
                        blurRadius = 6f
                    )
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                lineHeight = 30.sp,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Small gap between title and info
            Spacer(
                modifier = Modifier.height(2.dp)
            )

            // Runtime + release year
            Text(
                text = "${formatRuntime(movie.runtime)} • ${watchlistItem?.releaseDate?.take(4) ?: "—"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Slightly larger gap between info and badges
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // Genre + rating badges
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp,
                    Alignment.CenterHorizontally
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                movie.genres.forEach { genre ->
                    MediaChip(
                        genre.name,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }

                MediaChip(
                    "%.1f".format(watchlistItem?.avgRating),
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    shapeRadius = ShapeRadius.Small,
                    icon = R.drawable.star_24px
                )
            }
        }
    }
}


private fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return "—"

    val h = minutes / 60
    val m = minutes % 60

    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun backdropUrl(movie: MovieBundle, watchlistItem: WatchlistItemEntity?): String? =
    when {
        watchlistItem?.backdropOverride != null ->
            "https://image.tmdb.org/t/p/original${watchlistItem.backdropOverride}"

        movie.backdrop_path != null ->
            "https://image.tmdb.org/t/p/original${movie.backdrop_path}"

        movie.images.backdrops.isNotEmpty() ->
            "https://image.tmdb.org/t/p/original${movie.images.backdrops.first().file_path}"

        else -> null
    }
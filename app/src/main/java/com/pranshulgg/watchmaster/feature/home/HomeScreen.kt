package com.pranshulgg.watchmaster.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.Gap
import com.pranshulgg.watchmaster.core.ui.components.Symbol
import com.pranshulgg.watchmaster.core.ui.components.media.PosterBox
import com.pranshulgg.watchmaster.core.ui.components.media.PosterPlaceholder
import com.pranshulgg.watchmaster.core.ui.navigation.NavRoutes
import com.pranshulgg.watchmaster.core.ui.theme.ShapeRadius
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.feature.tv.detail.ui.TvDetailsEpisodeInfoSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    // Collect Flow state continuously
    val watchNextList by viewModel.watchNextItems.collectAsState()
    
    var selectedEpisode by remember { mutableStateOf<TvEpisodeEntity?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = innerPadding.calculateBottomPadding(),
                    top = 0.dp
                )
                .fillMaxSize()
        ) {
            if (watchNextList.isNotEmpty()) {
                WatchNextSection(
                    items = watchNextList,
                    onItemClick = { item ->
                        navController.navigate("${NavRoutes.TV_DETAIL_SCREEN}/${item.showId}/${item.seasonNumber}/${item.seasonId}")
                    },
                    onThumbnailClick = { episode ->
                        selectedEpisode = episode
                        showSheet = true
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing here yet")
                }
            }
        }
    }

    if (showSheet && selectedEpisode != null) {
        TvDetailsEpisodeInfoSheet(
            episode = selectedEpisode,
            show = showSheet,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showSheet = false
                    selectedEpisode = null
                }
            },
            onConfirm = {
                selectedEpisode?.let { ep ->
                    viewModel.toggleEpisodeWatched(ep)
                }
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showSheet = false
                    selectedEpisode = null
                }
            }
        )
    }
}

@Composable
fun WatchNextSection(
    items: List<WatchNextItem>,
    onItemClick: (WatchNextItem) -> Unit,
    onThumbnailClick: (TvEpisodeEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Watch Next",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            // Reduced top padding from 12.dp to 4.dp to bring it closer to header
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 3.dp, bottom = 8.dp) //Padding for Watch Next section
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.showId }) { item ->
                WatchNextCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onThumbnailClick = { onThumbnailClick(item.episodeEntity) }
                )
            }
        }
    }
}

@Composable
fun WatchNextCard(
    item: WatchNextItem,
    onClick: () -> Unit,
    onThumbnailClick: () -> Unit
) {
    val imagePath = item.displayImagePath
    val fullImageUrl = if (imagePath != null) "https://image.tmdb.org/t/p/w500$imagePath" else ""

    Column(
        modifier = Modifier
            .width(260.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.clickable { onThumbnailClick() }
        ) {
            PosterBox(
                posterUrl = fullImageUrl,
                apiPath = imagePath,
                width = 260.dp,
                height = 146.dp,
                cornerRadius = ShapeRadius.ExtraLarge,
                placeholder = {
                    PosterPlaceholder(
                        bgColor = MaterialTheme.colorScheme.surfaceBright
                    )
                },
                progressIndicatorSize = 34.dp
            )
        }

        Gap(8.dp)

        Text(
            text = item.showTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Gap(2.dp)

        // Updated text format to include S1 E1 and removed the Symbol (clock icon)
        Text(
            text = "S${item.seasonNumber} E${item.episodeNumber} • ${item.episodeName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
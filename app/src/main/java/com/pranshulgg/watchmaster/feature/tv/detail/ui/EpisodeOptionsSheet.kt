package com.pranshulgg.watchmaster.feature.tv.detail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.core.ui.components.ActionBottomSheet
import com.pranshulgg.watchmaster.core.ui.components.Symbol
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.core.utils.formatDate
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import java.time.Instant
import androidx.compose.foundation.layout.fillMaxWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeOptionsSheet(
    episode: TvEpisodeEntity?,
    show: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onEditWatchedDate: () -> Unit,
    seasonStatus: WatchStatus = WatchStatus.WATCHING
) {
    if (show && episode != null) {
        ActionBottomSheet(
            sheetState = sheetState,
            onCancel = onDismiss,
            onConfirm = {},
            showActions = false
        ) { hide ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    onClick = {
                        hide()
                        if (seasonStatus == WatchStatus.WANT_TO_WATCH) {
                            SnackbarManager.show("Please mark the season as 'Watching' to track episodes")
                        } else {
                            onEditWatchedDate()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Symbol(
                            R.drawable.schedule_24px,
                            size = 22.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Edit Watched Date",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Watched Date:",
                        style = MaterialTheme.typography.titleMedium,   // 16sp
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = episode.lastWatchedAt
                            .takeIf { it > 0L }
                            ?.let { Instant.ofEpochMilli(it).formatDate() }
                            ?: "N/A",
                        style = MaterialTheme.typography.titleLarge,    // 22sp
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

package com.pranshulgg.watchmaster.feature.setting

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.prefs.LocalAppPrefs
import com.pranshulgg.watchmaster.core.ui.components.CheckboxRow
import com.pranshulgg.watchmaster.core.ui.components.DialogBasic
import com.pranshulgg.watchmaster.core.ui.components.LargeTopBarScaffold
import com.pranshulgg.watchmaster.core.ui.components.NavigateUpBtn
import com.pranshulgg.watchmaster.core.ui.components.SettingSection
import com.pranshulgg.watchmaster.core.ui.components.SettingTile
import com.pranshulgg.watchmaster.core.ui.components.SettingsTileIcon
import com.pranshulgg.watchmaster.core.ui.components.TextAlertDialog
import com.pranshulgg.watchmaster.feature.setting.components.ColorPickerBtn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.core.sync.SyncManager
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import android.content.Context
import android.net.Uri

@Composable
fun SettingsScreen(navController: NavController) {

    val prefs = LocalAppPrefs.current
    val isAndroid12Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current


    var exportWatchlistChecked by remember { mutableStateOf(true) }
    var exportMovieListChecked by remember { mutableStateOf(false) }
    var isRemoveSyncDialogOpen by remember { mutableStateOf(false) }

    val exportLauncher = exportLauncher(context, exportWatchlistChecked, exportMovieListChecked)
    val importLauncher = importLauncher(context)

    val syncFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.setSyncFolderUri(it.toString())
            SnackbarManager.show("Sync folder set")
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val datesViewModel: TraktDatesViewModel = hiltViewModel()
    var pendingDatesJson by remember { mutableStateOf<String?>(null) }
    var datesPreview by remember { mutableStateOf<String?>(null) }

    var pendingWatchedJson by remember { mutableStateOf<String?>(null) }
    var watchedPreview by remember { mutableStateOf<String?>(null) }

    val watchedPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val text = datesViewModel.combine(readTexts(context, uris))
                    val (movieTotal, movieMissing) = datesViewModel.countMissingMovies(text)
                    val (showTotal, showMissing) = datesViewModel.countMissingShows(text)
                    val (_, progressUsable) = datesViewModel.countProgressCandidates(text)
                    pendingWatchedJson = text
                    watchedPreview = "Movies not in your app yet: $movieMissing of $movieTotal\n" +
                        "Shows not in your app yet: $showMissing of $showTotal\n\n" +
                        "They'll be added as Finished with their Trakt dates. " +
                        "Shows you've finished are added as Finished. Shows you're partway through " +
                        "($progressUsable can be imported with progress) are added as Watching, " +
                        "and seasons you skipped are set to Skipped. " +
                        "Anything already in your app isn't touched.\n\n" +
                        "This needs wifi and can take several minutes."
                } catch (e: Exception) {
                    SnackbarManager.show("Couldn't read those files")
                }
            }
        }
    }

    val datesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val text = datesViewModel.combine(readTexts(context, uris))
                    val s = datesViewModel.run(text, dryRun = true)
                    pendingDatesJson = text
                    datesPreview = "Movies to update: ${s.movies}\n" +
                        "Seasons to update: ${s.seasons}\n" +
                        "Episodes to date: ${s.episodes}\n" +
                        "Seasons with no episodes saved (skipped): ${s.noEpisodes}\n" +
                        "Items not in your app (skipped): ${s.notInApp}\n\n" +
                        "Only missing dates are filled in. Nothing else changes."
                } catch (e: Exception) {
                    SnackbarManager.show("Couldn't read that file")
                }
            }
        }
    }

    var pendingWatchlistJson by remember { mutableStateOf<String?>(null) }
    var watchlistPreview by remember { mutableStateOf<String?>(null) }

    val watchlistPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val text = datesViewModel.combine(readTexts(context, uris))
                    val (total, missing) = datesViewModel.countMissingWatchlist(text)
                    pendingWatchlistJson = text
                    watchlistPreview = "$missing of $total watchlist items aren't in your app yet.\n\n" +
                        "They'll be added to your Watchlist as \"want to watch\". " +
                        "Shows with no season info yet are skipped, like adding by hand. " +
                        "Items already in your app aren't touched.\n\n" +
                        "This needs wifi."
                } catch (e: Exception) {
                    SnackbarManager.show("Couldn't read that file")
                }
            }
        }
    }

    var isExportDialogOpen by remember { mutableStateOf(false) }

    var isWarningImportDialogOpen by remember { mutableStateOf(false) }

    LargeTopBarScaffold(
        title = "Settings",
        navigationIcon = { NavigateUpBtn(navController) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingSection(
                title = "App looks",
                tiles = listOf(
                    SettingTile.DialogOptionTile(
                        leading = { SettingsTileIcon(R.drawable.palette_24px) },
                        title = "App theme",
                        options = listOf(
                            "Dark",
                            "Light",
                            "System",
                        ),
                        selectedOption = prefs.appTheme,
                        onOptionSelected = {
                            prefs.setAppTheme(it)
                        }
                    ),
                    SettingTile.SwitchTile(
                        leading = {
                            if (prefs.isCustomTheme) ColorPickerBtn() else SettingsTileIcon(
                                R.drawable.brush_24px
                            )
                        },
                        title = "Use custom color",
                        description = "Select a seed color to generate the theme",
                        checked = prefs.isCustomTheme,
                        enabled = !prefs.useDynamicColor,
                        onCheckedChange = { checked ->
                            prefs.useCustomTheme(checked)
                            if (!checked) {
                                prefs.setThemeColor("#2196f3")
                            }
                        }
                    ),
                    SettingTile.SwitchTile(
                        leading = {
                            SettingsTileIcon(
                                R.drawable.photo_24px
                            )
                        },
                        title = "Dynamic colors",
                        description = "Use wallpaper colors",
                        checked = prefs.useDynamicColor,
                        enabled = isAndroid12Plus && !prefs.isCustomTheme,
                        onCheckedChange = { checked ->
                            prefs.setDynamicColor(checked)
                        }
                    ),
                ),
            )
            SettingSection(
                title = "General",
                tiles = listOf(
                    SettingTile.DialogOptionTile(
                        leading = { SettingsTileIcon(R.drawable.home_filled_24px) },
                        title = "Starting default screen",
                        options = listOf(
                            "Home",
                            "Movies",
                            "TV series",
                        ),
                        selectedOption = prefs.defaultTab,
                        onOptionSelected = {
                            prefs.setDefaultTab(it)
                        }
                    )
                )
            )
            SettingSection(
                title = "Data",
                tiles = listOf(
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.upload_24px) },
                        title = "Export app data",
                        onClick = {
                            isExportDialogOpen = true
                        }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.download_24px) },
                        title = "Import app data",
                        onClick = {
                            isWarningImportDialogOpen = true
                        }
                    )
                )
            )
            SettingSection(
                title = "Trakt JSON Import",
                tiles = listOf(
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.schedule_24px) },
                        title = "Import watch history",
                        onClick = { watchedPicker.launch(arrayOf("*/*")) }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.hourglass_24px) },
                        title = "Import watchlist",
                        onClick = { watchlistPicker.launch(arrayOf("*/*")) }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.refresh_24px) },
                        title = "Fill in missing watched dates",
                        onClick = { datesPicker.launch(arrayOf("*/*")) }
                    )
                )
            )
            SettingSection(
                title = "Sync",
                tiles = listOfNotNull(
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.sync_24px) },
                        title = if (prefs.syncFolderUri != null) "Change sync folder" else "Choose sync folder",
                        onClick = {
                            syncFolderLauncher.launch(null)
                        }
                    ),
                    SettingTile.ActionTile(
                        leading = { SettingsTileIcon(R.drawable.sync_24px) },
                        title = "Sync now",
                        onClick = {
                            val folderUri = prefs.syncFolderUri
                            if (folderUri == null) {
                                SnackbarManager.show("Choose a sync folder first")
                            } else {
                                coroutineScope.launch {
                                    SyncManager.writeSnapshot(context, folderUri)
                                    val merged = SyncManager.checkAndMergeIfNeeded(context, folderUri)
                                    SnackbarManager.show(if (merged) "Synced new data" else "Already up to date")
                                }
                            }
                        }
                    ),
                    if (prefs.syncFolderUri != null) {
                        SettingTile.ActionTile(
                            leading = { SettingsTileIcon(R.drawable.sync_24px) },
                            title = "Remove sync folder",
                            onClick = { isRemoveSyncDialogOpen = true }
                        )
                    } else null
                )
            )
        }
    }


    DialogBasic(
        show = isExportDialogOpen,
        onConfirm = {
            val intent = createNewDocumentIntent()
            exportLauncher(intent)
        },
        onDismiss = {
            isExportDialogOpen = false
        },
        title = "Export data",
        confirmBtnDisabled = !exportWatchlistChecked && !exportMovieListChecked,
    ) {
        Column() {

            CheckboxRow(
                label = "Include lists",
                checked = exportMovieListChecked
            ) { exportMovieListChecked = it }

            Text(
                "Always make sure to create a data export after updating the app to a new version; there’s a high chance that older backups won’t work.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp)
            )
        }
    }

    TextAlertDialog(
        show = isWarningImportDialogOpen,
        onDismiss = {
            isWarningImportDialogOpen = false
        },
        title = "Import data",
        onConfirm = {
            val intent = createOpenDocumentIntent()
            importLauncher(intent)
        },
        message = "Importing data will replace the current data. Are you sure you want to continue?"
    )
    TextAlertDialog(
        show = datesPreview != null,
        onDismiss = {
            datesPreview = null
            pendingDatesJson = null
        },
        title = "Import watched dates",
        onConfirm = {
            val text = pendingDatesJson
            datesPreview = null
            pendingDatesJson = null
            if (text != null) {
                coroutineScope.launch {
                    try {
                        datesViewModel.run(text, dryRun = false)
                        SnackbarManager.show("Watched dates imported")
                    } catch (e: Exception) {
                        SnackbarManager.show("Import failed")
                    }
                }
            }
        },
        message = datesPreview ?: ""
    )
    TextAlertDialog(
        show = watchlistPreview != null,
        onDismiss = {
            watchlistPreview = null
            pendingWatchlistJson = null
        },
        title = "Import watchlist",
        onConfirm = {
            val text = pendingWatchlistJson
            watchlistPreview = null
            pendingWatchlistJson = null
            if (text != null) datesViewModel.importWatchlist(text)
        },
        message = watchlistPreview ?: ""
    )
    TextAlertDialog(
        show = watchedPreview != null,
        onDismiss = {
            watchedPreview = null
            pendingWatchedJson = null
        },
        title = "Import watch history",
        onConfirm = {
            val text = pendingWatchedJson
            watchedPreview = null
            pendingWatchedJson = null
            if (text != null) datesViewModel.importMissingWatched(text)
        },
        message = watchedPreview ?: ""
    )
    TextAlertDialog(
        show = isRemoveSyncDialogOpen,
        onDismiss = { isRemoveSyncDialogOpen = false },
        title = "Remove sync folder",
        onConfirm = {
            isRemoveSyncDialogOpen = false

            prefs.syncFolderUri?.let { uriString ->
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(uriString),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }

            prefs.setSyncFolderUri(null)
            SnackbarManager.show("Sync folder removed")
        },
        message = "Syncing will stop on this device. The files already in the folder are kept, and your data here isn't changed."
    )
    
    TextAlertDialog(
        show = datesViewModel.status != null,
        onDismiss = { if (!datesViewModel.running) datesViewModel.clearStatus() },
        title = "Importing",
        onConfirm = { if (!datesViewModel.running) datesViewModel.clearStatus() },
        message = datesViewModel.status ?: ""
    )
}

private fun readTexts(context: Context, uris: List<Uri>): List<String> =
    uris.mapNotNull { uri ->
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }
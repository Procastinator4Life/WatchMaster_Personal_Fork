package com.pranshulgg.watchmaster.feature.setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.room.withTransaction
import com.google.gson.Gson
import com.pranshulgg.watchmaster.core.ui.snackbar.SnackbarManager
import com.pranshulgg.watchmaster.data.local.WatchMasterDatabase
import com.pranshulgg.watchmaster.data.local.entity.CustomListEntity
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pranshulgg.watchmaster.core.prefs.AppPrefs
import com.pranshulgg.watchmaster.core.sync.SyncManager
import com.pranshulgg.watchmaster.data.local.entity.DeletedItemEntity
import com.pranshulgg.watchmaster.data.local.entity.DeletedListEntity
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import java.util.Date

import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException

private data class ExportData(
    val version: Int,
    val watchlist: List<WatchlistItemEntity>,
    val tvSeasons: List<SeasonEntity>,
    val movieLists: List<CustomListEntity>,
    val tvEpisodes: List<TvEpisodeEntity>? = null
)

@Composable
fun exportLauncher(
    context: Context,
    exportWatchlist: Boolean = false,
    exportMovieList: Boolean = false
): (Intent) -> Unit {
    val scope = rememberCoroutineScope()


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        uri?.let {
            scope.launch {
                export(context, uri, exportWatchlist, exportMovieList)
            }
        }
    }

    return { intent ->
        launcher.launch(intent)
    }
}

@Composable
fun importLauncher(context: Context): (Intent) -> Unit {
    val scope = rememberCoroutineScope()


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        uri?.let {
            scope.launch {
                import(context, uri)
            }
        }
    }

    return { intent ->
        launcher.launch(intent)
    }
}

private suspend fun export(
    context: Context,
    uri: Uri,
    exportWatchlist: Boolean = false,
    exportMovieList: Boolean = false
) {

    val db = WatchMasterDatabase.getInstance(context)
    val watchlistData = if (exportWatchlist) {
        db.watchlistDao().getAll().first()
    } else emptyList()
    val tvSeasons = if (exportWatchlist) {
        db.seasonDao().getAllSeasons().first()
    } else emptyList()
    val movieLists = if (exportMovieList) {
        db.movieListsDao().getAllCustomLists().first()
    } else emptyList()

    val tvEpisodes = if (exportWatchlist) {
        db.tvEpisodeDao().getAllEpisodes()
    } else emptyList()

    val json = Gson().toJson(ExportData(4, watchlistData, tvSeasons, movieLists, tvEpisodes))

    val file = context.contentResolver.openOutputStream(uri)

    withContext(Dispatchers.IO) {
        file?.write(json.toByteArray())
        file?.close()
    }

    SnackbarManager.show("Exported successfully")

}

private suspend fun import(context: Context, uri: Uri) {

    val db = WatchMasterDatabase.getInstance(context)

    // 1. Read the file
    val json = try {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    if (json.isNullOrBlank()) {
        SnackbarManager.show("Couldn't read that file")
        return
    }

    // 2. Make sure it's a backup and not some other JSON file
    val root = try {
        JsonParser.parseString(json)
    } catch (e: Exception) {
        null
    }
    val obj = if (root != null && root.isJsonObject) root.asJsonObject else null

    if (obj == null) {
        SnackbarManager.show("That isn't a WatchMaster backup")
        return
    }

    // Sync files are per-device snapshots, not backups
    if (obj.has("deviceId") || obj.has("deletedItems") || obj.has("customLists")) {
        SnackbarManager.show("That's a sync file, not a backup. Pick a file made with Export app data")
        return
    }

    if (!obj.has("version") || !obj.has("watchlist")) {
        SnackbarManager.show("That isn't a WatchMaster backup")
        return
    }

    val data = try {
        Gson().fromJson(json, ExportData::class.java)
    } catch (e: Exception) {
        null
    }

    if (data == null) {
        SnackbarManager.show("That isn't a WatchMaster backup")
        return
    }

    // Older files may be missing a list, so read each one as optional
    val watchlistData: List<WatchlistItemEntity>? = data.watchlist
    val seasonData: List<SeasonEntity>? = data.tvSeasons
    val listData: List<CustomListEntity>? = data.movieLists
    val importedEpisodes = data.tvEpisodes.orEmpty()

    if (watchlistData.isNullOrEmpty() && listData.isNullOrEmpty()) {
        SnackbarManager.show("No data to import")
        return
    }

    val unsupportedVersions = listOf(0, 1, 2, 3)

    if (unsupportedVersions.contains(data.version)) {
        SnackbarManager.show("Unsupported version")
        return
    }

    // Imported shows, movies, seasons and lists get a fresh timestamp so they beat
    // anything older in the sync folder
    val now = System.currentTimeMillis()

    try {
        db.withTransaction {
            if (!watchlistData.isNullOrEmpty()) {
                val importedIds = watchlistData.map { it.id }.toSet()
                val existingIds = db.watchlistDao().getAllOnce().map { it.id }
                val removedIds = existingIds.filter { it !in importedIds }

                // Anything not in the backup is marked deleted, so sync can't bring it back
                removedIds.forEach { id ->
                    db.deletedItemDao().insert(DeletedItemEntity(id = id, deletedAt = now))
                }

                // A backup that carries episodes replaces every stored episode.
                // An older backup without them keeps the episodes already on the phone.
                val episodeOwners = if (importedEpisodes.isNotEmpty()) existingIds else removedIds
                episodeOwners.forEach { db.tvEpisodeDao().deleteEpisodesForShow(it) }

                db.watchlistDao().clearAll()
                db.seasonDao().clearAll()
                db.watchlistDao().insertAll(watchlistData.map { it.copy(lastModifiedAt = now) })
                db.seasonDao().insertSeasons(seasonData.orEmpty().map { it.copy(lastModifiedAt = now) })

                if (importedEpisodes.isNotEmpty()) {
                    db.tvEpisodeDao().insertEpisodes(importedEpisodes.map { it.copy(epId = 0L) })
                }
            }

            if (!listData.isNullOrEmpty()) {
                val importedKeys = listData
                    .map { it.syncId.orEmpty().ifBlank { "legacy-${it.id}" } }
                    .toSet()

                db.movieListsDao().getAllCustomLists().first()
                    .map { it.syncId.orEmpty().ifBlank { "legacy-${it.id}" } }
                    .filter { it !in importedKeys }
                    .forEach { key ->
                        db.deletedListDao().insert(DeletedListEntity(syncId = key, deletedAt = now))
                    }

                db.movieListsDao().clearAll()
                db.movieListsDao().insertAll(listData.map { it.copy(lastModifiedAt = now) })
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // The whole transaction is rolled back, so the old data is still there
        SnackbarManager.show("Import failed. Nothing was changed")
        return
    }

    // Put the imported state in the sync folder straight away
    AppPrefs.getSyncFolderUri()?.let { folderUri ->
        runCatching { SyncManager.writeSnapshot(context, folderUri) }
    }

    SnackbarManager.show("Imported successfully")
}

fun createNewDocumentIntent(): Intent {
    val randomId = (100000..999999).random()
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/json"
        putExtra(Intent.EXTRA_TITLE, "watchmaster_backup-${randomId}-v4.json")
    }

    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    return intent
}


fun createOpenDocumentIntent(): Intent {

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/json"
    }

    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION


    return intent
}

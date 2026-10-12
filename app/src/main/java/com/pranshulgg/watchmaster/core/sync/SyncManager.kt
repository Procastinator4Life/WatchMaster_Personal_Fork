package com.pranshulgg.watchmaster.core.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.google.gson.Gson
import com.pranshulgg.watchmaster.core.prefs.AppPrefs
import com.pranshulgg.watchmaster.data.local.WatchMasterDatabase
import com.pranshulgg.watchmaster.data.local.entity.CustomListEntity
import com.pranshulgg.watchmaster.data.local.entity.DeletedItemEntity
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.data.local.entity.DeletedListEntity
import kotlinx.coroutines.flow.first
import java.util.UUID

private const val SYNC_FILE_PREFIX = "watchmaster_sync_"
private const val LEGACY_SYNC_FILE_NAME = "watchmaster_sync.json"
private const val SYNC_VERSION = 4

private data class SyncData(
    val version: Int = SYNC_VERSION,
    val deviceId: String? = null,
    val watchlist: List<WatchlistItemEntity> = emptyList(),
    val tvSeasons: List<SeasonEntity> = emptyList(),
    val tvEpisodes: List<TvEpisodeEntity> = emptyList(),
    val customLists: List<CustomListEntity> = emptyList(),
    val deletedItems: List<DeletedItemEntity> = emptyList(),
    val deletedLists: List<DeletedListEntity> = emptyList()
)

/**
 * File-based sync designed for Syncthing.
 *
 * - Each device owns its own snapshot file.
 * - Every sync reads ALL snapshots in the folder and merges them into Room.
 * - Records are last-writer-wins using lastModifiedAt.
 * - Deletions are synced with "tombstones" (DeletedItemEntity). A show/movie is
 *   deleted everywhere only if its deletion time is NEWER than its last edit.
 *   If it was edited/re-added after being deleted, it is kept.
 */
object SyncManager {

    private val gson = Gson()

    suspend fun writeSnapshot(context: Context, folderUriString: String) {
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUriString)) ?: return
        val db = WatchMasterDatabase.getInstance(context)

        val snapshot = SyncData(
            version = SYNC_VERSION,
            deviceId = AppPrefs.getSyncDeviceId(),
            watchlist = db.watchlistDao().getAllOnce(),
            tvSeasons = db.seasonDao().getAllSeasons().first(),
            tvEpisodes = db.tvEpisodeDao().getAllEpisodes(),
            customLists = db.movieListsDao().getAllCustomLists().first(),
            deletedItems = db.deletedItemDao().getAll(),
            deletedLists = db.deletedListDao().getAll()
        )

        val json = gson.toJson(snapshot)
        val fileName = "$SYNC_FILE_PREFIX${safeDeviceId(AppPrefs.getSyncDeviceId())}.json"
        val file = folder.findFile(fileName)
            ?: folder.createFile("application/json", fileName)
            ?: return

        context.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }

        AppPrefs.setLastWrittenHash(json.hashCode().toString())
    }

    suspend fun checkAndMergeIfNeeded(context: Context, folderUriString: String): Boolean {
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUriString)) ?: return false

        val snapshots = folder.listFiles()
            .filter { file ->
                val name = file.name ?: return@filter false
                name == LEGACY_SYNC_FILE_NAME ||
                    (name.startsWith(SYNC_FILE_PREFIX) && name.endsWith(".json"))
            }
            .mapNotNull { file ->
                readSnapshot(context, file)
            }

        if (snapshots.isEmpty()) return false

        val db = WatchMasterDatabase.getInstance(context)
        val localDeviceId = AppPrefs.getSyncDeviceId()
        var changed = false

        db.withTransaction {
            val deletedDao = db.deletedItemDao()

            // ---------------------------------------------------------------
            // 1. DELETIONS: decide which shows/movies are deleted
            // ---------------------------------------------------------------
            val localWatchlist = db.watchlistDao().getAllOnce().associateBy { it.id }
            val remoteWatchlist = mergeRecords(
                snapshots.flatMap { it.watchlist.map { item -> item to (it.deviceId ?: "") } }
            )
            val remoteLastModified = remoteWatchlist.associate {
                it.first.id to it.first.lastModifiedAt
            }

            // Latest deletion time per id, from this device + all snapshots
            val tombstones = (deletedDao.getAll() + snapshots.flatMap { it.deletedItems.orEmpty() })
                .groupBy { it.id }
                .mapValues { (_, notes) -> notes.maxOf { it.deletedAt } }

            val deletedShowIds = mutableSetOf<Long>()
            tombstones.forEach { (id, deletedAt) ->
                val newestEdit = maxOf(
                    localWatchlist[id]?.lastModifiedAt ?: 0L,
                    remoteLastModified[id] ?: 0L
                )
                // Deleted AFTER the last edit -> it stays deleted.
                // Edited/re-added after the deletion -> keep it.
                if (deletedAt > newestEdit) {
                    deletedShowIds += id
                    if (localWatchlist.containsKey(id)) {
                        deletedDao.deleteEpisodesOfShow(id)
                        deletedDao.deleteSeasonsOfShow(id)
                        deletedDao.deleteWatchlistItem(id)
                        changed = true
                    }
                    deletedDao.insert(DeletedItemEntity(id = id, deletedAt = deletedAt))
                }
            }

            // ---------------------------------------------------------------
            // 2. Watchlist (shows + movies)
            // ---------------------------------------------------------------
            remoteWatchlist.forEach { (remote, sourceDevice) ->
                if (remote.id in deletedShowIds) return@forEach
                val local = localWatchlist[remote.id]
                if (isNewer(remote.lastModifiedAt, local?.lastModifiedAt, sourceDevice, localDeviceId)) {
                    db.watchlistDao().insert(remote)
                    changed = true
                }
            }

            // ---------------------------------------------------------------
            // 3. Seasons (only for shows that exist and aren't deleted)
            // ---------------------------------------------------------------
            val existingShowIds = db.watchlistDao().getAllOnce().map { it.id }.toSet()
            val refreshedLocalSeasons = db.seasonDao().getAllSeasons().first()
                .associateBy { it.showId to it.seasonNumber }
            val remoteSeasons = mergeRecords(
                snapshots.flatMap { it.tvSeasons.map { season -> season to (it.deviceId ?: "") } }
            )
            remoteSeasons.forEach { (remote, sourceDevice) ->
                if (remote.showId in deletedShowIds) return@forEach
                val local = refreshedLocalSeasons[remote.showId to remote.seasonNumber]
                if (remote.showId in existingShowIds &&
                    isNewer(remote.lastModifiedAt, local?.lastModifiedAt, sourceDevice, localDeviceId)
                ) {
                    db.seasonDao().insertSeason(remote)
                    changed = true
                }
            }

            // ---------------------------------------------------------------
            // 4. Episodes
            // ---------------------------------------------------------------
            val remoteEpisodes = mergeRecords(
                snapshots.flatMap { it.tvEpisodes.map { episode -> episode to (it.deviceId ?: "") } }
            )
            remoteEpisodes.forEach { (remote, sourceDevice) ->
                if (remote.showId in deletedShowIds) return@forEach
                val local = db.tvEpisodeDao().getEpisode(
                    remote.showId,
                    remote.seasonNumber,
                    remote.episode_number
                )
                if (isNewer(remote.lastModifiedAt, local?.lastModifiedAt, sourceDevice, localDeviceId)) {
                    if (local == null) {
                        db.tvEpisodeDao().insertEpisodes(listOf(remote.copy(epId = 0L)))
                    } else {
                        db.tvEpisodeDao().updateSyncedEpisode(
                            showId = remote.showId,
                            seasonNumber = remote.seasonNumber,
                            episodeNumber = remote.episode_number,
                            airDate = remote.air_date,
                            name = remote.name,
                            overview = remote.overview,
                            stillPath = remote.still_path,
                            runtime = remote.runtime,
                            watched = remote.isWatched,
                            lastWatchedAt = remote.lastWatchedAt,
                            lastModifiedAt = remote.lastModifiedAt
                        )
                    }
                    changed = true
                }
            }

            // ---------------------------------------------------------------
            // 5. Custom lists (with deletion support)
            // ---------------------------------------------------------------
            val deletedListDao = db.deletedListDao()
            val refreshedLocalLists = db.movieListsDao().getAllCustomLists().first()
                .associateBy { it.syncId }
            val remoteLists = mergeRecords(
                snapshots.flatMap { it.customLists.map { list -> list to (it.deviceId ?: "") } }
            )
            val remoteListModified = remoteLists.associate { (list, _) ->
                list.syncId.orEmpty().ifBlank { "legacy-${list.id}" } to list.lastModifiedAt
            }

            val listTombstones = (deletedListDao.getAll() + snapshots.flatMap { it.deletedLists.orEmpty() })
                .groupBy { it.syncId }
                .mapValues { (_, notes) -> notes.maxOf { it.deletedAt } }

            val deletedListIds = mutableSetOf<String>()
            listTombstones.forEach { (syncId, deletedAt) ->
                val newestEdit = maxOf(
                    refreshedLocalLists[syncId]?.lastModifiedAt ?: 0L,
                    remoteListModified[syncId] ?: 0L
                )
                if (deletedAt > newestEdit) {
                    deletedListIds += syncId
                    if (refreshedLocalLists.containsKey(syncId)) {
                        deletedListDao.deleteBySyncId(syncId)
                        changed = true
                    }
                    deletedListDao.insert(DeletedListEntity(syncId = syncId, deletedAt = deletedAt))
                }
            }

            remoteLists.forEach { (rawRemote, sourceDevice) ->
                val remote = rawRemote.copy(
                    syncId = rawRemote.syncId.orEmpty().ifBlank { "legacy-${rawRemote.id}" }
                )
                if (remote.syncId in deletedListIds) return@forEach

                val local = refreshedLocalLists[remote.syncId]
                    ?: refreshedLocalLists.values.firstOrNull {
                        it.id == remote.id
                    }

                if (isNewer(remote.lastModifiedAt, local?.lastModifiedAt, sourceDevice, localDeviceId)) {
                    if (local == null) {
                        db.movieListsDao().insertCustomListItem(remote.copy(id = 0L))
                    } else {
                        db.movieListsDao().insertCustomListItem(remote.copy(id = local.id))
                    }
                    changed = true
                }
            }
        }

        writeSnapshot(context, folderUriString)
        AppPrefs.setLastSyncTimestamp(System.currentTimeMillis())
        return changed
    }

    private fun <T> mergeRecords(records: List<Pair<T, String>>): List<Pair<T, String>>
        where T : Any {
        return records
            .groupBy { (record, _) ->
                when (record) {
                    is WatchlistItemEntity -> "watch:${record.id}"
                    is SeasonEntity -> "season:${record.showId}:${record.seasonNumber}"
                    is TvEpisodeEntity -> "episode:${record.showId}:${record.seasonNumber}:${record.episode_number}"
                    is CustomListEntity -> "list:${record.syncId.orEmpty().ifBlank { "legacy-${record.id}" }}"
                    else -> record.toString()
                }
            }
            .mapNotNull { (_, values) ->
                values.maxWithOrNull(
                    compareBy<Pair<T, String>>(
                        { lastModifiedAt(it.first) },
                        { it.second }
                    )
                )
            }
    }

    private fun lastModifiedAt(record: Any): Long = when (record) {
        is WatchlistItemEntity -> record.lastModifiedAt
        is SeasonEntity -> record.lastModifiedAt
        is TvEpisodeEntity -> record.lastModifiedAt
        is CustomListEntity -> record.lastModifiedAt
        else -> 0L
    }

    private fun isNewer(
        remoteModifiedAt: Long,
        localModifiedAt: Long?,
        sourceDevice: String,
        localDeviceId: String
    ): Boolean {
        if (localModifiedAt == null) return true
        if (remoteModifiedAt != localModifiedAt) return remoteModifiedAt > localModifiedAt
        return sourceDevice > localDeviceId
    }

    private fun readSnapshot(context: Context, file: DocumentFile): SyncData? {
        val json = runCatching {
            context.contentResolver.openInputStream(file.uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
        }.getOrNull() ?: return null

        return runCatching {
            gson.fromJson(json, SyncData::class.java)
        }.getOrNull()
    }

    private fun safeDeviceId(id: String): String =
        id.replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(64)
            .ifBlank { UUID.randomUUID().toString() }
}
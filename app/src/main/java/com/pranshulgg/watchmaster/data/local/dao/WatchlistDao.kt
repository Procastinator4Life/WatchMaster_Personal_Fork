package com.pranshulgg.watchmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.core.model.WatchStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import com.pranshulgg.watchmaster.data.local.entity.DeletedItemEntity

@Dao
interface WatchlistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistItemEntity)

    @Query(
        """
        SELECT * FROM watchlist
        ORDER BY
            CASE WHEN status = 'FINISHED' THEN finishedDate END DESC,
            CASE WHEN status = 'WANT_TO_WATCH' THEN title END COLLATE NOCASE ASC
        """
    )
    fun getAll(): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist")
    suspend fun getAllOnce(): List<WatchlistItemEntity>

    @Query(
        """
        UPDATE watchlist
        SET status = :status,
            startedDate = COALESCE(:started, startedDate),
            finishedDate = COALESCE(:finished, finishedDate),
            interruptedAt = COALESCE(:interruptedAt, interruptedAt)
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: Long,
        status: WatchStatus,
        started: Instant? = null,
        finished: Instant? = null,
        interruptedAt: Instant? = null
    )

    @Query("""
        UPDATE watchlist SET
            title = :title,
            overview = :overview,
            posterPath = :posterPath,
            backdropPath = :backdropPath,
            genreIds = :genreIds,
            releaseDate = :releaseDate,
            avgRating = :avgRating
        WHERE id = :id
    """)
    suspend fun updateMetadata(
        id: Long,
        title: String,
        overview: String?,
        posterPath: String?,
        backdropPath: String?,
        genreIds: List<Int>?,
        releaseDate: String?,
        avgRating: Double?
    )

    @Query("UPDATE watchlist SET lastModifiedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: Long, timestamp: Long)
    
    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM watchlist WHERE status = 'FINISHED'")
    suspend fun deleteFinished()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletion(item: DeletedItemEntity)

    // Leaves a deletion note for every finished item, before deleteFinished() runs
    @Query("INSERT OR REPLACE INTO deleted_items (id, deletedAt) SELECT id, :deletedAt FROM watchlist WHERE status = 'FINISHED'")
    suspend fun recordFinishedDeletions(deletedAt: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id)")
    suspend fun exists(id: Long): Boolean

    @Query("UPDATE watchlist SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE watchlist SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: Long, isPinned: Boolean)

    @Query("UPDATE watchlist SET userRating = :rating WHERE id = :id")
    suspend fun updateUserRating(id: Long, rating: Double)

    @Query("UPDATE watchlist SET userRating = NULL WHERE id = :id")
    suspend fun clearUserRating(id: Long)

    @Query("SELECT * FROM watchlist WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<WatchlistItemEntity?>

    @Query("UPDATE watchlist SET notes = :note WHERE id = :id")
    suspend fun setUserNote(id: Long, note: String)

    @Query("DELETE FROM watchlist")
    suspend fun clearAll()

    @Query("SELECT status FROM watchlist WHERE id = :id LIMIT 1")
    suspend fun getStatus(id: Long): WatchStatus?

    suspend fun insertAll(items: List<WatchlistItemEntity>) {
        items.forEach { insert(it) }
    }

    @Query("UPDATE watchlist SET finishedDate = :finished WHERE id = :id")
    suspend fun updateFinishedDate(id: Long, finished: Instant?)

    @Query("UPDATE watchlist SET isDropped = :isDropped WHERE id = :id")
    suspend fun setDropped(id: Long, isDropped: Boolean)

    @Query("UPDATE watchlist SET backdropOverride = :backdropOverride WHERE id = :id")
    suspend fun updateBackdropOverride(id: Long, backdropOverride: String?)

}

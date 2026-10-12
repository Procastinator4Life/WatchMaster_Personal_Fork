package com.pranshulgg.watchmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pranshulgg.watchmaster.core.model.WatchStatus
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SeasonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(seasons: List<SeasonEntity>)

    @Query(
        """
        SELECT * FROM tv_seasons
        WHERE showId = :showId
        ORDER BY seasonNumber
        """
    )
    fun getSeasonForShow(showId: Long): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM tv_seasons WHERE showId = :showId")
    suspend fun getSeasonsForShowSync(showId: Long): List<SeasonEntity>

    @Query(
        """
        SELECT * FROM tv_seasons
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        LIMIT 1
        """
    )
    suspend fun getSeason(
        showId: Long,
        seasonNumber: Int
    ): SeasonEntity?

    @Query(
        """
        UPDATE tv_seasons
        SET status = :status,
            seasonStartedDate = COALESCE(:started, seasonStartedDate),
            seasonFinishedDate = COALESCE(:finished, seasonFinishedDate),
            seasonInterruptedAt = COALESCE(:interruptedAt, seasonInterruptedAt)
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        """
    )
    suspend fun updateSeasonStatus(
        showId: Long,
        seasonNumber: Int,
        status: WatchStatus,
        started: Instant? = null,
        finished: Instant? = null,
        interruptedAt: Instant? = null
    )

    @Query(
        """
        UPDATE tv_seasons
        SET seasonUserRating = :rating
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        """
    )
    suspend fun updateSeasonUserRating(
        showId: Long,
        seasonNumber: Int,
        rating: Double
    )

    @Query("""
        UPDATE tv_seasons
        SET seasonUserRating = NULL
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
    """)
    suspend fun clearSeasonUserRating(
        showId: Long,
        seasonNumber: Int
    )

    @Query(
        """
        UPDATE tv_seasons
        SET seasonNotes = :note
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        """
    )
    suspend fun setSeasonUserNote(
        showId: Long,
        seasonNumber: Int,
        note: String
    )

    @Query("SELECT * FROM tv_seasons")
    fun getAllSeasons(): Flow<List<SeasonEntity>>

    @Query(
        """
        UPDATE tv_seasons
        SET seasonFinishedDate = :finished
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        """
    )
    suspend fun updateSeasonFinishedDate(
        showId: Long,
        seasonNumber: Int,
        finished: Instant?
    )

    @Query(
        """
        UPDATE tv_seasons
        SET lastModifiedAt = :timestamp
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        """
    )
    suspend fun touchSeason(showId: Long, seasonNumber: Int, timestamp: Long)
    
    @Query("DELETE FROM tv_seasons")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeason(season: SeasonEntity)

    @Query(
        """
        UPDATE tv_seasons
        SET lastEpWatched = :epNumber
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        """
    )
    suspend fun updateLastEpWatched(
        showId: Long,
        seasonNumber: Int,
        epNumber: Int?
    )

    @Query(
        """
        DELETE FROM tv_seasons
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        """
    )
    suspend fun deleteSeason(
        showId: Long,
        seasonNumber: Int
    )
}
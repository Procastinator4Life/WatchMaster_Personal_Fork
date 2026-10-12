package com.pranshulgg.watchmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TvEpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<TvEpisodeEntity>)

    @Query("""
        UPDATE tv_episodes
        SET isWatched = :watched,
            lastWatchedAt = CASE WHEN :watched = 1 THEN :timestamp ELSE 0 END,
            lastModifiedAt = :modifiedAt
        WHERE epId = :epId
    """)
    suspend fun updateEpisodeStatus(
        epId: Long,
        watched: Boolean,
        timestamp: Long = if (watched) System.currentTimeMillis() else 0L,
        modifiedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE tv_episodes
        SET isWatched = 1,
            lastWatchedAt = :timestamp,
            lastModifiedAt = :modifiedAt
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
    """)
    suspend fun markAllEpWatched(
        showId: Long,
        seasonNumber: Int,
        timestamp: Long = System.currentTimeMillis(),
        modifiedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE tv_episodes
        SET isWatched = 0,
            lastWatchedAt = 0,
            lastModifiedAt = :modifiedAt
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
    """)
    suspend fun markAllEpUnWatched(
        showId: Long,
        seasonNumber: Int,
        modifiedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE tv_episodes
        SET lastWatchedAt = :timestamp,
            lastModifiedAt = :modifiedAt
        WHERE epId = :epId
    """)
    suspend fun updateEpisodeWatchedDate(
        epId: Long,
        timestamp: Long,
        modifiedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE tv_episodes
        SET isWatched = :watched,
            lastWatchedAt = CASE WHEN :watched = 1 THEN :timestamp ELSE 0 END,
            lastModifiedAt = :modifiedAt
        WHERE epId = :epId
    """)
    suspend fun updateEpisodeWatchedDateAndStatus(
        epId: Long,
        watched: Boolean,
        timestamp: Long,
        modifiedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE tv_episodes
        SET air_date = :airDate,
            name = :name,
            overview = :overview,
            still_path = :stillPath,
            runtime = :runtime,
            isWatched = :watched,
            lastWatchedAt = :lastWatchedAt,
            lastModifiedAt = :lastModifiedAt
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        AND episode_number = :episodeNumber
    """)
    suspend fun updateSyncedEpisode(
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int,
        airDate: String?,
        name: String,
        overview: String?,
        stillPath: String?,
        runtime: Int?,
        watched: Boolean,
        lastWatchedAt: Long,
        lastModifiedAt: Long
    )

    @Query("""
        SELECT *
        FROM tv_episodes
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        ORDER BY episode_number
    """)
    fun getEpisodesForSeason(
        showId: Long,
        seasonNumber: Int
    ): Flow<List<TvEpisodeEntity>>

    @Query("""
        SELECT *
        FROM tv_episodes
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        ORDER BY episode_number
    """)
    suspend fun getEpisodesForSeasonOnce(
        showId: Long,
        seasonNumber: Int
    ): List<TvEpisodeEntity>


    @Query("""
        SELECT * FROM tv_episodes
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        AND episode_number = :episodeNumber
        LIMIT 1
    """)
    suspend fun getEpisode(
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int
    ): TvEpisodeEntity?

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM tv_episodes
            WHERE showId = :showId
            AND seasonNumber = :seasonNumber
        )
    """)
    suspend fun hasEpisodes(
        showId: Long,
        seasonNumber: Int
    ): Boolean

    @Query("""
        SELECT COUNT(*)
        FROM tv_episodes
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        AND isWatched = 0
    """)
    suspend fun getUnwatchedCount(
        showId: Long,
        seasonNumber: Int
    ): Int

    @Query("""
        SELECT *
        FROM tv_episodes
        ORDER BY showId, seasonNumber, episode_number
    """)
    fun observeAllEpisodes(): Flow<List<TvEpisodeEntity>>

    @Query("""
        SELECT *
        FROM tv_episodes
    """)
    suspend fun getAllEpisodes(): List<TvEpisodeEntity>

    @Query("""
        UPDATE tv_episodes
        SET isWatched = 1,
            lastWatchedAt = :timestamp,
            lastModifiedAt = :modifiedAt
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        AND episode_number BETWEEN 1 AND :count
    """)
    suspend fun markEpWatchedFromCount(
        showId: Long,
        seasonNumber: Int,
        count: Int,
        timestamp: Long = System.currentTimeMillis(),
        modifiedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        SELECT air_date
        FROM tv_episodes
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
        AND episode_number = 1
        LIMIT 1
    """)
    suspend fun getFirstEpisodeAirDate(
        showId: Long,
        seasonNumber: Int
    ): String?

    @Query("""
        DELETE FROM tv_episodes
        WHERE showId = :showId
        AND seasonNumber = :seasonNumber
    """)
    suspend fun deleteEpisodesForSeason(
        showId: Long,
        seasonNumber: Int
    )

    @Query("""
        DELETE FROM tv_episodes
        WHERE showId = :showId
    """)
    suspend fun deleteEpisodesForShow(showId: Long)

    @Query("DELETE FROM tv_episodes")
    suspend fun clearAll()
}
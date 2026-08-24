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

    // Updated to accept timestamp
    @Query("UPDATE tv_episodes SET isWatched = :watched, lastWatchedAt = :timestamp WHERE epId = :epId")
    suspend fun updateEpisodeStatus(epId: Long, watched: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tv_episodes SET isWatched = 1, lastWatchedAt = :timestamp WHERE seasonId = :seasonId")
    suspend fun markAllEpWatched(seasonId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tv_episodes SET isWatched = 0, lastWatchedAt = 0 WHERE seasonId = :seasonId")
    suspend fun markAllEpUnWatched(seasonId: Long)

    @Query("SELECT * FROM tv_episodes WHERE seasonId = :seasonId ORDER BY episode_number")
    fun getEpisodesForSeason(seasonId: Long): Flow<List<TvEpisodeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM tv_episodes WHERE seasonId = :seasonId)")
    suspend fun hasEpisodes(seasonId: Long): Boolean

    @Query("SELECT * FROM tv_episodes")
    suspend fun getAllEpisodes(): List<TvEpisodeEntity>

    @Query(
        """
            UPDATE tv_episodes 
            SET isWatched = 1, lastWatchedAt = :timestamp 
            WHERE seasonId = :seasonId 
            AND episode_number BETWEEN 1 AND :count
            """
    )
    suspend fun markEpWatchedFromCount(seasonId: Long, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM tv_episodes")
    suspend fun clearAll()
}
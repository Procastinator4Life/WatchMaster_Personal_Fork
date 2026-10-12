package com.pranshulgg.watchmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pranshulgg.watchmaster.data.local.entity.DeletedItemEntity

@Dao
interface DeletedItemDao {

    @Query("SELECT * FROM deleted_items")
    suspend fun getAll(): List<DeletedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DeletedItemEntity)

    // Used by sync when another device deleted a show/movie
    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteWatchlistItem(id: Long)

    @Query("DELETE FROM tv_seasons WHERE showId = :id")
    suspend fun deleteSeasonsOfShow(id: Long)

    @Query("DELETE FROM tv_episodes WHERE showId = :id")
    suspend fun deleteEpisodesOfShow(id: Long)
}
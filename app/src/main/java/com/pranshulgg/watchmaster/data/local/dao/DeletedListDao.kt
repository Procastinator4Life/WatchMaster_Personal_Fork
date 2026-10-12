package com.pranshulgg.watchmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pranshulgg.watchmaster.data.local.entity.DeletedListEntity

@Dao
interface DeletedListDao {

    @Query("SELECT * FROM deleted_lists")
    suspend fun getAll(): List<DeletedListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DeletedListEntity)

    // Used by sync when another device deleted a list
    @Query("DELETE FROM custom_lists WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
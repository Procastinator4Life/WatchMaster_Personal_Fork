package com.pranshulgg.watchmaster.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A note saying "this custom list was deleted at this time". Used by sync. */
@Entity(tableName = "deleted_lists")
data class DeletedListEntity(
    @PrimaryKey val syncId: String,
    val deletedAt: Long
)
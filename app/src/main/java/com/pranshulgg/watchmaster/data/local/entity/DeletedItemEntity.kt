package com.pranshulgg.watchmaster.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A note saying "this show/movie was deleted at this time". Used by sync. */
@Entity(tableName = "deleted_items")
data class DeletedItemEntity(
    @PrimaryKey val id: Long,
    val deletedAt: Long
)
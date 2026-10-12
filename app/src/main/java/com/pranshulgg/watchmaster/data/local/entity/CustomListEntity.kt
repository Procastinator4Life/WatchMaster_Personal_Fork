package com.pranshulgg.watchmaster.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pranshulgg.watchmaster.core.model.MediaListsIcons
import java.util.UUID

@Entity(tableName = "custom_lists")
data class CustomListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Stable identity used for cross-device sync; Room id may differ on independently-created lists. */
    val syncId: String = UUID.randomUUID().toString(),

    val name: String,
    val description: String = "",

    val icon: MediaListsIcons? = null,

    val ids: List<Long>,

    val isPinned: Boolean = false,

    val lastModifiedAt: Long = System.currentTimeMillis()
)
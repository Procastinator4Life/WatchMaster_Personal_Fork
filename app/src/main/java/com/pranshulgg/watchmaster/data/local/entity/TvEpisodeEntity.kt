package com.pranshulgg.watchmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tv_episodes",
    indices = [
        Index(value = ["showId", "seasonNumber"])
    ]
)
data class TvEpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val epId: Long = 0,

    val showId: Long,
    val seasonNumber: Int,

    val air_date: String?,
    val episode_number: Int,
    val name: String,
    val overview: String?,
    val still_path: String?,
    val runtime: Int?,

    val isWatched: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val lastWatchedAt: Long = 0L,

    /** Changes whenever the watched state changes; unlike lastWatchedAt this also tracks unwatching. */
    @ColumnInfo(defaultValue = "0")
    val lastModifiedAt: Long = System.currentTimeMillis()
)
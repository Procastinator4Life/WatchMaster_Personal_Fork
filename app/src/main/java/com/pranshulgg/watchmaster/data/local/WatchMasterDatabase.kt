package com.pranshulgg.watchmaster.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pranshulgg.watchmaster.data.local.dao.WatchlistDao
import com.pranshulgg.watchmaster.data.local.entity.WatchlistItemEntity
import com.pranshulgg.watchmaster.data.local.converters.GenreIdsConverter
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pranshulgg.watchmaster.data.local.converters.InstantConverters
import com.pranshulgg.watchmaster.data.local.converters.LongListConverter
import com.pranshulgg.watchmaster.data.local.converters.MediaListsIconConverter
import com.pranshulgg.watchmaster.data.local.converters.SeasonNameConverter
import com.pranshulgg.watchmaster.data.local.converters.WatchStatusConverter
import com.pranshulgg.watchmaster.data.local.dao.CustomListsDao
import com.pranshulgg.watchmaster.data.local.dao.MovieBundleDao
import com.pranshulgg.watchmaster.data.local.dao.SeasonDao
import com.pranshulgg.watchmaster.data.local.dao.TrendingDao
import com.pranshulgg.watchmaster.data.local.dao.TvBundleDao
import com.pranshulgg.watchmaster.data.local.dao.TvEpisodeDao
import com.pranshulgg.watchmaster.data.local.entity.CustomListEntity
import com.pranshulgg.watchmaster.data.local.entity.MovieBundleEntity
import com.pranshulgg.watchmaster.data.local.entity.SeasonEntity
import com.pranshulgg.watchmaster.data.local.entity.TrendingEntity
import com.pranshulgg.watchmaster.data.local.entity.TvBundleEntity
import com.pranshulgg.watchmaster.data.local.entity.TvEpisodeEntity
import com.pranshulgg.watchmaster.data.local.dao.DeletedItemDao
import com.pranshulgg.watchmaster.data.local.entity.DeletedItemEntity
import com.pranshulgg.watchmaster.data.local.dao.DeletedListDao
import com.pranshulgg.watchmaster.data.local.entity.DeletedListEntity

@Database(
    entities = [WatchlistItemEntity::class, MovieBundleEntity::class, TvBundleEntity::class, SeasonEntity::class, TvEpisodeEntity::class, CustomListEntity::class, TrendingEntity::class, DeletedItemEntity::class, DeletedListEntity::class],
    version = 44
)
@TypeConverters(
    GenreIdsConverter::class,
    InstantConverters::class,
    WatchStatusConverter::class,
    SeasonNameConverter::class,
    LongListConverter::class,
    MediaListsIconConverter::class
)
abstract class WatchMasterDatabase : RoomDatabase() {

    abstract fun watchlistDao(): WatchlistDao
    abstract fun movieBundleDao(): MovieBundleDao

    abstract fun tvBundleDao(): TvBundleDao

    abstract fun seasonDao(): SeasonDao

    abstract fun tvEpisodeDao(): TvEpisodeDao

    abstract fun movieListsDao(): CustomListsDao

    abstract fun trendingDao(): TrendingDao

    abstract fun deletedItemDao(): DeletedItemDao

    abstract fun deletedListDao(): DeletedListDao

    companion object {
        @Volatile
        private var INSTANCE: WatchMasterDatabase? = null

        fun getInstance(context: Context): WatchMasterDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WatchMasterDatabase::class.java,
                    "watchmaster.db"
                ).addMigrations(
                    MIGRATION_25_26,
                    MIGRATION_26_27,
                    MIGRATION_27_28,
                    MIGRATION_28_29,
                    MIGRATION_29_30,
                    MIGRATION_30_31,
                    MIGRATION_31_32,
                    MIGRATION_32_33,
                    MIGRATION_33_34,
                    MIGRATION_34_35,
                    MIGRATION_35_36,
                    MIGRATION_36_37,
                    MIGRATION_37_38,
                    MIGRATION_38_39,
                    MIGRATION_39_40,
                    MIGRATION_40_41,
                    MIGRATION_41_42,
                    MIGRATION_42_43,
                    MIGRATION_43_44
                )                
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS movie_lists (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                icon INTEGER,
                movieIds TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent()
        )
    }
}


val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE movie_lists_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                icon INTEGER,
                movieIds TEXT NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO movie_lists_new (id, name, description, icon, movieIds)
            SELECT id, name, 
                   COALESCE(description, ''), 
                   icon, 
                   movieIds
            FROM movie_lists
        """.trimIndent()
        )

        db.execSQL("DROP TABLE movie_lists")
        db.execSQL("ALTER TABLE movie_lists_new RENAME TO movie_lists")
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE movie_lists_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                icon TEXT,
                movieIds TEXT NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO movie_lists_new (id, name, description, icon, movieIds)
            SELECT id, name, 
                   description, 
                   icon, 
                   movieIds
            FROM movie_lists
        """.trimIndent()
        )

        db.execSQL("DROP TABLE movie_lists")
        db.execSQL("ALTER TABLE movie_lists_new RENAME TO movie_lists")
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE movie_lists ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0
        """.trimIndent()
        )
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE tv_seasons ADD COLUMN cachedAt INTEGER NOT NULL DEFAULT 0
        """.trimIndent()
        )
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE movie_lists RENAME TO custom_lists
        """.trimIndent()
        )
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE custom_lists_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                icon TEXT,
                ids TEXT NOT NULL,
                isPinned INTEGER NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO custom_lists_new (id, name, description, icon, ids, isPinned)
            SELECT id, name, description, icon, movieIds, isPinned
            FROM custom_lists
        """.trimIndent()
        )

        db.execSQL("DROP TABLE custom_lists")
        db.execSQL("ALTER TABLE custom_lists_new RENAME TO custom_lists")
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE tv_seasons ADD COLUMN lastEpWatched INTEGER
        """.trimIndent()
        )
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE trending_data_new (
                mainUuid TEXT PRIMARY KEY NOT NULL,
                id INTEGER NOT NULL,
                name TEXT,
                title TEXT,
                backdropPath TEXT NOT NULL,
                posterPath TEXT NOT NULL,
                mediaType TEXT NOT NULL,
                releaseDate TEXT,
                firstAirDate TEXT,
                avgRating REAL,
                overview TEXT NOT NULL,
                genreIds TEXT,
                cachedAt INTEGER NOT NULL,
                trendingType TEXT NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO trending_data_new (mainUuid, id, name, title, backdropPath, posterPath, mediaType, releaseDate, firstAirDate, avgRating, overview, genreIds, cachedAt, trendingType)
            SELECT mainUuid, id, name, title, backdropPath, posterPath, mediaType, releaseDate, firstAirDate, avgRating, overview, genreIds, cachedAt, 'day'
            FROM trending_data
        """.trimIndent()
        )

        db.execSQL("DROP TABLE trending_data")
        db.execSQL("ALTER TABLE trending_data_new RENAME TO trending_data")
    }
}

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Version 35 removes seasonId from both TV tables.
        // Preserve existing season/episode progress by mapping old episode rows
        // through tv_seasons.seasonId -> (showId, seasonNumber).
        //
        // The episode table is rebuilt first because the old episode table has a
        // foreign key pointing at tv_seasons.seasonId.
        db.execSQL(
            """
            CREATE TABLE tv_episodes_new (
                epId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                showId INTEGER NOT NULL,
                seasonNumber INTEGER NOT NULL,
                air_date TEXT,
                episode_number INTEGER NOT NULL,
                name TEXT NOT NULL,
                overview TEXT,
                still_path TEXT,
                runtime INTEGER,
                isWatched INTEGER NOT NULL,
                lastWatchedAt INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO tv_episodes_new (
                epId, showId, seasonNumber, air_date, episode_number, name, overview,
                still_path, runtime, isWatched, lastWatchedAt
            )
            SELECT
                e.epId, s.showId, s.seasonNumber, e.air_date, e.episode_number, e.name, e.overview,
                e.still_path, e.runtime, e.isWatched, e.lastWatchedAt
            FROM tv_episodes e
            INNER JOIN tv_seasons s ON e.seasonId = s.seasonId
            """.trimIndent()
        )

        db.execSQL("DROP TABLE tv_episodes")

        db.execSQL(
            """
            CREATE TABLE tv_seasons_new (
                showId INTEGER NOT NULL,
                seasonNumber INTEGER NOT NULL,
                name TEXT NOT NULL,
                episodeCount INTEGER NOT NULL,
                airDate TEXT,
                posterPath TEXT,
                status TEXT NOT NULL,
                seasonStartedDate INTEGER,
                seasonFinishedDate INTEGER,
                seasonInterruptedAt INTEGER,
                seasonAddedDate INTEGER NOT NULL,
                seasonUserRating REAL,
                seasonAvgRating REAL,
                seasonNotes TEXT,
                lastEpWatched INTEGER,
                cachedAt INTEGER NOT NULL,
                PRIMARY KEY(showId, seasonNumber),
                FOREIGN KEY(showId) REFERENCES watchlist(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO tv_seasons_new (
                showId, seasonNumber, name, episodeCount, airDate, posterPath, status,
                seasonStartedDate, seasonFinishedDate, seasonInterruptedAt, seasonAddedDate,
                seasonUserRating, seasonAvgRating, seasonNotes, lastEpWatched, cachedAt
            )
            SELECT
                showId, seasonNumber, name, episodeCount, airDate, posterPath, status,
                seasonStartedDate, seasonFinishedDate, seasonInterruptedAt, seasonAddedDate,
                seasonUserRating, seasonAvgRating, seasonNotes, lastEpWatched, cachedAt
            FROM tv_seasons
            """.trimIndent()
        )

        db.execSQL("DROP TABLE tv_seasons")

        db.execSQL("ALTER TABLE tv_seasons_new RENAME TO tv_seasons")
        db.execSQL("ALTER TABLE tv_episodes_new RENAME TO tv_episodes")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_tv_seasons_showId ON tv_seasons(showId)")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_tv_episodes_showId_seasonNumber
            ON tv_episodes(showId, seasonNumber)
            """.trimIndent()
        )
    }
}

val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE tv_bundle ADD COLUMN backdropOverride TEXT
        """.trimIndent()
        )
    }
}

val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE movie_bundle ADD COLUMN backdropOverride TEXT
        """.trimIndent()
        )
    }
}

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE watchlist ADD COLUMN isDropped INTEGER NOT NULL DEFAULT 0
        """.trimIndent()
        )
    }
}

val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE watchlist
            SET notes = (
                SELECT seasonNotes
                FROM tv_seasons
                WHERE tv_seasons.showId = watchlist.id
                  AND seasonNotes IS NOT NULL
                  AND TRIM(seasonNotes) != ''
                ORDER BY seasonNumber ASC
                LIMIT 1
            )
            WHERE mediaType = 'tv'
              AND (notes IS NULL OR TRIM(notes) = '')
              AND EXISTS (
                  SELECT 1
                  FROM tv_seasons
                  WHERE tv_seasons.showId = watchlist.id
                    AND seasonNotes IS NOT NULL
                    AND TRIM(seasonNotes) != ''
              )
        """.trimIndent()
        )
    }
}

val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE watchlist ADD COLUMN backdropOverride TEXT
        """.trimIndent()
        )
    }
}

val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE watchlist ADD COLUMN lastModifiedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tv_seasons ADD COLUMN lastModifiedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE custom_lists ADD COLUMN lastModifiedAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE custom_lists ADD COLUMN syncId TEXT NOT NULL DEFAULT ''"
        )
        // Give every existing list a stable cross-device identity.
        db.execSQL(
            "UPDATE custom_lists SET syncId = lower(hex(randomblob(16))) WHERE syncId = ''"
        )
        db.execSQL(
            "ALTER TABLE tv_episodes ADD COLUMN lastModifiedAt INTEGER NOT NULL DEFAULT 0"
        )
        // Existing watched rows retain their useful modification time. Rows that
        // were never watched remain at zero and will be refreshed normally.
        db.execSQL(
            "UPDATE tv_episodes SET lastModifiedAt = lastWatchedAt WHERE lastModifiedAt = 0 AND lastWatchedAt > 0"
        )
    }
}

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS deleted_items (id INTEGER NOT NULL, deletedAt INTEGER NOT NULL, PRIMARY KEY(id))"
        )
    }
}

val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS deleted_lists (syncId TEXT NOT NULL, deletedAt INTEGER NOT NULL, PRIMARY KEY(syncId))"
        )
    }
}
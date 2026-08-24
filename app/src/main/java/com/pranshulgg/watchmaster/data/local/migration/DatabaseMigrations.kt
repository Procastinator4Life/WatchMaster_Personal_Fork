package com.pranshulgg.watchmaster.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE tv_episodes ADD COLUMN lastWatchedAt INTEGER NOT NULL DEFAULT 0"
        )
    }
}
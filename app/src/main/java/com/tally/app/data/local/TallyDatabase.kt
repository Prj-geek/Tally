package com.tally.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import com.tally.app.data.local.entity.WatchHistoryEntity
import com.tally.app.data.local.entity.WatchlistEntity

@Database(
    entities = [WatchlistEntity::class, WatchHistoryEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class TallyDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchlist ADD COLUMN rewatchCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun create(context: Context): TallyDatabase =
            Room.databaseBuilder(context, TallyDatabase::class.java, "tally.db")
                .fallbackToDestructiveMigration(false)
                .addMigrations(MIGRATION_2_3)
                .build()
    }
}

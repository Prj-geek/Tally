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
    version = 6,
    exportSchema = false,
)
abstract class TallyDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS watchlist_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        tmdbId INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        visibility TEXT NOT NULL,
                        addedAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL,
                        remoteId INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO watchlist_new (
                        id, userId, tmdbId, status, visibility, addedAt, updatedAt, syncStatus, remoteId
                    )
                    SELECT id, userId, simklId, status, visibility, addedAt, updatedAt, syncStatus, remoteId
                    FROM watchlist
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE watchlist")
                db.execSQL("ALTER TABLE watchlist_new RENAME TO watchlist")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_watchlist_userId_tmdbId ON watchlist(userId, tmdbId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS watch_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        tmdbId INTEGER NOT NULL,
                        seasonNum INTEGER,
                        episodeNum INTEGER,
                        watchedAt INTEGER NOT NULL,
                        rewatch INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL,
                        remoteId INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO watch_history_new (
                        id, userId, tmdbId, seasonNum, episodeNum, watchedAt, rewatch, syncStatus, remoteId
                    )
                    SELECT id, userId, simklId, seasonNum, episodeNum, watchedAt, rewatch, syncStatus, remoteId
                    FROM watch_history
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE watch_history")
                db.execSQL("ALTER TABLE watch_history_new RENAME TO watch_history")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_watch_history_userId_tmdbId_seasonNum_episodeNum ON watch_history(userId, tmdbId, seasonNum, episodeNum)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchlist ADD COLUMN rewatchCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchlist ADD COLUMN mediaType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN posterPath TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchlist ADD COLUMN totalEpisodes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN watchedEpisodes INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchlist ADD COLUMN genres TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE watch_history ADD COLUMN runtime INTEGER DEFAULT NULL")
            }
        }

        fun create(context: Context): TallyDatabase =
            Room.databaseBuilder(context, TallyDatabase::class.java, "tally.db")
                .fallbackToDestructiveMigration(false)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
    }
}

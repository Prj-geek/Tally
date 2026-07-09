package com.tally.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tally.app.data.local.dao.MediaDao
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import com.tally.app.data.local.entity.MediaEntity
import com.tally.app.data.local.entity.WatchHistoryEntity
import com.tally.app.data.local.entity.WatchlistEntity

@Database(
    entities = [MediaEntity::class, WatchlistEntity::class, WatchHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TallyDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        fun create(context: Context): TallyDatabase =
            Room.databaseBuilder(context, TallyDatabase::class.java, "tally.db")
                .fallbackToDestructiveMigration(false)
                .build()
    }
}

package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tally.app.data.local.entity.WatchlistEntity

@Dao
interface WatchlistDao {

    // ponytail: only methods with active callers kept — add back as needed
    @Query("SELECT * FROM watchlist WHERE userId = :userId AND tmdbId = :tmdbId")
    suspend fun get(userId: String, tmdbId: Long): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun delete(id: Long)
}

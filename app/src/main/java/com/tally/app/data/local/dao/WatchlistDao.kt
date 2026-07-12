package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tally.app.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    // ponytail: only methods with active callers kept — add back as needed
    @Query("SELECT * FROM watchlist WHERE userId = :userId AND tmdbId = :tmdbId")
    suspend fun get(userId: String, tmdbId: Long): WatchlistEntity?

    @Query("SELECT * FROM watchlist WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAll(userId: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND mediaType = :mediaType ORDER BY updatedAt DESC")
    fun getByMediaType(userId: String, mediaType: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND status = :status ORDER BY updatedAt DESC")
    fun getByStatus(userId: String, status: String): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun delete(id: Long)
}

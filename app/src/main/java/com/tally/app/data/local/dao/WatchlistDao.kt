package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow



@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND tmdbId = :tmdbId")
    suspend fun get(userId: String, tmdbId: Long): WatchlistEntity?

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND syncStatus != 'PENDING_DELETE' ORDER BY updatedAt DESC")
    fun getAll(userId: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND mediaType = :mediaType AND syncStatus != 'PENDING_DELETE' ORDER BY updatedAt DESC")
    fun getByMediaType(userId: String, mediaType: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND status = :status AND syncStatus != 'PENDING_DELETE' ORDER BY updatedAt DESC")
    fun getByStatus(userId: String, status: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND syncStatus IN ('PENDING_ADD', 'PENDING_UPDATE')")
    suspend fun getPendingSync(userId: String): List<WatchlistEntity>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletes(userId: String): List<WatchlistEntity>

    @Query("UPDATE watchlist SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun physicalDelete(id: Long)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND syncStatus = 'SYNCED'")
    suspend fun getAllSynced(userId: String): List<WatchlistEntity>

    @Query("DELETE FROM watchlist WHERE userId = :userId AND syncStatus = 'SYNCED' AND tmdbId NOT IN (:tmdbIds)")
    suspend fun deleteSyncedNotIn(userId: String, tmdbIds: List<Long>)

    @Query("DELETE FROM watchlist WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("UPDATE watchlist SET syncStatus = 'PENDING_DELETE' WHERE userId = :userId AND syncStatus != 'PENDING_DELETE'")
    suspend fun softDeleteAllForUser(userId: String)
}

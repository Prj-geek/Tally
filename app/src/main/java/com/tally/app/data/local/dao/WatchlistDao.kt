package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAll(userId: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND status = :status ORDER BY updatedAt DESC")
    fun getByStatus(userId: String, status: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND simklId = :simklId")
    suspend fun get(userId: String, simklId: Long): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchlistEntity)

    @Update
    suspend fun update(entry: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM watchlist WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<WatchlistEntity>

    @Query("UPDATE watchlist SET syncStatus = :status, remoteId = :remoteId WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: Long, status: SyncStatus = SyncStatus.SYNCED)
}

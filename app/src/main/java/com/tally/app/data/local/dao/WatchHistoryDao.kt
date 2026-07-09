package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history WHERE userId = :userId ORDER BY watchedAt DESC")
    fun getAll(userId: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND simklId = :simklId ORDER BY seasonNum, episodeNum")
    fun getForMedia(userId: String, simklId: Long): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND simklId = :simklId AND seasonNum = :seasonNum AND episodeNum = :episodeNum")
    suspend fun get(userId: String, simklId: Long, seasonNum: Int?, episodeNum: Int?): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchHistoryEntity)

    @Update
    suspend fun update(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM watch_history WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<WatchHistoryEntity>

    @Query("UPDATE watch_history SET syncStatus = :status, remoteId = :remoteId WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: Long, status: SyncStatus = SyncStatus.SYNCED)
}

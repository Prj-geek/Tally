package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND tmdbId = :tmdbId AND syncStatus != 'PENDING_DELETE' ORDER BY seasonNum, episodeNum")
    fun getForMedia(userId: String, tmdbId: Long): Flow<List<WatchHistoryEntity>>

    @Query("SELECT COUNT(*) FROM watch_history WHERE userId = :userId AND tmdbId = :tmdbId AND syncStatus != 'PENDING_DELETE'")
    suspend fun countForMedia(userId: String, tmdbId: Long): Int

    @Query("SELECT DISTINCT tmdbId FROM watch_history WHERE userId = :userId AND syncStatus != 'PENDING_DELETE'")
    fun getAllWatchedTmdbIds(userId: String): Flow<List<Long>>

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND tmdbId = :tmdbId AND seasonNum IS :seasonNum AND episodeNum IS :episodeNum")
    suspend fun get(userId: String, tmdbId: Long, seasonNum: Int?, episodeNum: Int?): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND syncStatus IN ('PENDING_ADD', 'PENDING_UPDATE')")
    suspend fun getPendingSync(userId: String): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletes(userId: String): List<WatchHistoryEntity>

    @Query("UPDATE watch_history SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun physicalDelete(id: Long)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun delete(id: Long)
}

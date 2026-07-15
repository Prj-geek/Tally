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

    @Query("SELECT SUM(runtime) FROM watch_history h INNER JOIN watchlist w ON h.tmdbId = w.tmdbId AND h.userId = w.userId WHERE h.userId = :userId AND w.mediaType = 'movie' AND h.syncStatus != 'PENDING_DELETE'")
    fun getMovieWatchTime(userId: String): Flow<Int?>

    @Query("SELECT SUM(runtime) FROM watch_history h INNER JOIN watchlist w ON h.tmdbId = w.tmdbId AND h.userId = w.userId WHERE h.userId = :userId AND w.mediaType = 'tv' AND h.syncStatus != 'PENDING_DELETE'")
    fun getTvWatchTime(userId: String): Flow<Int?>

    @Query("SELECT COUNT(DISTINCT h.tmdbId) FROM watch_history h INNER JOIN watchlist w ON h.tmdbId = w.tmdbId AND h.userId = w.userId WHERE h.userId = :userId AND w.mediaType = 'movie' AND h.syncStatus != 'PENDING_DELETE'")
    fun getWatchedMovieCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT h.tmdbId) FROM watch_history h INNER JOIN watchlist w ON h.tmdbId = w.tmdbId AND h.userId = w.userId WHERE h.userId = :userId AND w.mediaType = 'tv' AND h.syncStatus != 'PENDING_DELETE'")
    fun getWatchedTvCount(userId: String): Flow<Int>

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND tmdbId = :tmdbId AND seasonNum IS :seasonNum AND episodeNum IS :episodeNum")
    suspend fun get(userId: String, tmdbId: Long, seasonNum: Int?, episodeNum: Int?): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND syncStatus IN ('PENDING_ADD', 'PENDING_UPDATE')")
    suspend fun getPendingSync(userId: String): List<WatchHistoryEntity>

    @Query("SELECT COUNT(*) FROM watch_history WHERE userId = :userId AND syncStatus IN ('PENDING_ADD', 'PENDING_UPDATE')")
    suspend fun countPendingSync(userId: String): Int

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletes(userId: String): List<WatchHistoryEntity>

    @Query("UPDATE watch_history SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun physicalDelete(id: Long)

    @Query("UPDATE watch_history SET syncStatus = 'PENDING_DELETE' WHERE userId = :userId AND syncStatus != 'PENDING_DELETE'")
    suspend fun softDeleteAllForUser(userId: String)
    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun physicalDeleteAllForUser(userId: String)
}

package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tally.app.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    // ponytail: only methods with active callers kept — add back as needed
    @Query("SELECT * FROM watch_history WHERE userId = :userId AND tmdbId = :tmdbId ORDER BY seasonNum, episodeNum")
    fun getForMedia(userId: String, tmdbId: Long): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND tmdbId = :tmdbId AND seasonNum = :seasonNum AND episodeNum = :episodeNum")
    suspend fun get(userId: String, tmdbId: Long, seasonNum: Int?, episodeNum: Int?): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun delete(id: Long)
}

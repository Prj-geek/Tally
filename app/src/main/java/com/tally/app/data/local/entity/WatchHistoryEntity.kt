package com.tally.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tally.app.data.local.SyncStatus

/**
 * Movie rows use null season and episode values. SQLite and Postgres unique indexes treat
 * NULL != NULL, so the unique index does not dedupe movie watch-history rows.
 *
 * New movie "mark watched" call sites must first call
 * watchHistoryDao.get(uid, tmdbId, null, null) and update that row when present instead of
 * relying on OnConflictStrategy.REPLACE. Rewatch rows intentionally skip that lookup because
 * each rewatch is a separate row used by SUM(runtime) watch-time stats.
 */
@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["userId", "tmdbId", "seasonNum", "episodeNum"], unique = true)],
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val tmdbId: Long,
    val seasonNum: Int? = null,
    val episodeNum: Int? = null,
    val runtime: Int? = null,
    val watchedAt: Long = System.currentTimeMillis(),
    val rewatch: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_ADD,
    val remoteId: Long? = null,
)

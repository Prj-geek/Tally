package com.tally.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tally.app.data.local.SyncStatus

@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["userId", "simklId", "seasonNum", "episodeNum"], unique = true)],
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val simklId: Long,
    val seasonNum: Int? = null,
    val episodeNum: Int? = null,
    val watchedAt: Long = System.currentTimeMillis(),
    val rewatch: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_ADD,
    val remoteId: Long? = null,
)

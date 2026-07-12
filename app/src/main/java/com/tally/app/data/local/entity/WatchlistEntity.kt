package com.tally.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tally.app.data.local.SyncStatus

@Entity(
    tableName = "watchlist",
    indices = [Index(value = ["userId", "tmdbId"], unique = true)],
)
data class WatchlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val tmdbId: Long,
    val status: String,
    val rewatchCount: Int = 0,
    val visibility: String = "private",
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_ADD,
    val remoteId: Long? = null,
)

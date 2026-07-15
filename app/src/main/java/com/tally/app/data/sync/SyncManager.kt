package com.tally.app.data.sync

import android.util.Log
import com.tally.app.data.auth.AuthRepository
import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncManager"

@Singleton
class SyncManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val watchlistDao: WatchlistDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val authRepository: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()
    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()
    private var pendingSync = false

    fun sync() {
        scope.launch { doSync() }
    }

    /** Suspends until the current sync cycle finishes. Useful for clear-data. */
    suspend fun syncAndWait() {
        if (!_isSyncing.value) {
            doSync()
            return
        }
        // Already syncing — wait for it to finish
        _isSyncing.first { !it }
    }

    private suspend fun doSync() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            Log.w(TAG, "sync: no user ID, skipping")
            return
        }
        if (_isSyncing.value) {
            pendingSync = true
            return
        }
        Log.d(TAG, "sync: starting for user $uid")
        _isSyncing.value = true
        _lastSyncError.value = null
        try {
            pushPendingChanges(uid)
            pullRemoteChanges(uid)
            Log.d(TAG, "sync: completed")
        } catch (e: Exception) {
            Log.e(TAG, "sync failed", e)
            _lastSyncError.value = e.message ?: "Sync failed"
        } finally {
            _isSyncing.value = false
            updatePendingCount(uid)
            if (pendingSync) {
                pendingSync = false
                doSync()
            }
        }
    }

    suspend fun getPendingCount(uid: String): Int =
        watchlistDao.countPendingSync(uid) + watchHistoryDao.countPendingSync(uid)

    private suspend fun updatePendingCount(uid: String) {
        _pendingCount.value = watchlistDao.countPendingSync(uid) + watchHistoryDao.countPendingSync(uid)
    }

    private suspend fun pushPendingChanges(uid: String) {
        val pendingWatchlist = watchlistDao.getPendingSync(uid)
        Log.d(TAG, "push: ${pendingWatchlist.size} watchlist entries pending")
        for (entry in pendingWatchlist) {
            try {
                val current = watchlistDao.get(uid, entry.tmdbId) ?: continue
                if (current.syncStatus != SyncStatus.PENDING_ADD && current.syncStatus != SyncStatus.PENDING_UPDATE) continue
                Log.d(TAG, "push: watchlist upsert tmdbId=${current.tmdbId} status=${current.status} watchedEpisodes=${current.watchedEpisodes}")
                val result = supabase.from("watchlist").upsert(current.toSupabase()) {
                    onConflict = "user_id,tmdb_id"
                    select()
                }.decodeSingle<SupabaseWatchlistEntry>()
                watchlistDao.upsert(current.copy(syncStatus = SyncStatus.SYNCED, remoteId = result.id))
                Log.d(TAG, "push: watchlist tmdbId=${current.tmdbId} synced")
            } catch (e: Exception) {
                Log.e(TAG, "push: watchlist upsert failed for tmdbId=${entry.tmdbId}", e)
                _lastSyncError.value = e.message ?: "Watchlist sync failed"
            }
        }

        val pendingHistory = watchHistoryDao.getPendingSync(uid)
        Log.d(TAG, "push: ${pendingHistory.size} history entries pending")
        for (entry in pendingHistory) {
            try {
                val current = watchHistoryDao.get(uid, entry.tmdbId, entry.seasonNum, entry.episodeNum) ?: continue
                if (current.syncStatus != SyncStatus.PENDING_ADD && current.syncStatus != SyncStatus.PENDING_UPDATE) continue
                
                val result = supabase.from("watch_history").upsert(current.toSupabase()) {
                    onConflict = "user_id,tmdb_id,season_num,episode_num"
                    select()
                }.decodeSingle<SupabaseWatchHistoryEntry>()

                watchHistoryDao.upsert(current.copy(syncStatus = SyncStatus.SYNCED, remoteId = result.id))
                Log.d(TAG, "push: history tmdbId=${current.tmdbId} season=${current.seasonNum} episode=${current.episodeNum} synced")
            } catch (e: Exception) {
                Log.e(TAG, "push: history upsert failed", e)
                _lastSyncError.value = e.message ?: "History sync failed"
            }
        }

        val pendingWatchlistDeletes = watchlistDao.getPendingDeletes(uid)
        Log.d(TAG, "push: ${pendingWatchlistDeletes.size} watchlist deletes pending")
        for (entry in pendingWatchlistDeletes) {
            try {
                supabase.from("watchlist").delete {
                    filter {
                        eq("user_id", uid)
                        eq("tmdb_id", entry.tmdbId)
                    }
                }
                watchlistDao.physicalDelete(entry.id)
            } catch (e: Exception) {
                Log.e(TAG, "push: watchlist delete failed", e)
                _lastSyncError.value = e.message ?: "Watchlist delete failed"
            }
        }

        val pendingHistoryDeletes = watchHistoryDao.getPendingDeletes(uid)
        for (entry in pendingHistoryDeletes) {
            try {
                val remoteEntry = entry.toSupabase()
                supabase.from("watch_history").delete {
                    filter {
                        eq("user_id", uid)
                        eq("tmdb_id", entry.tmdbId)
                        eq("season_num", remoteEntry.seasonNum ?: 0)
                        eq("episode_num", remoteEntry.episodeNum ?: 0)
                    }
                }
                watchHistoryDao.physicalDelete(entry.id)
            } catch (e: Exception) {
                Log.e(TAG, "push: history delete failed", e)
                _lastSyncError.value = e.message ?: "History delete failed"
            }
        }
    }

    /**
     * Supabase/PostgREST silently caps unpaginated selects at 1000 rows (project default).
     * A plain .select() with no .range() returns a truncated, arbitrarily-ordered subset once
     * a table exceeds that cap -- no error, no warning. We must page through with a stable
     * ORDER BY before doing any reconciliation against the remote set (especially before
     * deleteSyncedNotIn, which would otherwise treat "not in this page" as "delete locally").
     */
    private suspend inline fun <reified T : Any> fetchAllPages(
        table: String,
        uid: String,
    ): List<T> {
        val pageSize = 1000L
        val all = mutableListOf<T>()
        var offset = 0L
        while (true) {
            val page = supabase.from(table)
                .select {
                    filter { eq("user_id", uid) }
                    order("id", Order.ASCENDING)
                    range(offset, offset + pageSize - 1)
                }
                .decodeList<T>()
            all += page
            if (page.size < pageSize) break
            offset += pageSize
        }
        return all
    }

    private suspend fun pullRemoteChanges(uid: String) {
        val remoteWatchlist = try {
            fetchAllPages<SupabaseWatchlistEntry>("watchlist", uid)
        } catch (e: Exception) {
            Log.e(TAG, "pull: watchlist fetch failed", e)
            _lastSyncError.value = e.message ?: "Watchlist pull failed"
            // Bail out entirely -- do NOT proceed to deleteSyncedNotIn with a partial list.
            return
        }
        Log.d(TAG, "pull: ${remoteWatchlist.size} remote watchlist entries")

        if (remoteWatchlist.isNotEmpty()) {
            for (remote in remoteWatchlist) {
                val local = watchlistDao.get(uid, remote.tmdbId)
                if (local == null) {
                    watchlistDao.upsert(remote.toLocalEntity())
                } else if (
                    local.syncStatus == SyncStatus.SYNCED &&
                    (remote.updatedAt > local.updatedAt || remote.genres != local.genres)
                ) {
                    watchlistDao.upsert(
                        local.copy(
                            mediaType = remote.mediaType,
                            title = remote.title,
                            posterPath = remote.posterPath,
                            status = remote.status,
                            rewatchCount = remote.rewatchCount,
                            totalEpisodes = remote.totalEpisodes,
                            watchedEpisodes = remote.watchedEpisodes,
                            genres = remote.genres,
                            visibility = remote.visibility,
                            updatedAt = remote.updatedAt,
                            syncStatus = SyncStatus.SYNCED,
                        )
                    )
                }
            }
            val remoteTmdbIds = remoteWatchlist.map { it.tmdbId }
            watchlistDao.deleteSyncedNotIn(uid, remoteTmdbIds)
        }

        val remoteHistory = try {
            fetchAllPages<SupabaseWatchHistoryEntry>("watch_history", uid)
        } catch (e: Exception) {
            Log.e(TAG, "pull: history fetch failed", e)
            emptyList()
        }
        Log.d(TAG, "pull: ${remoteHistory.size} remote history entries")

        for (remote in remoteHistory) {
            val normalizedRemote = remote.toLocalEntity()
            val local = watchHistoryDao.get(uid, normalizedRemote.tmdbId, normalizedRemote.seasonNum, normalizedRemote.episodeNum)
            if (local == null) {
                watchHistoryDao.upsert(normalizedRemote)
            } else if (local.syncStatus == SyncStatus.SYNCED && local.runtime != normalizedRemote.runtime) {
                watchHistoryDao.upsert(
                    local.copy(
                        runtime = normalizedRemote.runtime,
                        watchedAt = normalizedRemote.watchedAt,
                        rewatch = normalizedRemote.rewatch,
                        remoteId = normalizedRemote.remoteId,
                        syncStatus = SyncStatus.SYNCED,
                    )
                )
            }
        }
    }
}

package com.tally.app.data.sync

import android.util.Log
import com.tally.app.data.auth.AuthRepository
import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private var pendingSync = false

    fun sync() {
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
        scope.launch {
            _isSyncing.value = true
            try {
                pushPendingChanges(uid)
                pullRemoteChanges(uid)
                Log.d(TAG, "sync: completed")
            } catch (e: Exception) {
                Log.e(TAG, "sync failed", e)
            } finally {
                _isSyncing.value = false
                if (pendingSync) {
                    pendingSync = false
                    sync()
                }
            }
        }
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
            }
        }

        val pendingHistory = watchHistoryDao.getPendingSync(uid)
        Log.d(TAG, "push: ${pendingHistory.size} history entries pending")
        for (entry in pendingHistory) {
            try {
                val current = watchHistoryDao.get(uid, entry.tmdbId, entry.seasonNum, entry.episodeNum) ?: continue
                if (current.syncStatus != SyncStatus.PENDING_ADD && current.syncStatus != SyncStatus.PENDING_UPDATE) continue
                
                val result = supabase.from("watch_history").upsert(current.toSupabase()) {
                    select()
                }.decodeSingle<SupabaseWatchHistoryEntry>()
                
                watchHistoryDao.upsert(current.copy(syncStatus = SyncStatus.SYNCED, remoteId = result.id))
            } catch (e: Exception) {
                Log.e(TAG, "push: history upsert failed", e)
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
            }
        }

        val pendingHistoryDeletes = watchHistoryDao.getPendingDeletes(uid)
        for (entry in pendingHistoryDeletes) {
            try {
                supabase.from("watch_history").delete {
                    filter {
                        eq("user_id", uid)
                        eq("tmdb_id", entry.tmdbId)
                        if (entry.seasonNum != null) eq("season_num", entry.seasonNum) else filterNot("season_num", io.github.jan.supabase.postgrest.query.filter.FilterOperator.NEQ, "null")
                        if (entry.episodeNum != null) eq("episode_num", entry.episodeNum) else filterNot("episode_num", io.github.jan.supabase.postgrest.query.filter.FilterOperator.NEQ, "null")
                    }
                }
                watchHistoryDao.physicalDelete(entry.id)
            } catch (e: Exception) {
                Log.e(TAG, "push: history delete failed", e)
            }
        }
    }

    private suspend fun pullRemoteChanges(uid: String) {
        val remoteWatchlist = try {
            supabase.from("watchlist")
                .select { filter { eq("user_id", uid) } }
                .decodeList<SupabaseWatchlistEntry>()
        } catch (e: Exception) {
            Log.e(TAG, "pull: watchlist fetch failed", e)
            emptyList()
        }
        Log.d(TAG, "pull: ${remoteWatchlist.size} remote watchlist entries")

        if (remoteWatchlist.isNotEmpty()) {
            for (remote in remoteWatchlist) {
                val local = watchlistDao.get(uid, remote.tmdbId)
                if (local == null) {
                    watchlistDao.upsert(remote.toLocalEntity())
                } else if (remote.updatedAt > local.updatedAt && local.syncStatus == SyncStatus.SYNCED) {
                    watchlistDao.upsert(
                        local.copy(
                            mediaType = remote.mediaType,
                            title = remote.title,
                            posterPath = remote.posterPath,
                            status = remote.status,
                            rewatchCount = remote.rewatchCount,
                            totalEpisodes = remote.totalEpisodes,
                            watchedEpisodes = remote.watchedEpisodes,
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
            supabase.from("watch_history")
                .select { filter { eq("user_id", uid) } }
                .decodeList<SupabaseWatchHistoryEntry>()
        } catch (e: Exception) {
            Log.e(TAG, "pull: history fetch failed", e)
            emptyList()
        }
        Log.d(TAG, "pull: ${remoteHistory.size} remote history entries")

        for (remote in remoteHistory) {
            val local = watchHistoryDao.get(uid, remote.tmdbId, remote.seasonNum, remote.episodeNum)
            if (local == null) {
                watchHistoryDao.upsert(remote.toLocalEntity())
            }
        }
    }
}

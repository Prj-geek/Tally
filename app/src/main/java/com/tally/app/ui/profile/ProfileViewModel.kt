package com.tally.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.auth.AuthRepository
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import com.tally.app.data.remote.TmdbImageUrl
import com.tally.app.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String) : AuthState
}

data class WatchedItem(
    val tmdbId: Long,
    val mediaType: String,
    val title: String,
    val posterUrl: String?,
)

data class WatchedState(
    val watchedMovies: List<WatchedItem> = emptyList(),
    val watchedShows: List<WatchedItem> = emptyList(),
    val movieWatchTimeMinutes: Int = 0,
    val tvWatchTimeMinutes: Int = 0,
    val watchedMovieCount: Int = 0,
    val watchedTvCount: Int = 0,
    val watchlistedMovieCount: Int = 0,
    val watchlistedTvCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val watchlistDao: WatchlistDao,
    private val watchHistoryDao: WatchHistoryDao,
    val syncManager: SyncManager,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _watchedState = MutableStateFlow(WatchedState())
    val watchedState: StateFlow<WatchedState> = _watchedState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                _authState.value = when (status) {
                    is SessionStatus.Initializing -> AuthState.Loading
                    is SessionStatus.NotAuthenticated -> {
                        _avatarUrl.value = null
                        AuthState.SignedOut
                    }
                    is SessionStatus.Authenticated -> {
                        _avatarUrl.value = (status.session.user?.userMetadata?.get("avatar_url") as? JsonPrimitive)?.content
                        AuthState.SignedIn(
                            userId = status.session.user?.id ?: return@collect
                        )
                    }
                    is SessionStatus.RefreshFailure -> AuthState.SignedOut
                }
            }
        }
    }

    // ponytail: load watched data when user signs in
    fun loadWatched() {
        val uid = (authState.value as? AuthState.SignedIn)?.userId ?: return
        syncManager.sync()
        viewModelScope.launch {
            val statsFlow = combine(
                watchHistoryDao.getMovieWatchTime(uid),
                watchHistoryDao.getTvWatchTime(uid),
                watchHistoryDao.getWatchedMovieCount(uid),
                watchHistoryDao.getWatchedTvCount(uid),
                watchlistDao.getWatchlistedMovieCount(uid),
            ) { mt, tt, mc, tc, wlmc ->
                arrayOf(mt, tt, mc, tc, wlmc)
            }
            combine(
                watchlistDao.getAll(uid),
                statsFlow,
                watchlistDao.getWatchlistedTvCount(uid),
            ) { entries, arr, wlTvCount ->
                val movieTime = (arr[0] as? Int) ?: 0
                val tvTime = (arr[1] as? Int) ?: 0
                val movieCount = arr[2] as Int
                val tvCount = arr[3] as Int
                val wlMovieCount = arr[4] as Int
                WatchedState(
                    watchedMovies = entries
                        .filter { it.mediaType == "movie" && it.status == "watched" }
                        .map { WatchedItem(it.tmdbId, "movie", it.title, TmdbImageUrl.poster(it.posterPath)) },
                    watchedShows = entries
                        .filter { it.mediaType == "tv" && it.watchedEpisodes >= it.totalEpisodes && it.totalEpisodes > 0 }
                        .map { WatchedItem(it.tmdbId, "tv", it.title, TmdbImageUrl.poster(it.posterPath)) },
                    movieWatchTimeMinutes = movieTime,
                    tvWatchTimeMinutes = tvTime,
                    watchedMovieCount = movieCount,
                    watchedTvCount = tvCount,
                    watchlistedMovieCount = wlMovieCount,
                    watchlistedTvCount = wlTvCount,
                )
            }.collect { _watchedState.value = it }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                authRepository.signInWithGoogle(idToken)
            } catch (e: Exception) {
                _authState.value = AuthState.SignedOut
                _error.emit(e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearData() {
        val uid = (authState.value as? AuthState.SignedIn)?.userId ?: return
        viewModelScope.launch {
            // 1. Soft-delete locally (marks PENDING_DELETE, UI shows empty immediately)
            watchlistDao.softDeleteAllForUser(uid)
            watchHistoryDao.softDeleteAllForUser(uid)
            // 2. Trigger sync to push deletes to Supabase, wait for completion
            syncManager.syncAndWait()
            // 3. Physical-delete local PENDING_DELETE rows to clean up
            watchlistDao.physicalDeleteAllForUser(uid)
            watchHistoryDao.physicalDeleteAllForUser(uid)
            loadWatched()
        }
    }
}

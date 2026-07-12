package com.tally.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.auth.AuthRepository
import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import com.tally.app.data.local.entity.WatchHistoryEntity
import com.tally.app.data.local.entity.WatchlistEntity
import com.tally.app.data.remote.EpisodeGroupOverrideRepository
import com.tally.app.data.remote.TmdbImageUrl
import com.tally.app.data.remote.TmdbRepository
import com.tally.app.data.remote.model.TmdbEpisode
import com.tally.app.data.remote.model.TmdbEpisodeGroupGroup
import com.tally.app.data.remote.toTmdbEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ponytail: status values stored in DB — "watchlist" or "watched"
private const val STATUS_WATCHLIST = "watchlist"
private const val STATUS_WATCHED = "watched"

data class DetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val mediaType: String = "",
    val title: String = "",
    val synopsis: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val rating: Double? = null,
    val voteCount: Int? = null,
    val runtime: Int? = null,
    val numSeasons: Int? = null,
    val numEpisodes: Int? = null,
    val seasonLabels: List<String> = emptyList(),
    val selectedSeasonIndex: Int = 0,
    val episodes: List<TmdbEpisode> = emptyList(),
    val usingEpisodeGroup: Boolean = false,
    val isWatchlisted: Boolean = false,
    val isWatched: Boolean = false,
    val rewatchCount: Int = 0,
    // ponytail: set of (seasonNum, episodeNum) pairs that are watched
    val watchedEpisodes: Set<Pair<Int, Int>> = emptySet(),
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: TmdbRepository,
    private val episodeGroupOverrideRepository: EpisodeGroupOverrideRepository,
    private val watchlistDao: WatchlistDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mediaType: String = savedStateHandle.get<String>("mediaType") ?: ""
    private val mediaId: Int = savedStateHandle.get<Int>("id") ?: 0
    private val userId: String? get() = authRepository.currentUserId

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private var cachedSeasonNumbers: List<Int> = emptyList()

    init {
        if (mediaId > 0) loadDetails()
    }

    fun onSeasonSelected(index: Int) {
        _state.value = _state.value.copy(selectedSeasonIndex = index)

        if (_state.value.usingEpisodeGroup) {
            val group = cachedEpisodeGroups?.getOrNull(index) ?: return
            _state.value = _state.value.copy(
                episodes = group.episodes.map { it.toTmdbEpisode() },
            )
        } else {
            val seasonNum = cachedSeasonNumbers.getOrNull(index) ?: return
            loadEpisodesFromApi(seasonNum)
        }
    }

    // ponytail: cached episode groups from override, null = not using override
    private var cachedEpisodeGroups: List<TmdbEpisodeGroupGroup>? = null

    private fun loadDetails() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val isMovie = mediaType == "movie"
                if (isMovie) {
                    val movie = repository.getMovieDetails(mediaId)
                    if (movie != null) {
                        _state.value = DetailUiState(
                            isLoading = false,
                            mediaType = "movie",
                            title = movie.title,
                            synopsis = movie.overview,
                            posterUrl = TmdbImageUrl.poster(movie.posterPath, size = "w500"),
                            backdropUrl = TmdbImageUrl.backdrop(movie.backdropPath),
                            year = movie.releaseDate?.take(4)?.toIntOrNull(),
                            genres = movie.genres?.map { it.name }.orEmpty(),
                            rating = movie.voteAverage,
                            voteCount = movie.voteCount,
                            runtime = movie.runtime,
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false, error = "Movie not found")
                    }
                } else {
                    val tv = repository.getTvShowDetails(mediaId)
                    if (tv != null) {
                        val showSeasonInfo = episodeGroupOverrideRepository.getShowSeasons(tv)
                        val usingOverride = showSeasonInfo.isNotEmpty()

                        if (usingOverride) {
                            cachedEpisodeGroups = showSeasonInfo
                            val firstGroup = showSeasonInfo.first()

                            _state.value = DetailUiState(
                                isLoading = false,
                                mediaType = "tv",
                                title = tv.name,
                                synopsis = tv.overview,
                                posterUrl = TmdbImageUrl.poster(tv.posterPath, size = "w500"),
                                backdropUrl = TmdbImageUrl.backdrop(tv.backdropPath),
                                year = tv.firstAirDate?.take(4)?.toIntOrNull(),
                                genres = tv.genres?.map { it.name }.orEmpty(),
                                rating = tv.voteAverage,
                                voteCount = tv.voteCount,
                                numSeasons = showSeasonInfo.size,
                                numEpisodes = showSeasonInfo.sumOf { it.episodes.size },
                                seasonLabels = showSeasonInfo.map { it.name },
                                selectedSeasonIndex = 0,
                                episodes = firstGroup.episodes.map { it.toTmdbEpisode() },
                                usingEpisodeGroup = true,
                            )
                        } else {
                            cachedSeasonNumbers = tv.seasons
                                ?.filter { it.seasonNumber > 0 }
                                ?.map { it.seasonNumber }
                                .orEmpty()
                            val defaultSeason = cachedSeasonNumbers.firstOrNull() ?: 1
                            val episodesDeferred = async { repository.getSeasonEpisodes(mediaId, defaultSeason) }

                            _state.value = DetailUiState(
                                isLoading = false,
                                mediaType = "tv",
                                title = tv.name,
                                synopsis = tv.overview,
                                posterUrl = TmdbImageUrl.poster(tv.posterPath, size = "w500"),
                                backdropUrl = TmdbImageUrl.backdrop(tv.backdropPath),
                                year = tv.firstAirDate?.take(4)?.toIntOrNull(),
                                genres = tv.genres?.map { it.name }.orEmpty(),
                                rating = tv.voteAverage,
                                voteCount = tv.voteCount,
                                numSeasons = tv.numberOfSeasons,
                                numEpisodes = tv.numberOfEpisodes,
                                seasonLabels = cachedSeasonNumbers.map { "Season $it" },
                                selectedSeasonIndex = 0,
                                episodes = episodesDeferred.await(),
                                usingEpisodeGroup = false,
                            )
                        }
                    } else {
                        _state.value = _state.value.copy(isLoading = false, error = "TV show not found")
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
                _error.emit(e.message ?: "Failed to load details")
            }
            // ponytail: load watchlist status after details so it doesn't get overwritten
            loadWatchlistStatus()
            loadWatchedEpisodes()
        }
    }

    private fun loadEpisodesFromApi(seasonNumber: Int) {
        viewModelScope.launch {
            try {
                val episodes = repository.getSeasonEpisodes(mediaId, seasonNumber)
                _state.value = _state.value.copy(episodes = episodes)
            } catch (_: Exception) { }
        }
    }

    private fun loadWatchlistStatus() {
        val uid = userId ?: return
        viewModelScope.launch {
            val entry = watchlistDao.get(uid, mediaId.toLong())
            if (entry != null) {
                _state.value = _state.value.copy(
                    isWatchlisted = entry.status == STATUS_WATCHLIST,
                    isWatched = entry.status == STATUS_WATCHED,
                    rewatchCount = entry.rewatchCount,
                )
            }
        }
    }

    // ponytail: collect watched episodes from DB, keep UI state in sync
    private fun loadWatchedEpisodes() {
        val uid = userId ?: return
        viewModelScope.launch {
            watchHistoryDao.getForMedia(uid, mediaId.toLong()).collect { entries ->
                _state.value = _state.value.copy(
                    watchedEpisodes = entries.map { it.seasonNum!! to it.episodeNum!! }.toSet()
                )
            }
        }
    }

    fun onToggleEpisodeWatched(seasonNum: Int, episodeNum: Int) {
        val uid = userId
        if (uid == null) {
            viewModelScope.launch { _error.emit("Sign in to track episodes") }
            return
        }
        viewModelScope.launch {
            val existing = watchHistoryDao.get(uid, mediaId.toLong(), seasonNum, episodeNum)
            if (existing != null) {
                watchHistoryDao.delete(existing.id)
            } else {
                watchHistoryDao.upsert(
                    WatchHistoryEntity(userId = uid, tmdbId = mediaId.toLong(), seasonNum = seasonNum, episodeNum = episodeNum)
                )
            }
        }
    }

    fun onToggleSeasonWatched(seasonNum: Int, episodeCount: Int, watchAll: Boolean) {
        val uid = userId
        if (uid == null) {
            viewModelScope.launch { _error.emit("Sign in to track episodes") }
            return
        }
        viewModelScope.launch {
            if (watchAll) {
                for (ep in 1..episodeCount) {
                    val existing = watchHistoryDao.get(uid, mediaId.toLong(), seasonNum, ep)
                    if (existing == null) {
                        watchHistoryDao.upsert(
                            WatchHistoryEntity(userId = uid, tmdbId = mediaId.toLong(), seasonNum = seasonNum, episodeNum = ep)
                        )
                    }
                }
            } else {
                // Unwatch all: delete all episodes for this season
                watchHistoryDao.getForMedia(uid, mediaId.toLong())
                    .first()
                    .filter { it.seasonNum == seasonNum }
                    .forEach { watchHistoryDao.delete(it.id) }
            }
        }
    }

    // ponytail: simple toggle — add to watchlist or remove
    fun onToggleWatchlist() {
        val uid = userId
        if (uid == null) {
            viewModelScope.launch { _error.emit("Sign in to use watchlist") }
            return
        }
        viewModelScope.launch {
            val current = watchlistDao.get(uid, mediaId.toLong())
            if (current == null) {
                // Not in any list → add to watchlist
                watchlistDao.upsert(
                    WatchlistEntity(userId = uid, tmdbId = mediaId.toLong(), status = STATUS_WATCHLIST)
                )
                _state.value = _state.value.copy(isWatchlisted = true, isWatched = false, rewatchCount = 0)
            } else if (current.status == STATUS_WATCHLIST) {
                // In watchlist → remove
                watchlistDao.delete(current.id)
                _state.value = _state.value.copy(isWatchlisted = false, isWatched = false, rewatchCount = 0)
            }
            // If watched, watchlist toggle does nothing (watched takes priority)
        }
    }

    // ponytail: check watched — add to watched, remove from watchlist
    fun onCheckWatched() {
        val uid = userId
        if (uid == null) {
            viewModelScope.launch { _error.emit("Sign in to use watchlist") }
            return
        }
        viewModelScope.launch {
            val current = watchlistDao.get(uid, mediaId.toLong())
            if (current != null) {
                watchlistDao.upsert(current.copy(status = STATUS_WATCHED, rewatchCount = 0, updatedAt = System.currentTimeMillis()))
            } else {
                watchlistDao.upsert(
                    WatchlistEntity(userId = uid, tmdbId = mediaId.toLong(), status = STATUS_WATCHED)
                )
            }
            _state.value = _state.value.copy(isWatchlisted = false, isWatched = true, rewatchCount = 0)
        }
    }

    // ponytail: uncheck watched — choose "Not Watched" or "Rewatched"
    fun onUncheckWatched(isRewatched: Boolean) {
        val uid = userId ?: return
        viewModelScope.launch {
            val current = watchlistDao.get(uid, mediaId.toLong()) ?: return@launch
            if (isRewatched) {
                // Rewatched — increment count, stay watched
                // ponytail: first rewatch = 2, then increment from there
                val newCount = maxOf(current.rewatchCount + 1, 2)
                watchlistDao.upsert(current.copy(rewatchCount = newCount, updatedAt = System.currentTimeMillis()))
                _state.value = _state.value.copy(rewatchCount = newCount)
            } else {
                // Not Watched — remove from watched entirely
                watchlistDao.delete(current.id)
                _state.value = _state.value.copy(isWatchlisted = false, isWatched = false, rewatchCount = 0)
            }
        }
    }
}

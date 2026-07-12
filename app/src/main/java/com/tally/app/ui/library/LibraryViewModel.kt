package com.tally.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.auth.AuthRepository
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import com.tally.app.data.local.entity.WatchlistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryItem(
    val tmdbId: Long,
    val mediaType: String,
    val title: String,
    val posterUrl: String?,
    val status: String,
    val hasWatchedEpisodes: Boolean = false,
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val showItems: List<LibraryItem> = emptyList(),
    val movieItems: List<LibraryItem> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        val uid = authRepository.currentUserId ?: run {
            _state.value = LibraryUiState(isLoading = false)
            return
        }
        viewModelScope.launch {
            val watchlistFlow = watchlistDao.getAll(uid)
            // ponytail: collect all tmdbIds that have watch history entries
            val historyFlow = watchHistoryDao.getAllWatchedTmdbIds(uid).map { it.toSet() }

            combine(watchlistFlow, historyFlow) { entries, watchedIds ->
                LibraryUiState(
                    isLoading = false,
                    showItems = entries
                        .filter { it.mediaType == "tv" }
                        .map { it.toLibraryItem(it.tmdbId in watchedIds) },
                    movieItems = entries
                        .filter { it.mediaType == "movie" }
                        .map { it.toLibraryItem(false) },
                )
            }.collect { _state.value = it }
        }
    }

    private fun WatchlistEntity.toLibraryItem(hasWatchedEpisodes: Boolean) = LibraryItem(
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        posterUrl = posterPath,
        status = status,
        hasWatchedEpisodes = hasWatchedEpisodes,
    )
}

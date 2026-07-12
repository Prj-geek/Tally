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
import kotlinx.coroutines.flow.first
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
            watchlistDao.getAll(uid).collect { entries ->
                // Check which shows have watched episodes
                val showEntries = entries.filter { it.mediaType == "tv" }
                val showIdsWithEpisodes = mutableSetOf<Long>()
                for (show in showEntries) {
                    val history = watchHistoryDao.getForMedia(uid, show.tmdbId).first()
                    if (history.isNotEmpty()) showIdsWithEpisodes.add(show.tmdbId)
                }

                _state.value = LibraryUiState(
                    isLoading = false,
                    showItems = showEntries.map { it.toLibraryItem(it.tmdbId in showIdsWithEpisodes) },
                    movieItems = entries.filter { it.mediaType == "movie" }.map { it.toLibraryItem() },
                )
            }
        }
    }

    private fun WatchlistEntity.toLibraryItem(hasWatchedEpisodes: Boolean = false) = LibraryItem(
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        posterUrl = posterPath,
        status = status,
        hasWatchedEpisodes = hasWatchedEpisodes,
    )
}

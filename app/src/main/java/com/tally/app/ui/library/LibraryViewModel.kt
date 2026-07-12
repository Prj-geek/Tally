package com.tally.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.auth.AuthRepository
import com.tally.app.data.local.dao.WatchlistDao
import com.tally.app.data.local.entity.WatchlistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryItem(
    val tmdbId: Long,
    val mediaType: String,
    val title: String,
    val posterUrl: String?,
    val status: String,
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val showItems: List<LibraryItem> = emptyList(),
    val movieItems: List<LibraryItem> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private var collectionJob: Job? = null

    // ponytail: reload when screen appears — handles auth being ready later
    fun load() {
        if (collectionJob?.isActive == true) return
        val uid = authRepository.currentUserId
        if (uid == null) {
            _state.value = LibraryUiState(isLoading = false)
            return
        }
        collectionJob = viewModelScope.launch {
            watchlistDao.getAll(uid).collect { entries ->
                _state.value = LibraryUiState(
                    isLoading = false,
                    showItems = entries.filter { it.mediaType == "tv" }.map { it.toLibraryItem() },
                    movieItems = entries.filter { it.mediaType == "movie" }.map { it.toLibraryItem() },
                )
            }
        }
    }

    private fun WatchlistEntity.toLibraryItem() = LibraryItem(
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        posterUrl = posterPath,
        status = status,
    )
}

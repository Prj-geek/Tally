package com.tally.app.ui.search

import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.remote.TmdbRepository
import com.tally.app.data.remote.model.TmdbSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<TmdbSearchResult> = emptyList(),
    val isSearching: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: TmdbRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _queryFlow = MutableStateFlow("")

    private val searchCache = LruCache<String, List<TmdbSearchResult>>(50)

    init {
        _queryFlow
            .debounce(400)
            .filter { it.length >= 2 }
            .distinctUntilChanged()
            .onEach { performSearch() }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        _queryFlow.value = query
    }

    private fun performSearch() {
        val query = _uiState.value.query.trim()
        if (query.length < 2) return

        val cacheKey = query.lowercase()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)

            val cached = searchCache.get(cacheKey)
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    results = cached,
                    isSearching = false,
                )
                return@launch
            }

            try {
                val results = repository.search(query)
                searchCache.put(cacheKey, results)
                _uiState.value = _uiState.value.copy(
                    results = results,
                    isSearching = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSearching = false)
                _error.emit(e.message ?: "Search failed")
            }
        }
    }
}

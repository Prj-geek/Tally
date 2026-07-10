package com.tally.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.remote.EpisodeGroupEpisode
import com.tally.app.data.remote.EpisodeGroupOverrideRepository
import com.tally.app.data.remote.TmdbImageUrl
import com.tally.app.data.remote.TmdbRepository
import com.tally.app.data.remote.model.TmdbEpisode
import com.tally.app.data.remote.toTmdbEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: TmdbRepository,
    private val episodeGroupOverrideRepository: EpisodeGroupOverrideRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mediaType: String = savedStateHandle.get<String>("mediaType") ?: ""
    private val mediaId: Int = savedStateHandle.get<Int>("id") ?: 0

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
            val group = cachedShowSeasonInfo?.seasons?.getOrNull(index) ?: return
            _state.value = _state.value.copy(
                episodes = group.episodes.map { it.toTmdbEpisode() },
            )
        } else {
            val seasonNum = cachedSeasonNumbers.getOrNull(index) ?: return
            loadEpisodesFromApi(seasonNum)
        }
    }

    private var cachedShowSeasonInfo: com.tally.app.data.remote.ShowSeasonInfo? = null

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
                        val usingOverride = showSeasonInfo.seasons.isNotEmpty()

                        if (usingOverride) {
                            cachedShowSeasonInfo = showSeasonInfo
                            val firstGroup = showSeasonInfo.seasons.first()

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
                                numSeasons = showSeasonInfo.seasons.size,
                                numEpisodes = showSeasonInfo.seasons.sumOf { it.episodes.size },
                                seasonLabels = showSeasonInfo.seasons.map { it.name },
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
}

package com.tally.app.data.remote

import com.tally.app.data.remote.api.SimklApiService
import com.tally.app.data.remote.model.MovieDetail
import com.tally.app.data.remote.model.SearchResult
import com.tally.app.data.remote.model.TvEpisode
import com.tally.app.data.remote.model.TvShowDetail
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimklRepository @Inject constructor(
    private val api: SimklApiService,
    private val json: Json,
) {

    suspend fun searchMovies(query: String, limit: Int = 20): List<SearchResult> {
        return api.search("movie", query, limit)
    }

    suspend fun searchTvShows(query: String, limit: Int = 20): List<SearchResult> {
        return api.search("tv", query, limit)
    }

    suspend fun getMovieDetails(simklId: Int): MovieDetail? {
        val result = api.getMovieDetailsRaw(simklId)
        if (result is JsonArray && result.isEmpty()) return null
        return json.decodeFromJsonElement<MovieDetail>(result)
    }

    suspend fun getTvShowDetails(simklId: Int): TvShowDetail? {
        val result = api.getTvShowDetailsRaw(simklId)
        if (result is JsonArray && result.isEmpty()) return null
        return json.decodeFromJsonElement<TvShowDetail>(result)
    }

    suspend fun getTvEpisodes(simklId: Int): List<TvEpisode> {
        return api.getTvEpisodes(simklId)
    }
}

package com.tally.app.data.remote

import com.tally.app.data.remote.api.TmdbApiService
import com.tally.app.data.remote.model.TmdbEpisode
import com.tally.app.data.remote.model.TmdbMovieDetail
import com.tally.app.data.remote.model.TmdbSearchResult
import com.tally.app.data.remote.model.TmdbSearchResponse
import com.tally.app.data.remote.model.TmdbSeasonResponse
import com.tally.app.data.remote.model.TmdbTvShowDetail
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepository @Inject constructor(
    private val api: TmdbApiService,
    private val json: Json,
) {

    suspend fun search(query: String): List<TmdbSearchResult> {
        val body = api.proxy(
            path = "search/multi",
            params = mapOf("query" to query, "language" to "en-US"),
        )
        val response = json.decodeFromString<TmdbSearchResponse>(body.string())
        return response.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
    }

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetail? {
        val body = api.proxy(path = "movie/$movieId")
        val text = body.string()
        if (text.isBlank() || text == "null") return null
        return try {
            json.decodeFromString<TmdbMovieDetail>(text)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getTvShowDetails(tvId: Int): TmdbTvShowDetail? {
        val body = api.proxy(path = "tv/$tvId")
        val text = body.string()
        if (text.isBlank() || text == "null") return null
        return try {
            json.decodeFromString<TmdbTvShowDetail>(text)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getSeasonEpisodes(tvId: Int, seasonNumber: Int): List<TmdbEpisode> {
        val body = api.proxy(path = "tv/$tvId/season/$seasonNumber")
        val text = body.string()
        if (text.isBlank() || text == "null") return emptyList()
        return try {
            json.decodeFromString<TmdbSeasonResponse>(text).episodes
        } catch (_: Exception) {
            emptyList()
        }
    }
}

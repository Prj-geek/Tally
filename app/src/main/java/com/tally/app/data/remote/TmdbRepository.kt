package com.tally.app.data.remote

import com.tally.app.data.remote.api.TmdbApiService
import com.tally.app.data.remote.model.TmdbEpisode
import com.tally.app.data.remote.model.TmdbEpisodeGroup
import com.tally.app.data.remote.model.TmdbEpisodeGroupDetail
import com.tally.app.data.remote.model.TmdbEpisodeGroupsResponse
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

    // ponytail: single decode helper, replaces copy-paste proxy→read→decode→catch ×5
    private suspend inline fun <reified T> fetch(path: String, params: Map<String, String> = emptyMap()): T? {
        val text = api.proxy(path = path, params = params).string()
        if (text.isBlank() || text == "null") return null
        return try {
            json.decodeFromString<T>(text)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun search(query: String): List<TmdbSearchResult> {
        val response = fetch<TmdbSearchResponse>(
            path = "search/multi",
            params = mapOf("query" to query, "language" to "en-US"),
        ) ?: return emptyList()
        return response.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
    }

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetail? =
        fetch("movie/$movieId")

    suspend fun getTvShowDetails(tvId: Int): TmdbTvShowDetail? =
        fetch("tv/$tvId")

    suspend fun getSeasonEpisodes(tvId: Int, seasonNumber: Int): List<TmdbEpisode> =
        fetch<TmdbSeasonResponse>("tv/$tvId/season/$seasonNumber")?.episodes.orEmpty()

    suspend fun getEpisodeGroups(tvId: Int): List<TmdbEpisodeGroup> =
        fetch<TmdbEpisodeGroupsResponse>("tv/$tvId/episode_groups")?.results.orEmpty()

    suspend fun getEpisodeGroupDetail(groupId: String): TmdbEpisodeGroupDetail? =
        fetch("tv/episode_group/$groupId")
}

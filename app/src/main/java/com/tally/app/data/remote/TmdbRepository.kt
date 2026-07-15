package com.tally.app.data.remote

import com.tally.app.data.remote.api.TmdbApiService
import com.tally.app.data.remote.model.TmdbEpisode
import com.tally.app.data.remote.model.TmdbEpisodeGroup
import com.tally.app.data.remote.model.TmdbEpisodeGroupDetail
import com.tally.app.data.remote.model.TmdbEpisodeGroupsResponse
import com.tally.app.data.remote.model.TmdbFindResponse
import com.tally.app.data.remote.model.TmdbMovieDetail
import com.tally.app.data.remote.model.TmdbSearchResult
import com.tally.app.data.remote.model.TmdbSearchResponse
import com.tally.app.data.remote.model.TmdbSeasonResponse
import com.tally.app.data.remote.model.TmdbTvShowDetail
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import java.io.IOException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepository @Inject constructor(
    private val api: TmdbApiService,
    private val json: Json,
) {

    // ponytail: single decode helper — IO errors (timeout) and JSON errors return null;
    // HTTP errors (4xx/5xx) propagate to the caller so user gets feedback
    private suspend inline fun <reified T> fetch(path: String, params: Map<String, String> = emptyMap()): T? {
        return try {
            val text = api.proxy(path = path, params = params).string()
            if (text.isBlank() || text == "null") null
            else json.decodeFromString<T>(text)
        } catch (_: IOException) {
            null
        } catch (_: SerializationException) {
            null
        } catch (_: HttpException) {
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

    /** Look up a show or movie by its TheTVDB ID via TMDB's /find endpoint. */
    suspend fun findByTvdbId(tvdbId: String, mediaType: String): TmdbSearchResult? {
        val response = fetch<TmdbFindResponse>(
            path = "find/$tvdbId",
            params = mapOf("external_source" to "tvdb_id"),
        ) ?: return null
        return when (mediaType) {
            "movie" -> response.movieResults.firstOrNull()
            "tv" -> response.tvResults.firstOrNull()
            else -> null
        }
    }

    /** Look up a show or movie by its IMDB ID via TMDB's /find endpoint. */
    suspend fun findByImdbId(imdbId: String, mediaType: String): TmdbSearchResult? {
        val response = fetch<TmdbFindResponse>(
            path = "find/$imdbId",
            params = mapOf("external_source" to "imdb_id"),
        ) ?: return null
        return when (mediaType) {
            "movie" -> response.movieResults.firstOrNull()
            "tv" -> response.tvResults.firstOrNull()
            else -> null
        }
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

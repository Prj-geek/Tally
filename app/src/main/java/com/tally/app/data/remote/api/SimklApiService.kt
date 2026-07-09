package com.tally.app.data.remote.api

import com.tally.app.data.remote.model.MovieDetail
import com.tally.app.data.remote.model.SearchResult
import com.tally.app.data.remote.model.TvEpisode
import com.tally.app.data.remote.model.TvShowDetail
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SimklApiService {

    @GET("search/{type}")
    suspend fun search(
        @Path("type") type: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
    ): List<SearchResult>

    @GET("movies/{id}")
    suspend fun getMovieDetailsRaw(@Path("id") simklId: Int): JsonElement

    @GET("tv/{id}")
    suspend fun getTvShowDetailsRaw(@Path("id") simklId: Int): JsonElement

    @GET("tv/episodes/{id}")
    suspend fun getTvEpisodes(@Path("id") simklId: Int): List<TvEpisode>
}

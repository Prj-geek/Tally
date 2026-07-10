package com.tally.app.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface TmdbApiService {

    @GET("tmdb-proxy")
    suspend fun proxy(
        @Query("path") path: String,
        @QueryMap params: Map<String, String> = emptyMap(),
    ): ResponseBody
}

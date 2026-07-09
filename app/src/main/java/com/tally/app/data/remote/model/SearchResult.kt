package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val title: String,
    val year: Int,
    @SerialName("endpoint_type") val endpointType: String,
    val poster: String? = null,
    val ids: SearchResultIds,
    val url: String? = null,
    @SerialName("ep_count") val epCount: Int? = null,
    val rank: Int? = null,
    val status: String? = null,
    val ratings: Ratings? = null,
)

@Serializable
data class SearchResultIds(
    @SerialName("simkl_id") val simklId: Int,
    val slug: String,
    val tmdb: String? = null,
)

@Serializable
data class Ratings(
    val simkl: Rating? = null,
    val imdb: Rating? = null,
)

@Serializable
data class Rating(
    val rating: Double? = null,
    val votes: Int? = null,
)

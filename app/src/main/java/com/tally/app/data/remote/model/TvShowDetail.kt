package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvShowDetail(
    val title: String,
    val year: Int,
    val type: String,
    val ids: TvShowIds,
    val rank: Int? = null,
    val poster: String? = null,
    val fanart: String? = null,
    val overview: String? = null,
    val genres: List<String>? = null,
    val network: String? = null,
    val status: String? = null,
    @SerialName("first_aired") val firstAired: String? = null,
    @SerialName("last_aired") val lastAired: String? = null,
    @SerialName("total_episodes") val totalEpisodes: Int? = null,
    val ratings: Ratings? = null,
)

@Serializable
data class TvShowIds(
    val simkl: Int,
    val slug: String? = null,
    val tmdb: String? = null,
    val imdb: String? = null,
)

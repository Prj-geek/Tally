package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetail(
    val title: String,
    val year: Int,
    val type: String,
    val ids: MovieIds,
    val rank: Int? = null,
    val poster: String? = null,
    val fanart: String? = null,
    val released: String? = null,
    val runtime: Int? = null,
    val director: String? = null,
    val certification: String? = null,
    val overview: String? = null,
    val genres: List<String>? = null,
    val country: String? = null,
    val ratings: Ratings? = null,
)

@Serializable
data class MovieIds(
    val simkl: Int,
    val slug: String? = null,
    val tmdb: String? = null,
    val imdb: String? = null,
)

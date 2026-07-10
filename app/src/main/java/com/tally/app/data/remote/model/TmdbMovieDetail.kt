package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovieDetail(
    val id: Int,
    val title: String,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<TmdbGenre>? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val popularity: Double? = null,
    @SerialName("production_countries") val productionCountries: List<TmdbCountry>? = null,
    val status: String? = null,
    val tagline: String? = null,
)

@Serializable
data class TmdbGenre(
    val id: Int,
    val name: String,
)

@Serializable
data class TmdbCountry(
    @SerialName("iso_3166_1") val iso: String,
    val name: String,
)

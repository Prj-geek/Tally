package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbTvShowDetail(
    val id: Int,
    val name: String,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    val genres: List<TmdbGenre>? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val popularity: Double? = null,
    val status: String? = null,
    val tagline: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    val networks: List<TmdbNetwork>? = null,
    val seasons: List<TmdbSeason>? = null,
)

@Serializable
data class TmdbNetwork(
    val id: Int,
    val name: String,
)

@Serializable
data class TmdbSeason(
    val id: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

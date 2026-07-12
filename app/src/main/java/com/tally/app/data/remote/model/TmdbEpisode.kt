package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbSeasonResponse(
    val id: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    val episodes: List<TmdbEpisode> = emptyList(),
)

@Serializable
data class TmdbEpisode(
    val id: Int,
    val name: String,
    val overview: String? = null,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val runtime: Int? = null,
)

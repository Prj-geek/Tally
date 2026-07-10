package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbEpisodeGroupsResponse(
    val id: Int,
    val results: List<TmdbEpisodeGroup>,
)

@Serializable
data class TmdbEpisodeGroup(
    val id: String,
    val name: String,
    val type: Int,
    @SerialName("episode_count") val episodeCount: Int,
    @SerialName("group_count") val groupCount: Int,
    val description: String? = null,
    val network: String? = null,
)

@Serializable
data class TmdbEpisodeGroupDetail(
    val id: String,
    val name: String,
    val type: Int,
    @SerialName("episode_count") val episodeCount: Int,
    @SerialName("group_count") val groupCount: Int,
    val groups: List<TmdbEpisodeGroupGroup>,
)

@Serializable
data class TmdbEpisodeGroupGroup(
    val id: String,
    val name: String,
    val order: Int,
    val episodes: List<TmdbEpisodeGroupEpisode>,
    val locked: Boolean = false,
)

@Serializable
data class TmdbEpisodeGroupEpisode(
    val id: Int,
    val name: String,
    val overview: String? = null,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("episode_type") val episodeType: String? = null,
    val order: Int = 0,
)

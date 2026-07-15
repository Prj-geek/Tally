package com.tally.app.data.importer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiberatorShow(
    val uuid: String = "",
    val id: LiberatorIds = LiberatorIds(),
    @SerialName("created_at") val createdAt: String = "",
    val title: String = "",
    val status: String = "",
    val seasons: List<LiberatorSeason> = emptyList(),
)

@Serializable
data class LiberatorIds(
    val tvdb: Int? = null,
    val imdb: String? = null,
    val tmdb: Int? = null,
)

@Serializable
data class LiberatorSeason(
    val number: Int = 0,
    val episodes: List<LiberatorEpisode> = emptyList(),
)

@Serializable
data class LiberatorEpisode(
    val id: LiberatorIds = LiberatorIds(),
    val number: Int = 0,
    val special: Boolean = false,
    @SerialName("is_watched") val isWatched: Boolean = false,
    @SerialName("watched_at") val watchedAt: String? = null,
    val rating: Int? = null,
)

@Serializable
data class LiberatorMovie(
    val uuid: String = "",
    val id: LiberatorIds = LiberatorIds(),
    @SerialName("created_at") val createdAt: String = "",
    val title: String = "",
    @SerialName("is_watched") val isWatched: Boolean = false,
    val rating: Int? = null,
)

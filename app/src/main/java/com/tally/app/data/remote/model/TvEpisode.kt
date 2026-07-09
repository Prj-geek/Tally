package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvEpisode(
    val title: String,
    val description: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val type: String,
    val aired: Boolean = false,
    val img: String? = null,
    val date: String? = null,
    val ids: EpisodeIds,
)

@Serializable
data class EpisodeIds(
    @SerialName("simkl_id") val simklId: Int,
)

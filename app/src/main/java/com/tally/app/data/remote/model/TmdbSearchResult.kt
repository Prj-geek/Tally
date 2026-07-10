package com.tally.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbSearchResponse(
    val page: Int = 0,
    val results: List<TmdbSearchResult> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class TmdbSearchResult(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    // movie fields
    val title: String? = null,
    // tv fields
    val name: String? = null,
    // common
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val popularity: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int>? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
) {
    val displayTitle: String get() = title ?: name ?: ""
    val displayYear: Int? get() {
        val date = releaseDate ?: firstAirDate ?: return null
        return date.take(4).toIntOrNull()
    }
}

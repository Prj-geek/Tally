package com.tally.app.data.remote

object TmdbImageUrl {

    private const val IMAGE_BASE = "https://image.tmdb.org/t/p"

    fun poster(posterPath: String?, size: String = "w500"): String? {
        if (posterPath.isNullOrBlank()) return null
        return "$IMAGE_BASE/$size$posterPath"
    }

    fun backdrop(backdropPath: String?, size: String = "w1280"): String? {
        if (backdropPath.isNullOrBlank()) return null
        return "$IMAGE_BASE/$size$backdropPath"
    }

    fun still(stillPath: String?, size: String = "w300"): String? {
        if (stillPath.isNullOrBlank()) return null
        return "$IMAGE_BASE/$size$stillPath"
    }
}

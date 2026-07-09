package com.tally.app.data.remote.model

enum class MediaType(val apiValue: String) {
    MOVIE("movie"),
    TV("tv"),
    ANIME("anime");

    companion object {
        fun fromApiValue(value: String): MediaType? =
            entries.find { it.apiValue == value }
    }
}

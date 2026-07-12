package com.tally.app.data.remote

// ponytail: three wrappers over one private builder — callers stay readable
object TmdbImageUrl {

    private const val BASE = "https://image.tmdb.org/t/p"

    private fun url(path: String?, size: String): String? =
        if (path.isNullOrBlank()) null else "$BASE/$size$path"

    fun poster(path: String?, size: String = "w500") = url(path, size)
    fun backdrop(path: String?, size: String = "w1280") = url(path, size)
    fun still(path: String?, size: String = "w300") = url(path, size)
}

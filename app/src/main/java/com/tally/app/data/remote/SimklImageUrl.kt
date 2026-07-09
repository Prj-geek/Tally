package com.tally.app.data.remote

object SimklImageUrl {

    private const val POSTER_BASE = "https://wsrv.nl/?url=https://simkl.in/posters/%s_m.webp&q=90"
    private const val FANART_BASE = "https://wsrv.nl/?url=https://simkl.in/fanart/%s_m.webp&q=90"

    fun poster(posterPath: String?): String? {
        if (posterPath == null) return null
        return POSTER_BASE.format(posterPath)
    }

    fun fanart(fanartPath: String?): String? {
        if (fanartPath == null) return null
        return FANART_BASE.format(fanartPath)
    }
}

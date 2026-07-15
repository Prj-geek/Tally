package com.tally.app.navigation

object Routes {
    const val SHOWS = "shows"
    const val MOVIES = "movies"
    const val SEARCH = "search"
    const val PROFILE = "profile"
    const val DETAIL = "detail/{mediaType}/{id}"
    const val LIBERATOR_IMPORT = "liberator-import"

    fun detail(mediaType: String, id: Int) = "detail/$mediaType/$id"
}

package com.tally.app.navigation

// Route constants for all screens in the app
// Each screen gets a unique route path used by the navigation system
object Routes {
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val PROFILE = "profile"
    const val SHOW_DETAIL = "show/{showId}"
    const val MOVIE_DETAIL = "movie/{movieId}"

    fun showDetail(showId: String) = "show/$showId"
    fun movieDetail(movieId: String) = "movie/$movieId"
}

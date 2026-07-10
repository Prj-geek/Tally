package com.tally.app.data.remote

import com.tally.app.data.remote.model.TmdbTvShowDetail

object AnimeSeasonCollapseDetector {

    fun isLikelySeasonCollapseCandidate(show: TmdbTvShowDetail): Boolean {
        val isAnime = show.originalLanguage == "ja" &&
                show.genres?.any { it.name == "Animation" } == true
        val hasSuspiciouslyFewSeasons = (show.numberOfSeasons ?: 0) <= 1
        val hasHighEpisodeCount = (show.numberOfEpisodes ?: 0) > 26
        return isAnime && hasSuspiciouslyFewSeasons && hasHighEpisodeCount
    }
}

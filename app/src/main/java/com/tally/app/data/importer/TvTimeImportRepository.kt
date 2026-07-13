package com.tally.app.data.importer

import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import com.tally.app.data.local.entity.WatchHistoryEntity
import com.tally.app.data.local.entity.WatchlistEntity
import com.tally.app.data.remote.TmdbRepository
import com.tally.app.data.remote.model.TmdbSearchResult
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImportProgress {
    data object Parsing : ImportProgress
    data class Matching(val processed: Int, val total: Int, val currentTitle: String) : ImportProgress
    data class Done(val summary: ImportSummary) : ImportProgress
    data class Failed(val message: String) : ImportProgress
}

data class UnmatchedTitle(val title: String, val isShow: Boolean, val reason: String)

data class ImportSummary(
    val showsMatched: Int,
    val showsUnmatched: List<UnmatchedTitle>,
    val moviesMatched: Int,
    val moviesUnmatched: List<UnmatchedTitle>,
    val episodesImported: Int,
    val watchlistedShowsMatched: Int = 0,
    val watchlistedShowsUnmatched: List<UnmatchedTitle> = emptyList(),
    val watchlistedMoviesMatched: Int = 0,
    val watchlistedMoviesUnmatched: List<UnmatchedTitle> = emptyList(),
)

@Singleton
class TvTimeImportRepository @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val watchlistDao: WatchlistDao,
    private val watchHistoryDao: WatchHistoryDao,
) {

    /**
     * @param episodesCsvText  content of tracking-prod-records-v2.csv (episodes)
     * @param moviesCsvText    content of tracking-prod-records.csv, no "-v2" (movies)
     *
     * Resolves each unique show/movie title against TMDB, and writes matched results
     * straight into Room via the existing DAOs (as PENDING_ADD, so the app's normal
     * background sync picks them up and pushes to Supabase automatically).
     */
    suspend fun import(
        userId: String,
        episodesCsvText: String,
        moviesCsvText: String,
        onProgress: (ImportProgress) -> Unit,
    ): ImportSummary {
        onProgress(ImportProgress.Parsing)
        val shows = TvTimeCsvParser.parseEpisodes(episodesCsvText)
        val movies = TvTimeCsvParser.parseMovies(moviesCsvText)
        val watchlistedShows = TvTimeCsvParser.parseWatchlistedShows(episodesCsvText)
        val watchlistedMovies = TvTimeCsvParser.parseWatchlistedMovies(moviesCsvText)

        val totalItems = shows.size + movies.size + watchlistedShows.size + watchlistedMovies.size
        var processed = 0

        var showsMatched = 0
        val showsUnmatched = mutableListOf<UnmatchedTitle>()
        var moviesMatched = 0
        val moviesUnmatched = mutableListOf<UnmatchedTitle>()
        var episodesImported = 0
        var watchlistedShowsMatched = 0
        val watchlistedShowsUnmatched = mutableListOf<UnmatchedTitle>()
        var watchlistedMoviesMatched = 0
        val watchlistedMoviesUnmatched = mutableListOf<UnmatchedTitle>()

        for (show in shows) {
            processed++
            onProgress(ImportProgress.Matching(processed, totalItems, show.seriesName))

            val match = findBestMatch(show.seriesName, year = null, wantMediaType = "tv")
            if (match == null) {
                showsUnmatched += UnmatchedTitle(show.seriesName, isShow = true, reason = "No confident TMDB match")
                continue
            }

            val earliestWatch = show.episodes.minOf { it.watchedAtEpochMillis }
            watchlistDao.upsert(
                WatchlistEntity(
                    userId = userId,
                    tmdbId = match.id.toLong(),
                    mediaType = "tv",
                    title = match.displayTitle,
                    posterPath = match.posterPath,
                    status = "watching",
                    watchedEpisodes = show.episodes.size,
                    addedAt = earliestWatch,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_ADD,
                ),
            )

            for (ep in show.episodes) {
                watchHistoryDao.upsert(
                    WatchHistoryEntity(
                        userId = userId,
                        tmdbId = match.id.toLong(),
                        seasonNum = ep.seasonNumber,
                        episodeNum = ep.episodeNumber,
                        runtime = null,
                        watchedAt = ep.watchedAtEpochMillis,
                        rewatch = ep.isRewatch,
                        syncStatus = SyncStatus.PENDING_ADD,
                    ),
                )
                episodesImported++
            }
            showsMatched++
        }

        for (movie in movies) {
            processed++
            onProgress(ImportProgress.Matching(processed, totalItems, movie.movieName))

            val match = findBestMatch(movie.movieName, year = movie.releaseYear, wantMediaType = "movie")
            if (match == null) {
                moviesUnmatched += UnmatchedTitle(movie.movieName, isShow = false, reason = "No confident TMDB match")
                continue
            }

            watchlistDao.upsert(
                WatchlistEntity(
                    userId = userId,
                    tmdbId = match.id.toLong(),
                    mediaType = "movie",
                    title = match.displayTitle,
                    posterPath = match.posterPath,
                    status = "watched",
                    addedAt = movie.watchedAtEpochMillis,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_ADD,
                ),
            )
            watchHistoryDao.upsert(
                WatchHistoryEntity(
                    userId = userId,
                    tmdbId = match.id.toLong(),
                    seasonNum = null,
                    episodeNum = null,
                    runtime = movie.runtimeMinutes,
                    watchedAt = movie.watchedAtEpochMillis,
                    rewatch = movie.isRewatch,
                    syncStatus = SyncStatus.PENDING_ADD,
                ),
            )
            moviesMatched++
        }

        for (show in watchlistedShows) {
            processed++
            onProgress(ImportProgress.Matching(processed, totalItems, show.seriesName))

            val match = findBestMatch(show.seriesName, year = null, wantMediaType = "tv")
            if (match == null) {
                watchlistedShowsUnmatched += UnmatchedTitle(show.seriesName, isShow = true, reason = "No confident TMDB match")
                continue
            }

            val existing = watchlistDao.get(userId, match.id.toLong())
            if (existing != null) continue

            watchlistDao.upsert(
                WatchlistEntity(
                    userId = userId, tmdbId = match.id.toLong(),
                    mediaType = "tv", title = match.displayTitle,
                    posterPath = match.posterPath,
                    status = "watchlist",
                    addedAt = show.addedAtEpochMillis,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_ADD,
                ),
            )
            watchlistedShowsMatched++
        }

        for (movie in watchlistedMovies) {
            processed++
            onProgress(ImportProgress.Matching(processed, totalItems, movie.movieName))

            val match = findBestMatch(movie.movieName, year = movie.releaseYear, wantMediaType = "movie")
            if (match == null) {
                watchlistedMoviesUnmatched += UnmatchedTitle(movie.movieName, isShow = false, reason = "No confident TMDB match")
                continue
            }

            val existing = watchlistDao.get(userId, match.id.toLong())
            if (existing != null) continue

            watchlistDao.upsert(
                WatchlistEntity(
                    userId = userId, tmdbId = match.id.toLong(),
                    mediaType = "movie", title = match.displayTitle,
                    posterPath = match.posterPath,
                    status = "watchlist",
                    addedAt = movie.addedAtEpochMillis,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_ADD,
                ),
            )
            watchlistedMoviesMatched++
        }

        return ImportSummary(
            showsMatched = showsMatched,
            showsUnmatched = showsUnmatched,
            moviesMatched = moviesMatched,
            moviesUnmatched = moviesUnmatched,
            episodesImported = episodesImported,
            watchlistedShowsMatched = watchlistedShowsMatched,
            watchlistedShowsUnmatched = watchlistedShowsUnmatched,
            watchlistedMoviesMatched = watchlistedMoviesMatched,
            watchlistedMoviesUnmatched = watchlistedMoviesUnmatched,
        )
    }

    /**
     * Simple, conservative matching: prefer an exact (case-insensitive) title match of the
     * right media type; if there's a year hint, prefer the candidate whose year also matches.
     * Deliberately does NOT fall back to "just pick the first result" — a wrong silent match
     * is worse than an item flagged for the user to add manually.
     */
    private suspend fun findBestMatch(
        title: String,
        year: Int?,
        wantMediaType: String,
    ): TmdbSearchResult? {
        val results = try {
            tmdbRepository.search(title)
        } catch (_: Exception) {
            emptyList()
        }.filter { it.mediaType == wantMediaType }

        if (results.isEmpty()) return null

        val normalizedQuery = normalize(title)
        val exactMatches = results.filter { normalize(it.displayTitle) == normalizedQuery }

        return when {
            exactMatches.size == 1 -> exactMatches.first()
            exactMatches.size > 1 && year != null ->
                exactMatches.firstOrNull { it.displayYear == year } ?: exactMatches.first()
            exactMatches.size > 1 -> exactMatches.first()
            results.size == 1 -> results.first()
            year != null -> results.firstOrNull { it.displayYear == year }
            else -> null
        }
    }

    private fun normalize(s: String): String =
        s.lowercase().trim().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ")
}

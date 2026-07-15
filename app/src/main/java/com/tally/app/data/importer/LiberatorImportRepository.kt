package com.tally.app.data.importer

import com.tally.app.data.local.SyncStatus
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import com.tally.app.data.local.entity.WatchHistoryEntity
import com.tally.app.data.local.entity.WatchlistEntity
import com.tally.app.data.remote.TmdbRepository
import com.tally.app.data.remote.model.TmdbSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.coroutineContext

data class ImportProgress(
    val phase: String,
    val current: Int,
    val total: Int,
) {
    val fraction: Float get() = if (total > 0) current.toFloat() / total else 0f
}

data class ImportSummary(
    val showsImported: Int,
    val episodesImported: Int,
    val moviesImported: Int,
    val showsSkipped: List<String>,
    val moviesSkipped: List<String>,
)

data class PreparedShow(
    val tmdbId: Int,
    val title: String,
    val posterPath: String?,
    val genres: String,
    val episodeRuntime: Int?,
    val totalEpisodes: Int,
    val watchedEpisodes: Int,
    val episodes: List<PreparedEpisode>,
)

data class PreparedEpisode(
    val tmdbId: Int,
    val seasonNum: Int,
    val episodeNum: Int,
    val runtime: Int?,
    val watchedAt: Long,
)

data class PreparedMovie(
    val tmdbId: Int,
    val title: String,
    val posterPath: String?,
    val runtime: Int?,
    val genres: String,
    val watchedAt: Long?,
)

data class PreparedImport(
    val shows: List<PreparedShow>,
    val episodes: List<PreparedEpisode>,
    val movies: List<PreparedMovie>,
    val showsSkipped: List<String>,
    val moviesSkipped: List<String>,
)

class LiberatorImportRepository @javax.inject.Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val watchlistDao: WatchlistDao,
    private val watchHistoryDao: WatchHistoryDao,
) {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
    )

    private fun parseTimestamp(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        for (fmt in dateFormats) {
            try { return fmt.parse(raw)?.time ?: continue } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }

    private suspend fun retryFind(lookup: suspend () -> TmdbSearchResult?): TmdbSearchResult? {
        var result = lookup()
        if (result == null) {
            delay(500)
            result = lookup()
        }
        return result
    }

    suspend fun prepareImport(
        shows: List<LiberatorShow>,
        movies: List<LiberatorMovie>,
        onProgress: (ImportProgress) -> Unit,
    ): PreparedImport = withContext(Dispatchers.IO) {
        val totalItems = shows.size + movies.size
        var processed = 0

        val preparedShows = mutableListOf<PreparedShow>()
        val showsSkipped = mutableListOf<String>()

        for (show in shows) {
            coroutineContext.ensureActive()
            val tvdbId = show.id.tvdb
            if (tvdbId == null) {
                showsSkipped += show.title
                processed++
                onProgress(ImportProgress("Matching shows", processed, totalItems))
                continue
            }

            onProgress(ImportProgress("Matching shows", processed, totalItems))
            val result = retryFind { tmdbRepository.findByTvdbId(tvdbId.toString(), "tv") }
            if (result != null) {
                val detail = tmdbRepository.getTvShowDetails(result.id)
                val genres = detail?.genres?.joinToString(",") ?: ""
                val episodeRuntime = detail?.episodeRunTime?.firstOrNull()
                val totalEps = show.seasons.sumOf { s -> s.episodes.count { !it.special } }
                val watchedEps = show.seasons.sumOf { s -> s.episodes.count { it.isWatched && !it.special } }
                preparedShows += PreparedShow(
                    tmdbId = result.id,
                    title = result.displayTitle,
                    posterPath = result.posterPath,
                    genres = genres,
                    episodeRuntime = episodeRuntime,
                    totalEpisodes = totalEps,
                    watchedEpisodes = watchedEps,
                    episodes = show.seasons.flatMap { season ->
                        season.episodes
                            .filter { it.isWatched && !it.special }
                            .map { ep ->
                                PreparedEpisode(
                                    tmdbId = result.id,
                                    seasonNum = season.number,
                                    episodeNum = ep.number,
                                    runtime = episodeRuntime,
                                    watchedAt = parseTimestamp(ep.watchedAt),
                                )
                            }
                    },
                )
            } else {
                showsSkipped += show.title
            }
            processed++
            delay(250)
        }

        val preparedMovies = mutableListOf<PreparedMovie>()
        val moviesSkipped = mutableListOf<String>()

        for (movie in movies) {
            coroutineContext.ensureActive()
            onProgress(ImportProgress("Matching movies", processed, totalItems))

            val tmdbId = movie.id.tmdb
                ?: movie.id.imdb?.takeIf { it != "-1" }
                    ?.let { retryFind { tmdbRepository.findByImdbId(it, "movie") }?.id }
                ?: movie.id.tvdb
                    ?.let { retryFind { tmdbRepository.findByTvdbId(it.toString(), "movie") }?.id }

            if (tmdbId != null) {
                val detail = tmdbRepository.getMovieDetails(tmdbId)
                if (detail != null) {
                    preparedMovies += PreparedMovie(
                        tmdbId = tmdbId,
                        title = detail.title,
                        posterPath = detail.posterPath,
                        runtime = detail.runtime,
                        genres = detail.genres?.joinToString(",") ?: "",
                        watchedAt = if (movie.isWatched) parseTimestamp(movie.createdAt) else null,
                    )
                } else {
                    moviesSkipped += movie.title
                }
            } else {
                moviesSkipped += movie.title
            }
            processed++
            delay(250)
        }

        PreparedImport(
            shows = preparedShows,
            episodes = preparedShows.flatMap { it.episodes },
            movies = preparedMovies,
            showsSkipped = showsSkipped,
            moviesSkipped = moviesSkipped,
        )
    }

    suspend fun applyImport(
        userId: String,
        prepared: PreparedImport,
        onProgress: (ImportProgress) -> Unit,
    ): ImportSummary = withContext(Dispatchers.IO) {
        val total = prepared.shows.size + prepared.episodes.size + prepared.movies.size
        var current = 0
        var episodesImported = 0

        for (show in prepared.shows) {
            coroutineContext.ensureActive()
            onProgress(ImportProgress("Importing shows", current, total))
            watchlistDao.upsert(
                WatchlistEntity(
                    userId = userId,
                    tmdbId = show.tmdbId.toLong(),
                    mediaType = "tv",
                    title = show.title,
                    posterPath = show.posterPath,
                    genres = show.genres,
                    status = if (show.watchedEpisodes >= show.totalEpisodes && show.totalEpisodes > 0) "watched" else "watchlist",
                    totalEpisodes = show.totalEpisodes,
                    watchedEpisodes = show.watchedEpisodes,
                    syncStatus = SyncStatus.PENDING_ADD,
                ),
            )
            current++
        }

        for (ep in prepared.episodes) {
            coroutineContext.ensureActive()
            onProgress(ImportProgress("Importing episodes", current, total))
            val existing = watchHistoryDao.get(userId, ep.tmdbId.toLong(), ep.seasonNum, ep.episodeNum)
            if (existing == null) {
                watchHistoryDao.upsert(
                    WatchHistoryEntity(
                        userId = userId,
                        tmdbId = ep.tmdbId.toLong(),
                        seasonNum = ep.seasonNum,
                        episodeNum = ep.episodeNum,
                        runtime = ep.runtime,
                        watchedAt = ep.watchedAt,
                        syncStatus = SyncStatus.PENDING_ADD,
                    ),
                )
                episodesImported++
            }
            current++
        }

        var moviesImported = 0
        for (movie in prepared.movies) {
            coroutineContext.ensureActive()
            onProgress(ImportProgress("Importing movies", current, total))
            watchlistDao.upsert(
                WatchlistEntity(
                    userId = userId,
                    tmdbId = movie.tmdbId.toLong(),
                    mediaType = "movie",
                    title = movie.title,
                    posterPath = movie.posterPath,
                    genres = movie.genres,
                    status = if (movie.watchedAt != null) "watched" else "watchlist",
                    syncStatus = SyncStatus.PENDING_ADD,
                ),
            )
            if (movie.watchedAt != null) {
                val existing = watchHistoryDao.get(userId, movie.tmdbId.toLong(), null, null)
                if (existing == null) {
                    watchHistoryDao.upsert(
                        WatchHistoryEntity(
                            userId = userId,
                            tmdbId = movie.tmdbId.toLong(),
                            seasonNum = null,
                            episodeNum = null,
                            runtime = movie.runtime,
                            watchedAt = movie.watchedAt,
                            syncStatus = SyncStatus.PENDING_ADD,
                        ),
                    )
                    moviesImported++
                }
            }
            current++
        }

        ImportSummary(
            showsImported = prepared.shows.size,
            episodesImported = episodesImported,
            moviesImported = moviesImported,
            showsSkipped = prepared.showsSkipped,
            moviesSkipped = prepared.moviesSkipped,
        )
    }
}

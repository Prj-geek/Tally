package com.tally.app.data.importer

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parses TV Time's GDPR export. IMPORTANT — episode and movie data live in two
 * DIFFERENT files inside the export zip, confirmed by inspecting a real export:
 *
 *   tracking-prod-records-v2.csv   -> the COMPLETE episode watch/rewatch history.
 *       Row `key` is prefixed "watch-episode-..." or "rewatch-episode-...".
 *       Columns used: s_id (show id), series_name, season_number, episode_number,
 *       created_at ("yyyy-MM-dd HH:mm:ss").
 *       This file has ZERO movie rows — don't use it for movies.
 *
 *   tracking-prod-records.csv (no "-v2")   -> the COMPLETE movie watch/rewatch history,
 *       plus a much sparser/incomplete subset of episode events (do not use its episode
 *       rows for episodes — tracking-prod-records-v2.csv has ~4x more unique episodes
 *       than this file does, confirmed against a real export where TV Time's own
 *       displayed "total episodes watched" stat matched the v2 count almost exactly).
 *       Row `type` column is "watch" | "rewatch" | "follow" | "towatch" | ...
 *       Row `entity_type` column is "movie" | "episode" — filter to entity_type == "movie".
 *       Columns used: movie_name, release_date ("yyyy-MM-dd HH:mm:ss"), runtime (seconds),
 *       created_at.
 *
 * We only care about "watch"/"rewatch" type rows for this first pass — "follow" and
 * "towatch" are a separate future feature (importing the watchlist itself, not "already
 * watched" history).
 *
 * UPDATE: "follow" and "towatch" rows are now also parsed — see parseWatchlistedShows
 * and parseWatchlistedMovies. Followed shows / "to watch" movies get added to Tally's
 * watchlist (status = "watchlist").
 */
object TvTimeCsvParser {

    // -- tracking-prod-records-v2.csv columns (episodes) --
    private const val COL_KEY = "key"
    private const val COL_V2_SERIES_ID = "s_id"
    private const val COL_V2_SERIES_NAME = "series_name"
    private const val COL_V2_SEASON_NUMBER = "season_number"
    private const val COL_V2_EPISODE_NUMBER = "episode_number"
    private const val COL_V2_CREATED_AT = "created_at"

    // -- tracking-prod-records.csv columns (movies) --
    private const val COL_TYPE = "type"
    private const val COL_ENTITY_TYPE = "entity_type"
    private const val COL_MOVIE_NAME = "movie_name"
    private const val COL_RELEASE_DATE = "release_date"
    private const val COL_RUNTIME = "runtime"
    private const val COL_CREATED_AT = "created_at"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    data class GroupedShow(
        val seriesId: String,
        val seriesName: String,
        val episodes: List<ParsedEpisodeWatch>,
    )

    data class ParsedEpisodeWatch(
        val seasonNumber: Int,
        val episodeNumber: Int,
        val watchedAtEpochMillis: Long,
        val isRewatch: Boolean,
    )

    data class GroupedMovie(
        val movieName: String,
        val releaseYear: Int?,
        val runtimeMinutes: Int?,
        val watchedAtEpochMillis: Long,
        val isRewatch: Boolean,
    )

    data class GroupedWatchlistedShow(
        val seriesId: String,
        val seriesName: String,
        val addedAtEpochMillis: Long,
    )

    data class GroupedWatchlistedMovie(
        val movieName: String,
        val releaseYear: Int?,
        val addedAtEpochMillis: Long,
    )

    /** Parse tracking-prod-records-v2.csv -> grouped, deduplicated episode watch data. */
    fun parseEpisodes(csvText: String): List<GroupedShow> {
        val lines = splitCsvLines(csvText)
        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines[0])
        val colIndex = header.withIndex().associate { (i, name) -> name to i }
        fun col(row: List<String>, name: String): String? {
            val idx = colIndex[name] ?: return null
            return row.getOrNull(idx)?.takeIf { it.isNotBlank() }
        }

        data class Raw(
            val seriesId: String,
            val seriesName: String,
            val season: Int,
            val episode: Int,
            val watchedAt: Long,
            val isRewatch: Boolean,
        )

        val raws = mutableListOf<Raw>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val row = parseCsvLine(line)

            val key = col(row, COL_KEY) ?: continue
            val isWatch = key.startsWith("watch-episode")
            val isRewatch = key.startsWith("rewatch-episode")
            if (!isWatch && !isRewatch) continue

            val seriesId = col(row, COL_V2_SERIES_ID) ?: continue
            val seriesName = col(row, COL_V2_SERIES_NAME) ?: continue
            val season = col(row, COL_V2_SEASON_NUMBER)?.toIntOrNull() ?: continue
            val episode = col(row, COL_V2_EPISODE_NUMBER)?.toIntOrNull() ?: continue
            val watchedAt = col(row, COL_V2_CREATED_AT)?.let { parseTimestamp(it) } ?: continue

            raws += Raw(seriesId, seriesName, season, episode, watchedAt, isRewatch)
        }

        val groupedEpisodes = raws
            .groupBy { Triple(it.seriesId, it.season, it.episode) }
            .map { (_, group) ->
                val ep = ParsedEpisodeWatch(
                    seasonNumber = group.first().season,
                    episodeNumber = group.first().episode,
                    watchedAtEpochMillis = group.minOf { it.watchedAt },
                    isRewatch = group.any { it.isRewatch },
                )
                Triple(ep, group.first().seriesId, group.first().seriesName)
            }

        return groupedEpisodes
            .groupBy { it.second } // seriesId
            .map { (seriesId, entries) ->
                GroupedShow(
                    seriesId = seriesId,
                    seriesName = entries.first().third,
                    episodes = entries.map { it.first }
                        .sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })),
                )
            }
            .sortedBy { it.seriesName.lowercase() }
    }

    /** Parse tracking-prod-records.csv (no "-v2") -> grouped, deduplicated movie watch data. */
    fun parseMovies(csvText: String): List<GroupedMovie> {
        val lines = splitCsvLines(csvText)
        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines[0])
        val colIndex = header.withIndex().associate { (i, name) -> name to i }
        fun col(row: List<String>, name: String): String? {
            val idx = colIndex[name] ?: return null
            return row.getOrNull(idx)?.takeIf { it.isNotBlank() }
        }

        data class Raw(
            val movieName: String,
            val releaseYear: Int?,
            val runtimeMinutes: Int?,
            val watchedAt: Long,
            val isRewatch: Boolean,
        )

        val raws = mutableListOf<Raw>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val row = parseCsvLine(line)

            val type = col(row, COL_TYPE) ?: continue
            if (type != "watch" && type != "rewatch") continue
            val entityType = col(row, COL_ENTITY_TYPE) ?: continue
            if (entityType != "movie") continue

            val movieName = col(row, COL_MOVIE_NAME) ?: continue
            val releaseYear = col(row, COL_RELEASE_DATE)?.take(4)?.toIntOrNull()
            val runtimeSeconds = col(row, COL_RUNTIME)?.toIntOrNull()
            val watchedAt = col(row, COL_CREATED_AT)?.let { parseTimestamp(it) } ?: continue
            val isRewatch = type == "rewatch"

            raws += Raw(movieName, releaseYear, runtimeSeconds?.let { it / 60 }, watchedAt, isRewatch)
        }

        return raws
            .groupBy { it.movieName to it.releaseYear }
            .map { (_, group) ->
                GroupedMovie(
                    movieName = group.first().movieName,
                    releaseYear = group.first().releaseYear,
                    runtimeMinutes = group.firstNotNullOfOrNull { it.runtimeMinutes },
                    watchedAtEpochMillis = group.minOf { it.watchedAt },
                    isRewatch = group.any { it.isRewatch },
                )
            }
            .sortedBy { it.movieName.lowercase() }
    }

    /** Parse tracking-prod-records-v2.csv -> followed / to-watch shows. */
    fun parseWatchlistedShows(csvText: String): List<GroupedWatchlistedShow> {
        val lines = splitCsvLines(csvText)
        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines[0])
        val colIndex = header.withIndex().associate { (i, name) -> name to i }
        fun col(row: List<String>, name: String): String? {
            val idx = colIndex[name] ?: return null
            return row.getOrNull(idx)?.takeIf { it.isNotBlank() }
        }

        data class Raw(val seriesId: String, val seriesName: String, val addedAt: Long)

        val raws = mutableListOf<Raw>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val row = parseCsvLine(line)

            val key = col(row, COL_KEY) ?: continue
            if (!key.startsWith("follow-") && !key.startsWith("towatch-")) continue

            val seriesId = col(row, COL_V2_SERIES_ID) ?: continue
            val seriesName = col(row, COL_V2_SERIES_NAME) ?: continue
            val addedAt = col(row, COL_V2_CREATED_AT)?.let { parseTimestamp(it) } ?: continue

            raws += Raw(seriesId, seriesName, addedAt)
        }

        return raws
            .groupBy { it.seriesId }
            .map { (seriesId, group) ->
                GroupedWatchlistedShow(
                    seriesId = seriesId,
                    seriesName = group.first().seriesName,
                    addedAtEpochMillis = group.minOf { it.addedAt },
                )
            }
            .sortedBy { it.seriesName.lowercase() }
    }

    /** Parse tracking-prod-records.csv (no "-v2") -> followed / to-watch movies. */
    fun parseWatchlistedMovies(csvText: String): List<GroupedWatchlistedMovie> {
        val lines = splitCsvLines(csvText)
        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines[0])
        val colIndex = header.withIndex().associate { (i, name) -> name to i }
        fun col(row: List<String>, name: String): String? {
            val idx = colIndex[name] ?: return null
            return row.getOrNull(idx)?.takeIf { it.isNotBlank() }
        }

        data class Raw(val movieName: String, val releaseYear: Int?, val addedAt: Long)

        val raws = mutableListOf<Raw>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val row = parseCsvLine(line)

            val type = col(row, COL_TYPE) ?: continue
            if (type != "follow" && type != "towatch") continue
            val entityType = col(row, COL_ENTITY_TYPE) ?: continue
            if (entityType != "movie") continue

            val movieName = col(row, COL_MOVIE_NAME) ?: continue
            val releaseYear = col(row, COL_RELEASE_DATE)?.take(4)?.toIntOrNull()
            val addedAt = col(row, COL_CREATED_AT)?.let { parseTimestamp(it) } ?: continue

            raws += Raw(movieName, releaseYear, addedAt)
        }

        return raws
            .groupBy { it.movieName to it.releaseYear }
            .map { (_, group) ->
                GroupedWatchlistedMovie(
                    movieName = group.first().movieName,
                    releaseYear = group.first().releaseYear,
                    addedAtEpochMillis = group.minOf { it.addedAt },
                )
            }
            .sortedBy { it.movieName.lowercase() }
    }

    private fun parseTimestamp(raw: String): Long? =
        try {
            dateFormat.parse(raw)?.time
        } catch (_: Exception) {
            null
        }

    /** Splits raw CSV text into logical lines, respecting quoted newlines (rare but possible). */
    private fun splitCsvLines(text: String): List<String> {
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                (c == '\n') && !inQuotes -> {
                    lines += current.toString().trimEnd('\r')
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotBlank()) lines += current.toString().trimEnd('\r')
        return lines
    }

    /** RFC 4180-ish CSV line tokenizer: handles quoted fields, embedded commas, and "" escaped quotes. */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields += current.toString()
        return fields
    }
}

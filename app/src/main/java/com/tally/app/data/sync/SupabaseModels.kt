package com.tally.app.data.sync

import com.tally.app.data.local.entity.WatchlistEntity
import com.tally.app.data.local.entity.WatchHistoryEntity
import com.tally.app.data.local.SyncStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val REMOTE_MOVIE_HISTORY_SEASON = 0
private const val REMOTE_MOVIE_HISTORY_EPISODE = 0

@Serializable
data class SupabaseWatchlistEntry(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("tmdb_id") val tmdbId: Long,
    @SerialName("media_type") val mediaType: String,
    val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    val status: String,
    @SerialName("rewatch_count") val rewatchCount: Int = 0,
    @SerialName("total_episodes") val totalEpisodes: Int = 0,
    @SerialName("watched_episodes") val watchedEpisodes: Int = 0,
    val genres: String = "",
    val visibility: String = "private",
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class SupabaseWatchHistoryEntry(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("tmdb_id") val tmdbId: Long,
    @SerialName("season_num") val seasonNum: Int? = null,
    @SerialName("episode_num") val episodeNum: Int? = null,
    val runtime: Int? = null,
    @SerialName("watched_at") val watchedAt: Long,
    val rewatch: Boolean = false,
)

fun WatchlistEntity.toSupabase() = SupabaseWatchlistEntry(
    id = remoteId,
    userId = userId,
    tmdbId = tmdbId,
    mediaType = mediaType,
    title = title,
    posterPath = posterPath,
    status = status,
    rewatchCount = rewatchCount,
    totalEpisodes = totalEpisodes,
    watchedEpisodes = watchedEpisodes,
    genres = genres,
    visibility = visibility,
    addedAt = addedAt,
    updatedAt = updatedAt,
)

fun WatchHistoryEntity.toSupabase() = SupabaseWatchHistoryEntry(
    id = remoteId,
    userId = userId,
    tmdbId = tmdbId,
    // Remote conflict keys cannot rely on NULL equality for movie history.
    seasonNum = seasonNum ?: REMOTE_MOVIE_HISTORY_SEASON,
    episodeNum = episodeNum ?: REMOTE_MOVIE_HISTORY_EPISODE,
    runtime = runtime,
    watchedAt = watchedAt,
    rewatch = rewatch,
)

fun SupabaseWatchlistEntry.toLocalEntity() = WatchlistEntity(
    id = 0,
    userId = userId,
    tmdbId = tmdbId,
    mediaType = mediaType,
    title = title,
    posterPath = posterPath,
    status = status,
    rewatchCount = rewatchCount,
    totalEpisodes = totalEpisodes,
    watchedEpisodes = watchedEpisodes,
    genres = genres,
    visibility = visibility,
    addedAt = addedAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED,
    remoteId = id,
)

fun SupabaseWatchHistoryEntry.toLocalEntity() = WatchHistoryEntity(
    id = 0,
    userId = userId,
    tmdbId = tmdbId,
    seasonNum = if (seasonNum == REMOTE_MOVIE_HISTORY_SEASON && episodeNum == REMOTE_MOVIE_HISTORY_EPISODE) null else seasonNum,
    episodeNum = if (seasonNum == REMOTE_MOVIE_HISTORY_SEASON && episodeNum == REMOTE_MOVIE_HISTORY_EPISODE) null else episodeNum,
    runtime = runtime,
    watchedAt = watchedAt,
    rewatch = rewatch,
    syncStatus = SyncStatus.SYNCED,
    remoteId = id,
)

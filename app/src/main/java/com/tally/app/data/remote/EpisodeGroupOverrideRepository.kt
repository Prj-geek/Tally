package com.tally.app.data.remote

import android.util.Log
import com.tally.app.data.remote.model.TmdbEpisode
import com.tally.app.data.remote.model.TmdbEpisodeGroup
import com.tally.app.data.remote.model.TmdbEpisodeGroupDetail
import com.tally.app.data.remote.model.TmdbEpisodeGroupEpisode
import com.tally.app.data.remote.model.TmdbEpisodeGroupGroup
import com.tally.app.data.remote.model.TmdbTvShowDetail
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AnimeEpisodeGroupOverride(
    @SerialName("tmdb_show_id") val tmdbShowId: Int,
    @SerialName("episode_group_id") val episodeGroupId: String,
    @SerialName("group_name") val groupName: String,
    val verified: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

data class EpisodeGroupProposalResult(
    val inserted: Boolean,
    val alreadyExists: Boolean,
    val groupName: String? = null,
)

@Singleton
class EpisodeGroupOverrideRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val tmdbRepository: TmdbRepository,
) {

    companion object {
        private const val TAG = "EpisodeGroupOverride"
        private val PREFERRED_GROUP_TYPES = setOf(6, 7)
        private val PREFERRED_NAME_KEYWORDS = listOf("season")
        private const val MIN_SUSPICIOUS_SINGLE_SEASON_EPISODES = 13
    }

    // ponytail: inlined from AnimeSeasonCollapseDetector — 4 lines, one caller
    private fun isLikelySeasonCollapseCandidate(show: TmdbTvShowDetail): Boolean {
        val isAnime = show.originalLanguage == "ja" &&
                show.genres?.any { it.name == "Animation" } == true
        return isAnime &&
                (show.numberOfSeasons ?: 0) <= 1 &&
                (show.numberOfEpisodes ?: 0) >= MIN_SUSPICIOUS_SINGLE_SEASON_EPISODES
    }

    private fun hasHistorySeasonMismatch(
        show: TmdbTvShowDetail,
        watchedEpisodes: Set<Pair<Int, Int>>,
    ): Boolean {
        if (watchedEpisodes.isEmpty()) return false
        val seasonEpisodeCounts = show.seasons
            ?.filter { it.seasonNumber > 0 }
            ?.associate { it.seasonNumber to it.episodeCount }
            .orEmpty()
        if (seasonEpisodeCounts.isEmpty()) return false
        return watchedEpisodes.any { (season, episode) ->
            val tmdbEpisodeCount = seasonEpisodeCounts[season] ?: return@any true
            episode > tmdbEpisodeCount
        }
    }

    private fun TmdbEpisodeGroup.candidateScore(
        watchedEpisodes: Set<Pair<Int, Int>>,
    ): Int {
        var score = 0
        if (type in PREFERRED_GROUP_TYPES) score += 100
        if (PREFERRED_NAME_KEYWORDS.any { name.contains(it, ignoreCase = true) }) score += 75
        if (watchedEpisodes.isNotEmpty()) {
            val maxWatchedSeason = watchedEpisodes.maxOf { it.first }
            val maxWatchedEpisode = watchedEpisodes.maxOf { it.second }
            if (groupCount >= maxWatchedSeason) score += 50
            if (episodeCount >= watchedEpisodes.size) score += 25
            if (episodeCount >= maxWatchedEpisode) score += 10
        }
        return score
    }

    suspend fun findBestEpisodeGroup(
        show: TmdbTvShowDetail,
        watchedEpisodes: Set<Pair<Int, Int>> = emptySet(),
    ): TmdbEpisodeGroup? {
        val groups = tmdbRepository.getEpisodeGroups(show.id)
        if (groups.isEmpty()) return null

        return groups
            .filter { group ->
                watchedEpisodes.isEmpty() ||
                        group.groupCount >= watchedEpisodes.maxOf { it.first }
            }
            .maxByOrNull { it.candidateScore(watchedEpisodes) }
    }

    suspend fun getVerifiedOverride(tmdbShowId: Int): AnimeEpisodeGroupOverride? {
        return try {
            val results = supabase.from("anime_episode_group_overrides")
                .select {
                    filter {
                        eq("tmdb_show_id", tmdbShowId)
                        eq("verified", true)
                    }
                }
                .decodeList<AnimeEpisodeGroupOverride>()
            results.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load verified episode group override for tmdbShowId=$tmdbShowId", e)
            null
        }
    }

    private suspend fun getAnyOverride(tmdbShowId: Int): AnimeEpisodeGroupOverride? {
        return try {
            val results = supabase.from("anime_episode_group_overrides")
                .select {
                    filter {
                        eq("tmdb_show_id", tmdbShowId)
                    }
                }
                .decodeList<AnimeEpisodeGroupOverride>()
            results.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check existing episode group override for tmdbShowId=$tmdbShowId", e)
            null
        }
    }

    suspend fun insertUnverifiedOverride(override: AnimeEpisodeGroupOverride): EpisodeGroupProposalResult {
        return try {
            if (getAnyOverride(override.tmdbShowId) != null) {
                EpisodeGroupProposalResult(inserted = false, alreadyExists = true)
            } else {
                supabase.from("anime_episode_group_overrides")
                    .insert(override)
                EpisodeGroupProposalResult(
                    inserted = true,
                    alreadyExists = false,
                    groupName = override.groupName,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert unverified episode group override for tmdbShowId=${override.tmdbShowId}", e)
            EpisodeGroupProposalResult(inserted = false, alreadyExists = false)
        }
    }

    suspend fun proposeOverrideForImportedHistory(
        show: TmdbTvShowDetail,
        watchedEpisodes: Set<Pair<Int, Int>>,
    ): EpisodeGroupProposalResult {
        if (!hasHistorySeasonMismatch(show, watchedEpisodes)) {
            return EpisodeGroupProposalResult(inserted = false, alreadyExists = false)
        }
        val bestGroup = findBestEpisodeGroup(show, watchedEpisodes)
            ?: return EpisodeGroupProposalResult(inserted = false, alreadyExists = false)
        return insertUnverifiedOverride(
            AnimeEpisodeGroupOverride(
                tmdbShowId = show.id,
                episodeGroupId = bestGroup.id,
                groupName = bestGroup.name,
                verified = false,
            )
        )
    }

    private suspend fun proposeOverrideForSuspiciousShow(
        show: TmdbTvShowDetail,
    ) {
        if (!isLikelySeasonCollapseCandidate(show)) return
        val bestGroup = findBestEpisodeGroup(show)
        if (bestGroup != null) {
            insertUnverifiedOverride(
                AnimeEpisodeGroupOverride(
                    tmdbShowId = show.id,
                    episodeGroupId = bestGroup.id,
                    groupName = bestGroup.name,
                    verified = false,
                )
            )
        }
    }

    // ponytail: returns TMDB groups directly, no intermediate model layer
    suspend fun getShowSeasons(
        show: TmdbTvShowDetail,
    ): List<TmdbEpisodeGroupGroup> {
        val override = getVerifiedOverride(show.id)

        if (override != null) {
            val detail = tmdbRepository.getEpisodeGroupDetail(override.episodeGroupId)
            if (detail != null) {
                return detail.groups
                    .filter { it.episodes.isNotEmpty() }
                    .sortedWith(
                        compareBy(
                            { it.name.contains("season", ignoreCase = true).not() },
                            { it.order },
                        )
                    )
            }
        }

        proposeOverrideForSuspiciousShow(show)

        return emptyList()
    }
}

fun TmdbEpisodeGroupEpisode.toTmdbEpisode(): TmdbEpisode = TmdbEpisode(
    id = id,
    name = name,
    overview = overview,
    episodeNumber = order + 1,
    seasonNumber = seasonNumber,
    stillPath = stillPath,
    airDate = airDate,
    voteAverage = voteAverage,
)

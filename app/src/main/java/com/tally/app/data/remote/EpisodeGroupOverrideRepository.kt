package com.tally.app.data.remote

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

@Singleton
class EpisodeGroupOverrideRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val tmdbRepository: TmdbRepository,
) {

    companion object {
        private val PREFERRED_GROUP_TYPES = setOf(6, 7)
        private val PREFERRED_NAME_KEYWORDS = listOf("season")
    }

    // ponytail: inlined from AnimeSeasonCollapseDetector — 4 lines, one caller
    private fun isLikelySeasonCollapseCandidate(show: TmdbTvShowDetail): Boolean {
        val isAnime = show.originalLanguage == "ja" &&
                show.genres?.any { it.name == "Animation" } == true
        return isAnime && (show.numberOfSeasons ?: 0) <= 1 && (show.numberOfEpisodes ?: 0) > 26
    }

    suspend fun findBestEpisodeGroup(
        show: TmdbTvShowDetail,
    ): TmdbEpisodeGroup? {
        val groups = tmdbRepository.getEpisodeGroups(show.id)
        if (groups.isEmpty()) return null

        return groups.sortedByDescending { group ->
            var score = 0
            if (group.type in PREFERRED_GROUP_TYPES) score += 100
            if (PREFERRED_NAME_KEYWORDS.any {
                    group.name.contains(it, ignoreCase = true)
                }) score += 50
            score
        }.first()
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
        } catch (_: Exception) {
            null
        }
    }

    suspend fun insertUnverifiedOverride(override: AnimeEpisodeGroupOverride) {
        try {
            supabase.from("anime_episode_group_overrides")
                .insert(override)
        } catch (_: Exception) { }
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

        if (isLikelySeasonCollapseCandidate(show)) {
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

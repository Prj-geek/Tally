package com.tally.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val tmdbId: Long,
    val title: String,
    val mediaType: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val synopsis: String? = null,
    val totalEpisodes: Int? = null,
)

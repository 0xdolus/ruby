package com.ruby.stream.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Convention: episodeId is never null. Movies use this reserved sentinel
// instead of null so a standard (non-partial) unique index on
// (profileId, contentId, episodeId) can enforce one row per playable
// item for BOTH movies and episodes. SQLite treats every NULL as
// distinct from every other NULL, which would otherwise let movie-
// progress rows multiply on every playback tick instead of upserting
// in place — the sentinel avoids that without partial indexes or
// repository-side merge logic.
const val NO_EPISODE = "__MOVIE__"

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["profileId", "contentId", "episodeId"], unique = true)]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val playedAt: Long,
    val episodeId: String = NO_EPISODE
)

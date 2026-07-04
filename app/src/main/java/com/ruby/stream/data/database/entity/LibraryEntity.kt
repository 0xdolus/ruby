package com.ruby.stream.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LibraryEntryType {
    WATCHLIST,
    CONTINUE_WATCHING
}

// Unique index enforces one row per (profile, content, type) so
// @Insert(onConflict = REPLACE) in LibraryDao.upsert behaves as a real
// upsert rather than silently allowing duplicate rows for the same
// title/type pair.
@Entity(
    tableName = "library",
    indices = [Index(value = ["profileId", "contentId", "type"], unique = true)]
)
data class LibraryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val contentId: String,
    val contentType: String,
    val type: LibraryEntryType,
    val title: String,
    val posterUrl: String?,
    val addedAt: Long,
    val updatedAt: Long,
    val playbackPositionMs: Long = 0,
    val durationMs: Long = 0,
    val episodeId: String? = null
)

package com.ruby.stream.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    RESOLVING,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    COMPLETE,
    FAILED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
    val status: DownloadStatus,
    val storageLocation: String,
    val createdAt: Long,
    val updatedAt: Long,
    val episodeId: String? = null,
    val localPath: String? = null,
    val checksum: String? = null,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val verified: Boolean = false,
    val failureReason: String? = null,
    val expiresAt: Long? = null
)

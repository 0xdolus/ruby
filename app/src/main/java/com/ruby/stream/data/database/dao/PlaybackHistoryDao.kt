package com.ruby.stream.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruby.stream.data.database.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history WHERE profileId = :profileId ORDER BY playedAt DESC")
    fun observeAll(profileId: Long): Flow<List<PlaybackHistoryEntity>>

    // Pass NO_EPISODE (from PlaybackHistoryEntity) for movies.
    @Query("SELECT * FROM playback_history WHERE profileId = :profileId AND contentId = :contentId AND episodeId = :episodeId LIMIT 1")
    suspend fun find(profileId: Long, contentId: String, episodeId: String): PlaybackHistoryEntity?

    // REPLACE is a real upsert here because of the unique index on
    // (profileId, contentId, episodeId) declared on PlaybackHistoryEntity.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaybackHistoryEntity): Long

    @Delete
    suspend fun delete(entity: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: Long)
}

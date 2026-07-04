package com.ruby.stream.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruby.stream.data.database.entity.LibraryEntity
import com.ruby.stream.data.database.entity.LibraryEntryType
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library WHERE profileId = :profileId AND type = :type ORDER BY updatedAt DESC")
    fun observeByType(profileId: Long, type: LibraryEntryType): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library WHERE profileId = :profileId AND contentId = :contentId AND type = :type LIMIT 1")
    suspend fun findEntry(profileId: Long, contentId: String, type: LibraryEntryType): LibraryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LibraryEntity): Long

    @Update
    suspend fun update(entity: LibraryEntity)

    @Delete
    suspend fun delete(entity: LibraryEntity)

    @Query("DELETE FROM library WHERE profileId = :profileId AND contentId = :contentId AND type = :type")
    suspend fun deleteEntry(profileId: Long, contentId: String, type: LibraryEntryType)
}

package com.ruby.stream.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruby.stream.data.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY isOwner DESC, createdAt ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun findById(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isOwner = 1 LIMIT 1")
    suspend fun findOwner(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ProfileEntity): Long

    @Update
    suspend fun update(entity: ProfileEntity)

    // Callers must enforce "promote another profile to Owner first" —
    // this DAO does not special-case isOwner deletion protection itself,
    // since that's application-level policy, not a storage constraint.
    @Delete
    suspend fun delete(entity: ProfileEntity)
}

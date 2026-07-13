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

    /**
     * Used by createProfile() (excludingId = 0L, no existing row to
     * exclude — id autogeneration starts at 1L, so this correctly
     * excludes nothing real) and by updateProfile() (excludingId = the
     * profile's own id — see AD-013/Session 11's locked exclusion
     * pattern, so a no-op save of an unchanged name doesn't collide
     * with itself). No default value on excludingId: Room's KSP
     * processor support for Kotlin default parameter values on @Query
     * methods specifically is not reliable enough to depend on, so both
     * call sites pass it explicitly instead. This mirrors, rather than
     * relies solely on, the unique index on profiles.name -- the index
     * is the actual data-integrity guarantee; this query lets the
     * repository surface a clean DuplicateNameResult instead of
     * catching a raw SQLiteConstraintException from insert()/update().
     */
    @Query("SELECT EXISTS(SELECT 1 FROM profiles WHERE name = :name AND id != :excludingId)")
    suspend fun existsByName(name: String, excludingId: Long): Boolean

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

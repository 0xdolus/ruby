package com.ruby.stream.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruby.stream.data.database.entity.InstalledAddonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAddonDao {
    @Query("SELECT * FROM installed_addons ORDER BY name ASC")
    fun observeAll(): Flow<List<InstalledAddonEntity>>

    @Query("SELECT * FROM installed_addons WHERE enabled = 1 ORDER BY name ASC")
    suspend fun findEnabled(): List<InstalledAddonEntity>

    @Query("SELECT * FROM installed_addons WHERE manifestUrl = :manifestUrl LIMIT 1")
    suspend fun findByManifestUrl(manifestUrl: String): InstalledAddonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InstalledAddonEntity): Long

    @Update
    suspend fun update(entity: InstalledAddonEntity)

    @Delete
    suspend fun delete(entity: InstalledAddonEntity)
}

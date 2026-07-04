package com.ruby.stream.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AddonHealth {
    HEALTHY,
    TIMEOUT,
    INVALID_MANIFEST,
    DISABLED,
    UNREACHABLE
}

// Unique index on manifestUrl (the add-on's natural identity) so
// @Insert(onConflict = REPLACE) in InstalledAddonDao.upsert behaves as a
// real upsert instead of allowing the same add-on to be installed twice.
@Entity(
    tableName = "installed_addons",
    indices = [Index(value = ["manifestUrl"], unique = true)]
)
data class InstalledAddonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manifestUrl: String,
    val name: String,
    val version: String,
    val health: AddonHealth,
    val enabled: Boolean = true,
    val lastSuccessfulValidation: Long? = null,
    val lastFailureReason: String? = null,
    val lastCheckedAt: Long? = null
)

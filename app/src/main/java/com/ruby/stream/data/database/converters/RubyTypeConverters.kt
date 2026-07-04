package com.ruby.stream.data.database.converters

import androidx.room.TypeConverter
import com.ruby.stream.data.database.entity.AddonHealth
import com.ruby.stream.data.database.entity.DownloadStatus
import com.ruby.stream.data.database.entity.LibraryEntryType

class RubyTypeConverters {
    @TypeConverter
    fun fromLibraryEntryType(value: LibraryEntryType): String = value.name

    @TypeConverter
    fun toLibraryEntryType(value: String): LibraryEntryType = LibraryEntryType.valueOf(value)

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)

    @TypeConverter
    fun fromAddonHealth(value: AddonHealth): String = value.name

    @TypeConverter
    fun toAddonHealth(value: String): AddonHealth = AddonHealth.valueOf(value)
}

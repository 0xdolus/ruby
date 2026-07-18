package com.ruby.stream.di

import android.content.Context
import androidx.room.Room
import com.ruby.stream.data.database.RubyDatabase
import com.ruby.stream.data.database.dao.DownloadDao
import com.ruby.stream.data.database.dao.InstalledAddonDao
import com.ruby.stream.data.database.dao.InstalledCatalogDao
import com.ruby.stream.data.database.dao.LibraryDao
import com.ruby.stream.data.database.dao.PlaybackHistoryDao
import com.ruby.stream.data.database.dao.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PASS 6/8 boundary — provides RubyDatabase itself and every DAO it
 * exposes. Written to close a gap CI surfaced directly (PlayerViewModel
 * requesting PlaybackHistoryDao and ProfileDao with
 * [Dagger/MissingBinding] errors -- no DatabaseModule existed at all
 * before this), not written in isolation -- RepositoryModule.kt/
 * PlayerModule.kt/CoroutineScopeModule.kt/Qualifiers.kt already existed
 * (from a separate session's work, also labeled AD-027) and are left
 * untouched; this file fills exactly the gap they didn't cover.
 *
 * All six DAOs provided now, not just the two CI flagged (
 * PlaybackHistoryDao/ProfileDao) -- LibraryDao and the rest would hit
 * this exact same missing-binding failure the moment any other
 * ViewModel (HomeViewModel, next in PASS 6's order, needs LibraryDao
 * for its Continue Watching/Watchlist sections) requests them. Cheaper
 * to close the whole gap once than to hit it again per-DAO per-future-
 * ViewModel.
 *
 * @Singleton on both the database and every DAO: RubyDatabase is a
 * single on-disk file backing the whole app, not a per-screen resource
 * -- matches Room's own standard guidance (exactly one RoomDatabase
 * instance per process) and this module's sibling modules' existing
 * @Singleton-for-application-lifetime-things convention.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRubyDatabase(@ApplicationContext context: Context): RubyDatabase {
        return Room.databaseBuilder(
            context,
            RubyDatabase::class.java,
            "ruby.db",
        ).build()
    }

    @Provides
    @Singleton
    fun provideLibraryDao(database: RubyDatabase): LibraryDao = database.libraryDao()

    @Provides
    @Singleton
    fun providePlaybackHistoryDao(database: RubyDatabase): PlaybackHistoryDao =
        database.playbackHistoryDao()

    @Provides
    @Singleton
    fun provideDownloadDao(database: RubyDatabase): DownloadDao = database.downloadDao()

    @Provides
    @Singleton
    fun provideProfileDao(database: RubyDatabase): ProfileDao = database.profileDao()

    @Provides
    @Singleton
    fun provideInstalledAddonDao(database: RubyDatabase): InstalledAddonDao =
        database.installedAddonDao()

    @Provides
    @Singleton
    fun provideInstalledCatalogDao(database: RubyDatabase): InstalledCatalogDao =
        database.installedCatalogDao()
}

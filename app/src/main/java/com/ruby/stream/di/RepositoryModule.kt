package com.ruby.stream.di

import com.ruby.stream.core.player.DefaultPlaybackPolicy
import com.ruby.stream.core.player.PlaybackPolicy
import com.ruby.stream.core.streams.RegexStreamLabelAnalyzer
import com.ruby.stream.core.streams.StreamLabelAnalyzer
import com.ruby.stream.feature.player.DefaultPendingPlaybackRepository
import com.ruby.stream.feature.player.PendingPlaybackRepository
import com.ruby.stream.feature.profiles.repository.DefaultProfileRepository
import com.ruby.stream.feature.profiles.repository.ProfileRepository
import com.ruby.stream.feature.settings.DefaultSettingsRepository
import com.ruby.stream.feature.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PASS 6 (AD-027) — activates the interface -> implementation bindings
 * individually deferred to "PASS 8" since as early as Session 4
 * (StreamLabelAnalyzer), whose trigger condition -- "when something
 * actually tries to inject this" -- is now live: DefaultPlaybackPolicy
 * (already committed, pass6:2, confirmed green) already has a real
 * @Inject constructor parameter with no binding, dormant only because
 * nothing has yet asked Hilt to construct one.
 *
 * bindPendingPlaybackRepository added after CI directly surfaced it as
 * a [Dagger/MissingBinding] once PlayerViewModel (a later commit than
 * this module's original version) requested it -- same "dormant until
 * something injects it" trigger condition as everything else in this
 * module, not a new category of binding.
 *
 * Everything here is @Singleton -- application-lifetime services with
 * no per-screen/per-ViewModel lifecycle requirement. PlayerController
 * is DELIBERATELY NOT here -- see PlayerModule.kt: AD-00U locks it as
 * @ViewModelScoped specifically, requiring installation into
 * ViewModelComponent, not SingletonComponent, so it cannot share this
 * module regardless of file organization. PendingPlaybackRepository,
 * by contrast, is correctly @Singleton here despite living in the same
 * feature.player package as PlayerController -- it is deliberately a
 * cross-ViewModel mailbox (StreamSelectionViewModel writes,
 * PlayerViewModel reads), so it must outlive any single ViewModel's
 * scope, unlike PlayerController which must NOT.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: DefaultProfileRepository,
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: DefaultSettingsRepository,
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackPolicy(
        impl: DefaultPlaybackPolicy,
    ): PlaybackPolicy

    @Binds
    @Singleton
    abstract fun bindStreamLabelAnalyzer(
        impl: RegexStreamLabelAnalyzer,
    ): StreamLabelAnalyzer

    @Binds
    @Singleton
    abstract fun bindPendingPlaybackRepository(
        impl: DefaultPendingPlaybackRepository,
    ): PendingPlaybackRepository
}

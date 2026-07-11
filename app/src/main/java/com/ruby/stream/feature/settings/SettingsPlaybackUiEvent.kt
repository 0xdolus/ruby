package com.ruby.stream.feature.settings

/**
 * PASS 5 — user-intent events emitted by SettingsPlayback (AD-014).
 *
 * Every event commits immediately (no batched Save) -- matching the
 * confirmed Netflix mobile precedent and every other preference screen
 * (Appearance, Network, Device, Content Filters). null on either
 * language setter means "clear preference," matching
 * SettingsPlaybackUiState.Content's nullable fields.
 */
sealed interface SettingsPlaybackUiEvent {
    data class AutoplayNextEpisodeChanged(val enabled: Boolean) : SettingsPlaybackUiEvent
    data class PreferredAudioLanguageChanged(val languageCode: String?) : SettingsPlaybackUiEvent
    data class PreferredSubtitleLanguageChanged(val languageCode: String?) : SettingsPlaybackUiEvent
}

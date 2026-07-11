package com.ruby.stream.feature.settings

/**
 * PASS 5 — user-intent events emitted by SettingsAppearance (AD-015).
 *
 * Single event, commits immediately (no batched Save) -- matching
 * every other preference screen (Playback, Network, Device, Content
 * Filters).
 */
sealed interface SettingsAppearanceUiEvent {
    data class ThemeChanged(val theme: AppTheme) : SettingsAppearanceUiEvent
}

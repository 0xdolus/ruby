package com.ruby.stream.feature.settings

/**
 * PASS 5 — user-intent events emitted by SettingsDevice (AD-019).
 *
 * Single event, commits immediately (no batched Save) -- matching
 * every other preference screen (Playback, Appearance, Network,
 * Content Filters).
 */
sealed interface SettingsDeviceUiEvent {
    data class KeepScreenAwakeDuringPlaybackChanged(val enabled: Boolean) : SettingsDeviceUiEvent
}

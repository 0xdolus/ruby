package com.ruby.stream.feature.settings

/**
 * PASS 5 — user-intent events emitted by SettingsNetwork (AD-016).
 *
 * Single event, commits immediately (no batched Save) -- matching
 * every other preference screen (Playback, Appearance, Device, Content
 * Filters).
 */
sealed interface SettingsNetworkUiEvent {
    data class CellularStreamingPolicyChanged(
        val policy: CellularStreamingPolicy,
    ) : SettingsNetworkUiEvent
}

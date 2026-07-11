package com.ruby.stream.feature.settings

/**
 * PASS 5 — user-intent events emitted by SettingsStorage (AD-017).
 *
 * Action-based, not a field-commit pattern like Playback/Appearance/
 * Network -- this screen has no settable preference, only a navigation
 * trigger and a destructive action scoped to rebuildable data only.
 * Database size is informational-only and has no corresponding event
 * (no destructive action is ever exposed for it, per AD-017).
 */
sealed interface SettingsStorageUiEvent {
    data object ManageDownloadsClicked : SettingsStorageUiEvent
    data object ClearCacheClicked : SettingsStorageUiEvent
}

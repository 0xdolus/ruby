package com.ruby.stream.feature.settings

/** PASS 5 — user-intent events emitted by the SettingsHome shell. */
sealed interface SettingsHomeUiEvent {
    data class SectionClicked(val id: SettingsSectionId) : SettingsHomeUiEvent
}

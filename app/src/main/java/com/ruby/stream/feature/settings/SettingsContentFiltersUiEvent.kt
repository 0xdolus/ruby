package com.ruby.stream.feature.settings

import com.ruby.stream.data.database.entity.ProfileType

/**
 * PASS 5 — user-intent events emitted by SettingsContentFilters
 * (AD-021).
 *
 * Both events commit immediately (no batched Save) -- matching every
 * other preference screen (Playback, Appearance, Network, Device).
 * ContentRatingLevelChanged accepts a nullable String, matching
 * SettingsContentFiltersUiState.Content's nullable contentRatingLevel
 * field (null = no rating restriction set).
 */
sealed interface SettingsContentFiltersUiEvent {
    data class ProfileTypeChanged(val profileType: ProfileType) : SettingsContentFiltersUiEvent
    data class ContentRatingLevelChanged(val ratingLevel: String?) : SettingsContentFiltersUiEvent
}

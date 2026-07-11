package com.ruby.stream.feature.settings

/**
 * PASS 5 — SettingsPlayback's state (Session 12, AD-014).
 *
 * Scoped by re-checking PASS 4's actual PlayerController/PlaybackModels
 * directly before modeling: Stremio streams are ALREADY-RESOLVED,
 * fixed-quality single URLs chosen once at Stream Selection time -- there
 * is no adaptive-bitrate ladder for a "preferred quality" setting to
 * control, unlike Netflix's server-side transcoding model. Netflix's own
 * "Data usage per screen" tier setting therefore has NO ARCHITECTURAL
 * EQUIVALENT in Ruby and is deliberately excluded here, not merely
 * deprioritized.
 *
 * Playback SPEED is also deliberately absent: confirmed Netflix's own
 * speed control is a PER-SESSION IN-PLAYER control chosen from the
 * in-player menu while watching, not a stored Settings-screen default --
 * Ruby's PlayerController.setPlaybackSpeed() already matches that
 * precedent exactly, and a stored "default speed" preference would be a
 * speculative Ruby-only addition with nothing real behind it.
 *
 * LOCKED fields: autoplay-next-episode (direct Netflix mobile precedent)
 * and preferred audio/subtitle language (Netflix's own profile-scoped
 * language defaults, language ONLY -- font/color/size belongs to a
 * separate future "Subtitle Appearance" concept, not bundled here).
 * preferredAudioLanguage/preferredSubtitleLanguage are plain nullable
 * BCP-47 codes (e.g. "en", "es") rather than an enum: no Language type
 * exists anywhere in the repo yet, and SettingsRepository/DataStore
 * itself is still an owed dependency (see SOT owed items) -- inventing an
 * enum ahead of a real backing store would be speculative. null means "no
 * preference set," matching PlayerController's expected fallback
 * behavior (fall back to the stream's own default track, no error --
 * PASS 6 runtime work, not resolved by this UiState).
 *
 * Loading earned (reads persisted preferences from SettingsRepository/
 * DataStore at entry, per AD-015's generalized rule). No canSave/
 * canSubmit -- unlike ProfileEditor/ChangePin/CreateProfile, this is NOT
 * a batched-submit workflow; each field commits immediately on
 * interaction, matching the confirmed Netflix mobile precedent and every
 * other preference screen (Appearance, Network, Device, Content
 * Filters).
 */
sealed interface SettingsPlaybackUiState {
    data object Loading : SettingsPlaybackUiState

    data class Content(
        val autoplayNextEpisode: Boolean,
        val preferredAudioLanguage: String?,
        val preferredSubtitleLanguage: String?,
    ) : SettingsPlaybackUiState
}

package com.ruby.stream.feature.settings

/**
 * PASS 5 — SettingsDevice's state (Session 14, AD-019).
 *
 * Checked PASS 4's actual PlayerController/DefaultPlayerController
 * directly before scoping (not assumed): confirmed ExoPlayer.Builder
 * uses fully DEFAULT decoder selection -- zero decoder-preference,
 * hardware-acceleration-toggle, or diagnostic-exposure code exists
 * anywhere in PlayerController. Media3 picks the decoder automatically;
 * Ruby has never overridden or exposed that choice. REJECTED a
 * decoder-preference/diagnostics setting entirely -- the FOURTH
 * independent validation of "a setting should only exist if Ruby owns
 * the subsystem it configures" (after Playback's quality tiers,
 * Network's torrent tuning, Storage's cache quotas).
 *
 * Also confirmed: no keep-screen-awake, orientation, or PiP handling
 * exists anywhere in PASS 4 -- PlayerController is purely an
 * ExoPlayer/PlaybackState wrapper with no Android Window/Activity-level
 * concerns at all. Unlike the decoder case, this is NOT "Ruby chose not
 * to expose it" -- it is genuinely UNBUILT platform-level behavior
 * sitting one layer up from PlayerController (PASS 7's Player screen,
 * not PASS 4's engine).
 *
 * DECISION MATRIX: keep screen awake during playback -> INCLUDE
 * (standard Android FLAG_KEEP_SCREEN_ON behavior Ruby can legitimately
 * own at the UI layer); Picture-in-Picture -> DEFER, not rejected (needs
 * real lifecycle/UX work, no evidence it's planned); orientation lock ->
 * DEFER, not rejected (cascades through the app, no established
 * fullscreen/video UX yet to govern); hardware decoder preference ->
 * REJECT (Media3 owns decoder selection); decoder diagnostics -> REJECT
 * for Device, redirect to SettingsAbout if ever exposed at all
 * (developer/diagnostic information, not user-controlled behavior).
 *
 * A one-setting screen is the CORRECT output here, not a shortcoming --
 * it accurately reflects what Ruby's PASS 4 engine actually exposes
 * today. Padding this screen with speculative options would be the
 * actual design flaw, not the small screen itself.
 *
 * IMPLEMENTATION NOTE (Deferred Decision, not resolved here):
 * keepScreenAwakeDuringPlayback is a device-wide PREFERENCE (stored via
 * SettingsRepository/DataStore), but its EFFECT only applies during an
 * active Player session -- the actual FLAG_KEEP_SCREEN_ON call happens
 * in PASS 7's Player screen reading this preference, NOT in
 * PlayerController (PASS 4) itself.
 *
 * Backed by SettingsRepository (AD-016's consolidation, NOT a new
 * repository). Loading earned per AD-015's rule, live-reactive after
 * first emission, immediate-commit on toggle (no canSave -- not a
 * workflow, matching Appearance/Network's pattern).
 */
sealed interface SettingsDeviceUiState {
    data object Loading : SettingsDeviceUiState

    data class Content(
        val keepScreenAwakeDuringPlayback: Boolean,
    ) : SettingsDeviceUiState
}

package com.ruby.stream.feature.settings

/**
 * PASS 5 — SettingsAppearance's state (Session 13, AD-015).
 *
 * Deliberately NOT researched against Netflix precedent, unlike
 * Playback/Content Filters -- Appearance is an application-shell
 * concern, not a streaming-domain concern, so there is no mature
 * streaming-service precedent worth emulating.
 *
 * OWNERSHIP: device-scoped, not profile-scoped. Governing test: does
 * this setting describe the INSTALLATION, or the PERSON currently
 * watching? Theme describes the app shell itself -- switching profiles
 * on a shared device should NOT change how the app looks. This is the
 * FIRST screen backed by DataStore (via SettingsRepository) rather than
 * Room, per the newly locked device-scoped repository category.
 *
 * SCOPE, deliberately kept small: v1 covers theme only. Explicitly
 * EXCLUDED, not deferred with a trigger: app icon variants, layout
 * density, card size, rail layout, animation speed -- following the
 * same "schema should represent current capabilities, not anticipated
 * ones" principle already applied to contentRatingLevel's original
 * deferral.
 *
 * LOADING/LIVENESS: Loading is earned (constructing initial Content
 * requires awaiting SettingsRepository's DataStore-backed theme Flow --
 * the generalized rule from AD-015 applies to any suspendable source,
 * not just Room). This screen is also LIVE, not a snapshot: the
 * ViewModel collects an observable Flow, so Loading represents only the
 * period before the first successful emission -- every subsequent
 * emission replaces Content's value directly and never regresses back
 * to Loading.
 *
 * No canSave/canSubmit -- immediate-commit on change, matching every
 * other preference screen (Playback, Network, Device, Content Filters).
 */
enum class AppTheme { SYSTEM, LIGHT, DARK }

sealed interface SettingsAppearanceUiState {
    data object Loading : SettingsAppearanceUiState

    data class Content(
        val theme: AppTheme,
    ) : SettingsAppearanceUiState
}

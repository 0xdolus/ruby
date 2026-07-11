package com.ruby.stream.feature.settings

/**
 * PASS 5 — SettingsHome's shell state (Session 7).
 *
 * Modeled directly against Netflix's real mobile settings architecture
 * (researched, not assumed): Netflix's "App Settings" shell is a
 * genuinely STATIC list with no live data, no counts, no summaries --
 * everything people think of as "Netflix settings" (PIN, language,
 * maturity level, playback, notifications) is actually reached only
 * after selecting a profile, and the shell itself shows NO
 * profile-identity header and NO cross-profile aggregate data.
 *
 * LOCKED: zero data-fetching, zero Loading state -- UiState is just a
 * static list of navigation rows. The one piece of "live-ish" data the
 * shell needs is a plain boolean read (the current profile's isOwner
 * flag, already on ProfileEntity), used to conditionally show/hide
 * Owner-gated rows (Add-ons, Storage, Device) -- this is a plain field
 * read, not a suspending fetch, so it does not earn a Loading state
 * under AD-015's generalized rule.
 */
data class SettingsHomeUiState(
    val sections: List<SettingsSection>,
)

/**
 * One navigation row on the shell. destination is a plain identifier
 * the ViewModel/NavGraph resolves to a real route -- kept decoupled
 * from Routes.kt (PASS 1) so this UI-layer model doesn't need to
 * import navigation internals directly.
 */
data class SettingsSection(
    val id: SettingsSectionId,
    val title: String,
    val visible: Boolean,
)

/**
 * Every Settings sub-destination reachable from the shell. Add-ons,
 * Storage, and Device are Owner-gated (visible only when the current
 * profile's isOwner == true) -- matches Routes.kt's existing
 * Owner-gating comments.
 */
enum class SettingsSectionId {
    PROFILE,
    PLAYBACK,
    APPEARANCE,
    NETWORK,
    STORAGE,
    ADDONS,
    DEVICE,
    CONTENT_FILTERS,
    NOTIFICATIONS,
    ABOUT,
}

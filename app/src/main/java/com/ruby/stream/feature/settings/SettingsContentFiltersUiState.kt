package com.ruby.stream.feature.settings

import com.ruby.stream.data.database.entity.ProfileType

/**
 * PASS 5 — SettingsContentFilters's state (Session 15, AD-021 — the
 * last of the eleven Settings sub-screens).
 *
 * Confirmed against the actual repo before designing: familyFriendly/
 * visibleToKids are both stored on their entities, but NOTHING anywhere
 * in the codebase reads or filters by them yet -- no DAO query, no
 * repository method touches either field. Confirms AD-011's own
 * Deferred Decisions entry precisely (the "what can this profile see"
 * filtering logic is genuinely unbuilt) and is the deciding fact for
 * this screen's scope.
 *
 * INFORMATION-ARCHITECTURE FORK, resolved in favor of the THIN option:
 * does Content Filters own only profile-level rating/type, or does it
 * become the single home for ALL kids-safety configuration (spanning
 * ProfileEntity, InstalledAddonEntity, InstalledCatalogEntity)? Resolved
 * THIN, for three reasons:
 * 1. familyFriendly is already correctly placed on SettingsAddons
 *    (AD-018) -- it is a property of the ADD-ON itself (an Owner trust
 *    judgment, independent of any profile), not a profile preference.
 *    Moving it here would mean managing one add-on's trust level from
 *    two different screens depending on entry point -- real
 *    duplication, not simplification.
 * 2. visibleToKids is per-CATALOG, and an add-on can have many catalogs
 *    (the "Disney Kids vs. Disney Marvel" case from AD-011). Does not
 *    fit as an inline list on a profile-scoped screen. DEFERRED as a
 *    NEW destination: AddonCatalogVisibility -- reached by drilling into
 *    a specific add-on from SettingsAddons, not from here.
 * 3. This leaves Content Filters with exactly what is genuinely
 *    profile-scoped: contentRatingLevel and profileType, both already
 *    living on ProfileEntity, both already read/writable via
 *    ProfileDao. No cross-repository complexity is actually needed BY
 *    THIS SPECIFIC SCREEN.
 *
 * Loading earned (reads the active profile from Room at entry, per
 * AD-015's generalized rule). Immediate-commit on change -- matching
 * every other preference screen (Appearance, Network, Device); no
 * canSave. Backed entirely by ProfileRepository, extended with two new
 * thin methods (setProfileType, setContentRatingLevel) -- same shape as
 * every prior ProfileRepository extension (PIN, recovery phrase).
 */
sealed interface SettingsContentFiltersUiState {
    data object Loading : SettingsContentFiltersUiState

    data class Content(
        val profileType: ProfileType,
        val contentRatingLevel: String?,
        val availableRatingLevels: List<String>,
    ) : SettingsContentFiltersUiState
}

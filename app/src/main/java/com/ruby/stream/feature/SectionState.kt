package com.ruby.stream.feature

/**
 * PASS 5 — shared per-section loading state, locked AD-006.
 *
 * Used by any screen composed of multiple independently-loading regions
 * (Home's rails, Library's tabs, Search results) rather than each
 * screen inventing its own Loading/Content/Empty/Error wrapper. Lives
 * at feature/ root (not inside any single feature package) because it
 * is genuinely shared across features, not owned by one of them --
 * same reasoning as core/ hosting cross-feature domain types, except
 * this is a UI-layer concept, so it does not belong in core/ either.
 *
 * Empty and Error are deliberately never conflated: Empty means a
 * successful request that legitimately returned zero items; Error
 * means the request itself failed. This distinction matters for
 * retry/analytics/caching behavior, not just rendering -- collapsing
 * them would lose real information the ViewModel already has.
 *
 * Models data availability ONLY. Retry/refresh/pagination are separate
 * ViewModel-exposed functions/events, never embedded in this type.
 */
sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>
    data class Success<T>(val data: T) : SectionState<T>
    data object Empty : SectionState<Nothing>
    data class Error(val error: SectionError) : SectionState<Nothing>
}

/**
 * Flat category set for section-level failures, never a raw message
 * string -- same reasoning as PlaybackError (AD-00S): PASS 7 branches
 * on category (retry button, offline messaging), not on message text.
 */
enum class SectionError {
    NETWORK,
    TIMEOUT,
    UNAUTHORIZED,
    UNKNOWN,
}

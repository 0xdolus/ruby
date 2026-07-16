package com.ruby.stream.feature.player

import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 6 (Session 27, AD-026) — a small, in-memory, one-shot handoff
 * between StreamSelectionViewModel and PlayerViewModel.
 *
 * PROBLEM THIS SOLVES: Player's nav route (Routes.Player, PASS 1,
 * already committed) carries only contentId/episodeId -- deliberately
 * NOT a raw stream URL, since navigation should identify destinations,
 * not transport runtime payloads (URLs can be very long, awkward to
 * URL-encode, and become actively problematic once signed/tokenized
 * URLs are involved). But PlayerController.prepare() needs an actual
 * resolved URL, and the only thing that has one is Stream Selection,
 * right after the user taps a candidate. This repository is the
 * narrow bridge for exactly that one value, and nothing else.
 *
 * DELIBERATELY NOT a navigation argument (see above) and DELIBERATELY
 * NOT a shared navigation-scoped ViewModel: a shared ViewModel is the
 * wrong lifetime model for a one-shot command ("play this stream
 * exactly once") -- it would couple two screens through the navigation
 * graph and introduce shared-state lifetime semantics that outlive the
 * single handoff this actually is.
 *
 * EPHEMERAL BY DESIGN, not a gap: in-memory only, never persisted.
 * Its contents are NOT expected to survive process death. Jetpack
 * Navigation can independently recreate the Player destination (with
 * the same contentId/episodeId route arguments) after such a death,
 * even though this repository's in-memory state is gone -- those two
 * mechanisms are not coupled, so "Player always has a pending
 * playback" is not a true invariant across the Android lifecycle.
 * PlayerViewModel treats a missing pending playback as a RECOVERABLE
 * initialization scenario (see PlayerInitializationState.
 * MissingPendingPlayback), not a programmer error to throw on.
 *
 * consume() takes contentId/episodeId and verifies them against the
 * stored request before returning it, rather than a bare no-arg
 * consume(): this prevents a stale handoff from an earlier, abandoned
 * navigation (e.g. user taps Play on title A, backs out quickly, taps
 * Play on title B) from being consumed by the wrong Player instance.
 * Even though this specific race is unlikely, tying the pending
 * playback to the destination's own route arguments makes the handoff
 * deterministic rather than merely probably-fine.
 *
 * One-shot: consume() clears the stored value on read (whether it
 * matched or not), so a second read for the same navigation always
 * returns null -- this is a single-use mailbox, not a cache.
 */

/**
 * title/posterUrl/contentType are carried here for the same reason
 * streamUrl is: PlaybackHistoryEntity (PASS 0B) requires all three for
 * its upsert, but Player's nav route only carries contentId/episodeId,
 * and Title Details/Stream Selection already have this metadata in
 * hand from their own getMeta() call. Re-fetching it from
 * PlayerViewModel would be a wasted round-trip for data the previous
 * screen already resolved, and could theoretically race with the
 * metadata having changed. Same handoff problem this repository
 * already exists to solve, not a second mechanism.
 */
data class PendingPlayback(
    val contentId: String,
    val episodeId: String?,
    val streamUrl: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
)

interface PendingPlaybackRepository {
    /** Called by StreamSelectionViewModel right before navigating to Player. */
    fun set(pending: PendingPlayback)

    /**
     * Called once by PlayerViewModel during initialization. Returns the
     * stored PendingPlayback only if its contentId/episodeId match the
     * ones passed in; returns null otherwise (including simply "nothing
     * was ever set" -- e.g. after process death). Clears the stored
     * value unconditionally on every call, matching the one-shot
     * contract above.
     */
    fun consume(contentId: String, episodeId: String?): PendingPlayback?
}

@Singleton
class DefaultPendingPlaybackRepository @Inject constructor() : PendingPlaybackRepository {

    // Plain field, not a Flow/StateFlow: this is a one-shot command
    // mailbox, not observable state anything should react to over time.
    // @Synchronized because set()/consume() could in principle race
    // across two different ViewModel coroutine scopes; the cost of
    // guarding against that is negligible and removes any doubt.
    private var pending: PendingPlayback? = null

    @Synchronized
    override fun set(pending: PendingPlayback) {
        this.pending = pending
    }

    @Synchronized
    override fun consume(contentId: String, episodeId: String?): PendingPlayback? {
        val current = pending
        pending = null
        return if (current != null &&
            current.contentId == contentId &&
            current.episodeId == episodeId
        ) {
            current
        } else {
            null
        }
    }
}

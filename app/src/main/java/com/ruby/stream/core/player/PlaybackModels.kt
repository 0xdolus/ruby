package com.ruby.stream.core.player

/**
 * PASS 4 — domain-level playback state, emitted by PlayerController.
 *
 * Kept in its own file rather than nested inside PlayerController.kt,
 * matching the convention set by StreamRankingModels.kt in PASS 3:
 * these are domain types future consumers (PASS 6 ViewModel, PASS 7 UI,
 * tests, analytics) will reference directly, and shouldn't need to
 * import the playback engine file to do so.
 *
 * Ended is intentionally distinct from Paused: PASS 6 needs to tell
 * "user paused, offer resume" apart from "playback completed, offer
 * restart" when deciding what to persist and what the UI should show.
 *
 * Preparing is included (buffering's first-time counterpart, entered
 * from prepare() before Media3 has ever reached STATE_READY) since
 * PASS 7 will likely want a distinct first-load skeleton vs. a
 * mid-playback buffering spinner, consistent with the SOT's existing
 * "skeleton loaders for all first-load states" rule.
 */
sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Preparing : PlaybackState
    data object Buffering : PlaybackState

    data class Playing(
        val positionMs: Long,
        val durationMs: Long,
    ) : PlaybackState

    data class Paused(
        val positionMs: Long,
        val durationMs: Long,
    ) : PlaybackState

    data class Ended(
        val durationMs: Long,
    ) : PlaybackState

    data class Error(
        val error: PlaybackError,
    ) : PlaybackState
}

/**
 * Domain-level playback error. PlayerController is the only thing that
 * ever sees a Media3 PlaybackException; everything above it (PASS 6
 * ViewModel, PASS 7 UI) works only with this type, per the "UI never
 * needs to understand Media3 exceptions" decision.
 *
 * Deliberately a flat category set, not a wrapper around the original
 * exception -- PASS 7 needs to branch on category (retry button vs.
 * "device unsupported" vs. DRM messaging), not on message text.
 */
enum class PlaybackError {
    NETWORK,
    SOURCE,
    DECODER,
    UNSUPPORTED_FORMAT,
    DRM,
    TIMEOUT,
    UNKNOWN,
}

/**
 * A selectable audio track, translated from Media3's Tracks/TrackGroup
 * so PASS 6/7 never touch androidx.media3.common.Tracks directly --
 * same reasoning as PlaybackError: the UI branches on plain fields,
 * not on Media3's group/format model.
 *
 * id is session-local ("$groupIndex:$trackIndex"), valid only until
 * the next prepare() call -- see PlayerController.selectAudioTrack.
 */
data class AudioTrack(
    val id: String,
    val language: String?,
    val label: String?,
    val isSelected: Boolean,
)

/** Same contract as AudioTrack, for subtitle/text tracks. */
data class SubtitleTrack(
    val id: String,
    val language: String?,
    val label: String?,
    val isSelected: Boolean,
)

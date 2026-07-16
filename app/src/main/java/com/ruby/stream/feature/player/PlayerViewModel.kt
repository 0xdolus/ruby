package com.ruby.stream.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruby.stream.core.player.PlaybackState
import com.ruby.stream.core.player.PlayerController
import com.ruby.stream.data.database.dao.PlaybackHistoryDao
import com.ruby.stream.data.database.entity.NO_EPISODE
import com.ruby.stream.data.database.entity.PlaybackHistoryEntity
import com.ruby.stream.feature.profiles.repository.ProfileRepository
import com.ruby.stream.navigation.NavArguments
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PASS 6 (Session 27) — the first ViewModel generated in this pass.
 * Maps PendingPlaybackRepository + PlayerController + PlaybackHistoryDao
 * into PlayerUiState, and PlayerUiEvent into calls on those same
 * dependencies. Every field/case referenced here traces back to an
 * already-locked PASS 4/PASS 5/AD-026 contract -- nothing here invents
 * new UI-facing shape.
 *
 * SAVE CADENCE (a judgment call, not pre-specified anywhere in the SOT
 * -- flagged here rather than silently decided): position is persisted
 * to PlaybackHistoryDao every SAVE_INTERVAL_TICKS engine ticks (see
 * PlayerController's 500ms poll interval), i.e. roughly every 5
 * seconds, plus once immediately on PlaybackState.Paused/Ended so a
 * pause is never lost to the interval. This avoids writing to Room on
 * every single 500ms tick while still keeping resume position
 * reasonably fresh.
 *
 * RESUME-PROMPT THRESHOLD (also a judgment call, also flagged): a prior
 * position is only offered as a resume prompt if it's between 2% and
 * 95% of duration -- below 2% is indistinguishable from "barely
 * started, not worth resuming"; above 95% is close enough to finished
 * that re-starting from the beginning is the more useful default.
 * Revisit if this ever needs to be user-configurable.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playerController: PlayerController,
    private val pendingPlaybackRepository: PendingPlaybackRepository,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val contentId: String =
        checkNotNull(savedStateHandle[NavArguments.CONTENT_ID]) {
            "PlayerViewModel requires ${NavArguments.CONTENT_ID} -- Routes.Player always supplies it."
        }
    private val episodeId: String? = savedStateHandle[NavArguments.EPISODE_ID]

    // NO_EPISODE sentinel (PlaybackHistoryEntity's own convention) so
    // movies and episodes share one lookup/upsert path -- see that
    // entity's doc comment for why a literal null can't be used here.
    private val historyEpisodeId: String = episodeId ?: NO_EPISODE

    private var pendingMetadata: PendingPlayback? = null
    private var ticksSinceLastSave = 0
    private var activeProfileId: Long? = null

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            initialization = PlayerInitializationState.Initializing,
            playback = PlaybackState.Idle,
            overlay = PlayerOverlay.None,
            availableAudioTracks = emptyList(),
            availableSubtitleTracks = emptyList(),
            playbackSpeed = 1.0f,
            resumePrompt = null,
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            activeProfileId = profileRepository.observeActiveProfile().first()?.id

            val pending = pendingPlaybackRepository.consume(contentId, episodeId)
            if (pending == null) {
                _uiState.update {
                    it.copy(initialization = PlayerInitializationState.MissingPendingPlayback)
                }
                return@launch
            }
            pendingMetadata = pending

            val resumePrompt = activeProfileId?.let { profileId ->
                playbackHistoryDao.find(profileId, contentId, historyEpisodeId)
            }?.let { history ->
                val fraction = if (history.durationMs > 0) {
                    history.positionMs.toFloat() / history.durationMs
                } else 0f
                if (fraction in RESUME_MIN_FRACTION..RESUME_MAX_FRACTION) {
                    ResumePrompt(positionMs = history.positionMs, durationMs = history.durationMs)
                } else null
            }

            _uiState.update {
                it.copy(
                    initialization = PlayerInitializationState.Ready,
                    resumePrompt = resumePrompt,
                )
            }

            // Buffering starts immediately; playback only actually
            // begins once the user resolves (or there is no) resume
            // prompt -- see onEvent(ResumeConfirmed/ResumeDismissed).
            playerController.prepare(pending.streamUrl)
            if (resumePrompt == null) {
                playerController.play()
            }

            observePlaybackState()
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            playerController.playbackState.collect { state ->
                _uiState.update { it.copy(playback = state) }

                when (state) {
                    is PlaybackState.Playing -> maybeSavePosition(state.positionMs, state.durationMs)
                    is PlaybackState.Paused -> savePosition(state.positionMs, state.durationMs)
                    is PlaybackState.Ended -> savePosition(state.durationMs, state.durationMs)
                    else -> Unit
                }
            }
        }
    }

    fun onEvent(event: PlayerUiEvent) {
        when (event) {
            PlayerUiEvent.PlayPauseClicked -> {
                val playing = _uiState.value.playback is PlaybackState.Playing
                if (playing) playerController.pause() else playerController.play()
            }

            is PlayerUiEvent.SeekTo -> playerController.seekTo(event.positionMs)

            PlayerUiEvent.SkipForwardClicked -> skipBy(SKIP_INTERVAL_MS)
            PlayerUiEvent.SkipBackwardClicked -> skipBy(-SKIP_INTERVAL_MS)

            is PlayerUiEvent.AudioTrackSelected -> {
                playerController.selectAudioTrack(event.trackId)
                refreshTracks()
            }

            is PlayerUiEvent.SubtitleTrackSelected -> {
                playerController.selectSubtitleTrack(event.trackId)
                refreshTracks()
            }

            is PlayerUiEvent.PlaybackSpeedChanged -> {
                playerController.setPlaybackSpeed(event.speed)
                _uiState.update { it.copy(playbackSpeed = event.speed) }
            }

            is PlayerUiEvent.OverlayRequested -> {
                if (event.overlay is PlayerOverlay.Settings) refreshTracks()
                _uiState.update { it.copy(overlay = event.overlay) }
            }

            PlayerUiEvent.OverlayDismissed ->
                _uiState.update { it.copy(overlay = PlayerOverlay.None) }

            is PlayerUiEvent.EpisodeSelected -> {
                // Playing a different episode is a new pending-playback
                // handoff (a new stream must be resolved), not something
                // this ViewModel can do in place -- out of scope for
                // this pass; PASS 7's Composable navigates back through
                // Stream Selection for the new episodeId. Recorded as a
                // deliberate no-op here rather than silently ignored.
            }

            PlayerUiEvent.ResumeConfirmed -> {
                val prompt = _uiState.value.resumePrompt
                _uiState.update { it.copy(resumePrompt = null) }
                if (prompt != null) playerController.seekTo(prompt.positionMs)
                playerController.play()
            }

            PlayerUiEvent.ResumeDismissed -> {
                _uiState.update { it.copy(resumePrompt = null) }
                playerController.play()
            }

            PlayerUiEvent.RetryClicked -> {
                val pending = pendingMetadata
                if (pending != null) playerController.prepare(pending.streamUrl)
            }
        }
    }

    private fun skipBy(deltaMs: Long) {
        val current = when (val state = _uiState.value.playback) {
            is PlaybackState.Playing -> state.positionMs
            is PlaybackState.Paused -> state.positionMs
            else -> return
        }
        playerController.seekTo((current + deltaMs).coerceAtLeast(0L))
    }

    private fun refreshTracks() {
        _uiState.update {
            it.copy(
                availableAudioTracks = playerController.getAudioTracks(),
                availableSubtitleTracks = playerController.getSubtitleTracks(),
            )
        }
    }

    private fun maybeSavePosition(positionMs: Long, durationMs: Long) {
        ticksSinceLastSave++
        if (ticksSinceLastSave >= SAVE_INTERVAL_TICKS) {
            ticksSinceLastSave = 0
            savePosition(positionMs, durationMs)
        }
    }

    private fun savePosition(positionMs: Long, durationMs: Long) {
        val profileId = activeProfileId ?: return
        val metadata = pendingMetadata ?: return
        viewModelScope.launch {
            playbackHistoryDao.upsert(
                PlaybackHistoryEntity(
                    profileId = profileId,
                    contentId = contentId,
                    contentType = metadata.contentType,
                    title = metadata.title,
                    posterUrl = metadata.posterUrl,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    playedAt = System.currentTimeMillis(),
                    episodeId = historyEpisodeId,
                )
            )
        }
    }

    override fun onCleared() {
        playerController.release()
    }

    private companion object {
        const val SKIP_INTERVAL_MS = 10_000L

        // PlayerController polls every 500ms (PASS 4) -- 10 ticks is
        // roughly every 5 seconds. See class doc for why this interval
        // exists rather than saving on every tick.
        const val SAVE_INTERVAL_TICKS = 10

        const val RESUME_MIN_FRACTION = 0.02f
        const val RESUME_MAX_FRACTION = 0.95f
    }
}

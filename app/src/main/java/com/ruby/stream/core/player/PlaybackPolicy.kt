package com.ruby.stream.core.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.ruby.stream.feature.settings.CellularStreamingPolicy
import com.ruby.stream.feature.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 6 infrastructure (Session 21, AD-022) — a small, reusable
 * domain service answering exactly one question: "given the current
 * settings and current network, may playback begin?"
 *
 * Deliberately NOT part of PlayerController (PASS 4) or
 * SettingsRepository (AD-024). Confirmed against both real interfaces
 * before placing this here: PlayerController's own doc comment
 * explicitly excludes any network/policy/persistence awareness by
 * design; SettingsRepository was locked (AD-016, reaffirmed AD-024) to
 * store POLICY ONLY, deliberately separated from connectivity
 * detection and enforcement. Neither existing layer is the right home,
 * so this is a new, narrow, reusable service rather than an expansion
 * of either.
 *
 * Reusable beyond Stream Selection, the actual justification for a
 * dedicated service over a ViewModel-local check: Next Episode, Retry-
 * another-source, Resume, and Downloads (once designed) are all real,
 * already-anticipated future playback-entry points per PASS 4/PASS 5's
 * own doc comments — each would otherwise duplicate the same Warn/
 * Blocked logic inline. Held to the same "real named use cases, not a
 * speculative just-in-case abstraction" bar AD-016/AD-017/AD-019
 * already applied when REJECTING settings for subsystems Ruby doesn't
 * yet own.
 *
 * evaluate() is a one-shot snapshot check, not a continuous observer --
 * "may playback begin right now" only needs the connectivity state at
 * the moment of the call, so this reads ConnectivityManager's current
 * active network synchronously (via getNetworkCapabilities) rather
 * than registering a NetworkCallback. A continuous subscription would
 * be the wrong tool for a question that is only ever asked at one
 * instant (the moment the user taps a stream) -- Warn/Blocked's
 * consequence is a one-time decision, not an ongoing state the UI
 * needs to keep observing after playback has already started.
 *
 * Contains no UI code and no playback code -- same narrow single-
 * responsibility contract already established for
 * ProfileRepository.verifyPin()/verifyRecoveryPhrase() (AD-010): this
 * answers exactly one question and does not decide what happens next.
 * The caller (e.g. StreamSelectionViewModel) owns what Warn/Blocked
 * actually means for its own screen (see the PASS 5 amendment to
 * StreamSelectionUiState/UiEvent, AD-012).
 */
interface PlaybackPolicy {
    suspend fun evaluate(): PlaybackPermission
}

sealed interface PlaybackPermission {
    /** No restriction applies, or the device is on Wi-Fi/Ethernet. */
    data object Allowed : PlaybackPermission

    /**
     * Policy is WARN and the device is currently on a cellular
     * connection -- the caller must obtain user confirmation before
     * proceeding.
     */
    data object Warn : PlaybackPermission

    /**
     * Policy is WIFI_ONLY and the device is currently on a cellular
     * connection (or has no connection at all) -- the caller must not
     * proceed.
     */
    data object Blocked : PlaybackPermission
}

@Singleton
class DefaultPlaybackPolicy @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : PlaybackPolicy {

    override suspend fun evaluate(): PlaybackPermission {
        return when (val policy = settingsRepository.cellularStreamingPolicy.first()) {
            // ALLOW never needs the network state at all -- no
            // ConnectivityManager call is made unless a restriction
            // could actually apply.
            CellularStreamingPolicy.ALLOW -> PlaybackPermission.Allowed

            CellularStreamingPolicy.WARN ->
                if (isOnWifiOrEthernet()) PlaybackPermission.Allowed else PlaybackPermission.Warn

            CellularStreamingPolicy.WIFI_ONLY ->
                if (isOnWifiOrEthernet()) PlaybackPermission.Allowed else PlaybackPermission.Blocked
        }
    }

    private fun isOnWifiOrEthernet(): Boolean {
        val connectivityManager = context.getSystemService<ConnectivityManager>() ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}

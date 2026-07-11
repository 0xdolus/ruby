package com.ruby.stream.feature.settings

/**
 * PASS 5 — SettingsNetwork's state (Session 13, AD-016).
 *
 * Governing test: "does this configure Ruby's OWN networking behavior,
 * or an external service." Confirmed Ruby owns NO torrent/P2P engine at
 * all -- PASS 2A/2B's add-ons resolve streams, StreamRanker ranks
 * resolved candidates, PlayerController receives an already-playable
 * URL, PASS 7's Media3 plays it. At no point does Ruby run a BitTorrent
 * session, DHT, tracker communication, or piece scheduling, even when a
 * debrid add-on is used (the debrid service does that work remotely).
 *
 * REJECTED, out-of-scope entirely (not merely unimplemented), exactly
 * parallel to Playback's abandoned quality-tier setting (AD-014) having
 * nowhere to attach once Media3-plays-a-supplied-URL was established:
 * max peers, upload/download caps, torrent-piece cache size, tracker
 * timeouts, DHT enable/disable, piece selection, debrid credentials
 * (belong to individual add-ons), proxy settings (would obligate every
 * network client app-wide), VPN detection (Ruby just plays whatever URL
 * resolves).
 *
 * ALSO DEFERRED, not decided here: UpdatePolicy (auto-download add-on
 * updates) belongs to Add-ons' own future design session -- introducing
 * it now would mean inventing an API before understanding the subsystem
 * it configures.
 *
 * LOCKED scope: CellularStreamingPolicy only. SettingsRepository stores
 * POLICY ONLY -- connectivity detection (Wi-Fi vs. cellular, via
 * Android's ConnectivityManager) and policy enforcement are DELIBERATELY
 * SEPARATED from this repository and deferred to PASS 6 (owner of that
 * decision point TBD until the playback flow is modeled in detail).
 *
 * Backed by SettingsRepository (AD-015's AppearanceRepository EXPLICITLY
 * SUPERSEDED, not extended -- ONE SettingsRepository serves every
 * device-wide settings screen, same consolidation principle as
 * ProfileRepository/AddonRepository on the Room side). Loading earned
 * per AD-015's generalized rule -- live Flow, remains reactive after
 * first emission, never regresses to Loading. No canSave/canSubmit --
 * immediate-commit on change, matching every other preference screen.
 */
enum class CellularStreamingPolicy { ALLOW, WARN, WIFI_ONLY }

sealed interface SettingsNetworkUiState {
    data object Loading : SettingsNetworkUiState

    data class Content(
        val cellularStreamingPolicy: CellularStreamingPolicy,
    ) : SettingsNetworkUiState
}

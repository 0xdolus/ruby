package com.ruby.stream.feature.settings

/**
 * PASS 5 — SettingsStorage's state (Session 13, AD-017).
 *
 * Confirmed against the ACTUAL REPO before designing (not assumed):
 * DownloadEntity/DownloadDao already exist, fully designed (per-profile,
 * full status lifecycle RESOLVING/QUEUED/DOWNLOADING/PAUSED/VERIFYING/
 * COMPLETE/FAILED, bytesDownloaded/totalBytes, checksum/verified for
 * integrity-checking torrent-sourced files) -- but feature/library/
 * downloads and core/download are both completely empty, same as every
 * other unbuilt feature folder. Offline downloads are real designed
 * infrastructure, not speculative scope.
 *
 * THREE STORAGE CLASSES, each with a DIFFERENT lifecycle policy:
 * 1. Downloads (user-created data) -- backed by the existing
 *    DownloadEntity. Ruby owns these completely. Deleting one is a real
 *    consequential loss -- a torrent-sourced file may not be trivially
 *    re-obtainable later.
 * 2. Cache (rebuildable) -- posters, background artwork, metadata,
 *    catalog pages, search results, manifest responses, subtitle cache.
 *    Disposable by nature; a single "Clear Cache" is the correct
 *    abstraction, not per-file management.
 * 3. Database (application state) -- the Room database itself.
 *    INFORMATIONAL ONLY -- no destructive action exposed; "clear
 *    database" would effectively be a factory reset and does not belong
 *    on this screen.
 *
 * REUSABLE PRINCIPLE: user-created data and rebuildable data must NEVER
 * share a destructive action -- clearing cache costs the user TIME
 * (re-fetchable); deleting downloads costs the user THE CONTENT ITSELF
 * (may not be recoverable). Collapsing both into one "Clear Everything"
 * button would be a real design mistake, not merely inelegant.
 *
 * EXPLICITLY REJECTED: a cache size LIMIT/quota setting, unless and
 * until Ruby actually implements an eviction algorithm -- the THIRD
 * independent validation of "a setting should only exist if Ruby owns
 * the subsystem it configures" (see AD-016), after Playback's quality
 * tiers and Network's torrent tuning.
 *
 * Storage aggregation is DEVICE-WIDE, across ALL profiles, not just the
 * currently-active one -- SettingsStorage is Owner-gated and storage is
 * a genuinely device-wide physical resource, unlike per-profile
 * preferences; the Owner needs the true total.
 *
 * "Manage Downloads" is EXPLICITLY DEFERRED as its own future
 * destination -- a real list-with-delete-actions screen, a materially
 * different shape than a settings toggle, backed directly by
 * DownloadDao.observeAll(profileId)'s existing reactive Flow.
 * SettingsStorage itself only navigates to it and shows the aggregate
 * size; it does not list or manage individual downloads inline.
 *
 * Loading earned per AD-015's generalized rule -- computing these three
 * aggregate sizes requires real I/O (Room queries, filesystem size
 * calculation), not a synchronous read.
 */
sealed interface SettingsStorageUiState {
    data object Loading : SettingsStorageUiState

    data class Content(
        val downloadSizeBytes: Long,   // aggregated across ALL profiles
        val cacheSizeBytes: Long,
        val databaseSizeBytes: Long,   // informational only
    ) : SettingsStorageUiState
}

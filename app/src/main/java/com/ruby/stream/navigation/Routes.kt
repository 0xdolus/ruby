package com.ruby.stream.navigation

/**
 * PASS 1 — Navigation Skeleton
 *
 * All routes Ruby will ever need, defined up front against the full
 * sitemap (Section 5, RUBY_HANDOFF.md), even though most destinations
 * are placeholders until PASS 7.
 *
 * Structure:
 *  - Top-level routes: profile/auth flow, browse flow, stream resolution,
 *    player.
 *  - homeGraph: nested graph for the bottom-nav section (Home/Search/
 *    New&Hot/Library-tabs).
 *  - settingsGraph: nested graph, one route per subsection (each maps to
 *    its own ViewModel per feature-settings subfolder — no shared
 *    "SettingsDetail(section)" route, see PASS 1 decision log).
 *
 * Explicitly EXCLUDED from routes (per locked decisions):
 *  - "Fetching Streams" — transient loading state on TitleDetails, not
 *    a destination.
 *  - Player's Settings Overlay / Episode List / Playback Error — these
 *    are PlayerOverlay states inside PlayerUiState, not routes.
 *  - PIN Prompt, Delete Download, Network Confirmation, Subtitle
 *    Download — dialogs, not routes.
 */
sealed class Routes(val route: String) {

    // ---- Onboarding / Profile flow ----
    data object CreateOwnerProfile : Routes("create_owner_profile")
    data object ProfilePicker : Routes("profile_picker")
    data object ManageProfiles : Routes("manage_profiles") // Owner only

    // ---- Home graph (bottom nav) ----
    data object HomeGraph : Routes("home_graph")
    data object Home : Routes("home")
    data object Search : Routes("search")
    data object NewAndHot : Routes("new_and_hot")

    // ---- Library graph (nested under Home graph's bottom nav) ----
    data object LibraryGraph : Routes("library_graph")
    data object Watchlist : Routes("watchlist")
    data object ContinueWatching : Routes("continue_watching")
    data object Downloads : Routes("downloads")
    data object History : Routes("history")

    // ---- Title / streams flow ----
    data object TitleDetails : Routes("title_details/{${NavArguments.CONTENT_ID}}") {
        fun createRoute(contentId: String) = "title_details/$contentId"
    }
    data object StreamSelection : Routes("stream_selection/{${NavArguments.CONTENT_ID}}") {
        fun createRoute(contentId: String) = "stream_selection/$contentId"
    }
    data object NoStreams : Routes("no_streams/{${NavArguments.CONTENT_ID}}") {
        fun createRoute(contentId: String) = "no_streams/$contentId"
    }
    data object AddonFailure : Routes("addon_failure/{${NavArguments.CONTENT_ID}}") {
        fun createRoute(contentId: String) = "addon_failure/$contentId"
    }
    data object StreamFetchError : Routes("stream_fetch_error/{${NavArguments.CONTENT_ID}}") {
        fun createRoute(contentId: String) = "stream_fetch_error/$contentId"
    }

    // ---- Player ----
    data object Player : Routes(
        "player/{${NavArguments.CONTENT_ID}}?${NavArguments.EPISODE_ID}={${NavArguments.EPISODE_ID}}"
    ) {
        fun createRoute(contentId: String, episodeId: String? = null): String {
            val base = "player/$contentId"
            return if (episodeId != null) "$base?${NavArguments.EPISODE_ID}=$episodeId" else base
        }
    }

    // ---- Settings graph ----
    data object SettingsGraph : Routes("settings_graph")
    data object SettingsHome : Routes("settings_home")
    data object SettingsPlayback : Routes("settings_playback")
    data object SettingsContentFilters : Routes("settings_content_filters") // placeholder
    data object SettingsProfile : Routes("settings_profile")
    data object SettingsAddons : Routes("settings_addons") // Owner only
    data object SettingsNetwork : Routes("settings_network")
    data object SettingsStorage : Routes("settings_storage") // Owner only
    data object SettingsAppearance : Routes("settings_appearance")
    data object SettingsDevice : Routes("settings_device") // Owner only
    data object SettingsNotifications : Routes("settings_notifications")
    data object SettingsAbout : Routes("settings_about")
}

package com.ruby.stream.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

/**
 * PASS 1 — Navigation Skeleton
 *
 * Wires every route from Routes.kt to a PLACEHOLDER screen. Real screens
 * arrive in PASS 7 — this pass exists solely to lock the navigation
 * shape against the full sitemap before any screen UI is built.
 *
 * Nesting:
 *  - HomeGraph contains Home/Search/NewAndHot + a nested LibraryGraph
 *    (Watchlist/ContinueWatching/Downloads/History), matching the
 *    bottom-nav "My Library" destination containing its own tabs.
 *  - SettingsGraph contains one route per settings subsection.
 *
 * PlayerOverlay states (Settings/EpisodeList/PlaybackError) and dialogs
 * (PIN/DeleteDownload/NetworkConfirmation/SubtitleDownload) are NOT
 * represented here — they live in-screen (PlayerUiState / per-screen
 * dialog state), per the locked PASS 1 decision.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.ProfilePicker.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {

        // ---- Onboarding / Profile flow ----
        composable(Routes.CreateOwnerProfile.route) {
            PlaceholderScreen("Create Owner Profile")
        }
        composable(Routes.ProfilePicker.route) {
            PlaceholderScreen("Profile Picker")
        }
        composable(Routes.ManageProfiles.route) {
            PlaceholderScreen("Manage Profiles (Owner Only)")
        }

        // ---- Home graph (bottom nav) ----
        navigation(
            route = Routes.HomeGraph.route,
            startDestination = Routes.Home.route,
        ) {
            composable(Routes.Home.route) {
                PlaceholderScreen("Home")
            }
            composable(Routes.Search.route) {
                PlaceholderScreen("Search")
            }
            composable(Routes.NewAndHot.route) {
                PlaceholderScreen("New & Hot")
            }

            // ---- Library graph (nested inside Home graph) ----
            navigation(
                route = Routes.LibraryGraph.route,
                startDestination = Routes.Watchlist.route,
            ) {
                composable(Routes.Watchlist.route) {
                    PlaceholderScreen("Watchlist")
                }
                composable(Routes.ContinueWatching.route) {
                    PlaceholderScreen("Continue Watching")
                }
                composable(Routes.Downloads.route) {
                    PlaceholderScreen("Downloads")
                }
                composable(Routes.History.route) {
                    PlaceholderScreen("History")
                }
            }
        }

        // ---- Title / streams flow ----
        composable(
            route = Routes.TitleDetails.route,
            arguments = listOf(navArgument(NavArguments.CONTENT_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen("Title Details")
        }
        composable(
            route = Routes.StreamSelection.route,
            arguments = listOf(navArgument(NavArguments.CONTENT_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen("Stream Selection")
        }
        composable(
            route = Routes.NoStreams.route,
            arguments = listOf(navArgument(NavArguments.CONTENT_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen("No Streams")
        }
        composable(
            route = Routes.AddonFailure.route,
            arguments = listOf(navArgument(NavArguments.CONTENT_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen("Add-on Failure")
        }
        composable(
            route = Routes.StreamFetchError.route,
            arguments = listOf(navArgument(NavArguments.CONTENT_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen("Stream Fetch Error")
        }

        // ---- Player ----
        composable(
            route = Routes.Player.route,
            arguments = listOf(
                navArgument(NavArguments.CONTENT_ID) { type = NavType.StringType },
                navArgument(NavArguments.EPISODE_ID) {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ) {
            PlaceholderScreen("Player")
        }

        // ---- Settings graph ----
        navigation(
            route = Routes.SettingsGraph.route,
            startDestination = Routes.SettingsHome.route,
        ) {
            composable(Routes.SettingsHome.route) {
                PlaceholderScreen("Settings")
            }
            composable(Routes.SettingsPlayback.route) {
                PlaceholderScreen("Settings — Playback")
            }
            composable(Routes.SettingsContentFilters.route) {
                PlaceholderScreen("Settings — Content Filters (placeholder)")
            }
            composable(Routes.SettingsProfile.route) {
                PlaceholderScreen("Settings — Profile")
            }
            composable(Routes.SettingsAddons.route) {
                PlaceholderScreen("Settings — Add-ons (Owner Only)")
            }
            composable(Routes.SettingsNetwork.route) {
                PlaceholderScreen("Settings — Network")
            }
            composable(Routes.SettingsStorage.route) {
                PlaceholderScreen("Settings — Storage (Owner Only)")
            }
            composable(Routes.SettingsAppearance.route) {
                PlaceholderScreen("Settings — Appearance")
            }
            composable(Routes.SettingsDevice.route) {
                PlaceholderScreen("Settings — Device (Owner Only)")
            }
            composable(Routes.SettingsNotifications.route) {
                PlaceholderScreen("Settings — Notifications")
            }
            composable(Routes.SettingsAbout.route) {
                PlaceholderScreen("Settings — About")
            }
        }
    }
}

/**
 * Temporary placeholder composable for every route until PASS 7 supplies
 * real screens. Deliberately minimal — no theming decisions belong here.
 */
@Composable
private fun PlaceholderScreen(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label)
    }
}

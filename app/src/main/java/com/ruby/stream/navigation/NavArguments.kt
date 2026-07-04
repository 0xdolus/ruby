package com.ruby.stream.navigation

/**
 * PASS 1 — Navigation Skeleton
 *
 * Centralized nav argument key names, referenced by Routes.kt and by
 * AppNavGraph.kt's navArgument{} declarations. Kept as plain string
 * constants (not an enum) since NavHost's route-building/argument APIs
 * are string-keyed at the framework level either way.
 */
object NavArguments {
    const val CONTENT_ID = "contentId"
    const val EPISODE_ID = "episodeId"
}

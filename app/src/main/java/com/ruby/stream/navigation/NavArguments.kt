package com.ruby.stream.navigation

import android.net.Uri

/**
 * PASS 1 — Navigation Skeleton
 *
 * Centralized nav argument key names, referenced by Routes.kt and by
 * AppNavGraph.kt's navArgument declarations.
 *
 * Ruby currently builds navigation routes manually using string
 * templates. Dynamic values must therefore be encoded before route
 * interpolation and decoded after reading from SavedStateHandle (see
 * AD-026 in the SOT for the full reasoning).
 */
object NavArguments {
    const val CONTENT_ID = "contentId"
    const val EPISODE_ID = "episodeId"

    /** Encode a dynamic value before interpolating it into a route string. */
    fun encodeArg(value: String): String = Uri.encode(value)

    /** Decode a value read back out of SavedStateHandle for an argument that was encoded via encodeArg(). */
    fun decodeArg(value: String): String = Uri.decode(value)
}

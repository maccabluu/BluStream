package com.blustream.app

import androidx.compose.runtime.Composable

/** Compatibility overload for callers using the original `profile` argument. */
@Composable
fun AddonsScreen(
    profile: String,
    onProfile: () -> Unit,
    onPlaySource: (BluStreamSource) -> Unit,
    compatibility: Unit = Unit
) {
    AddonsScreen(
        profileId = profile,
        profileName = profile,
        onProfile = onProfile,
        onPlaySource = onPlaySource
    )
}

package com.melodix.player.model

/** Minimal user identity surfaced to the app; decoupled from Firebase's FirebaseUser. */
data class AppUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)

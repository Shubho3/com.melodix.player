package com.melodix.player.repo

import com.melodix.player.model.AppUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the signed-in user, or null when signed out. Survives restarts (Firebase persists sessions). */
    val currentUser: Flow<AppUser?>

    /** Exchanges a Google ID token (from Credential Manager) for a Firebase session. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AppUser>

    suspend fun signOut()
}

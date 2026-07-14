package com.melodix.player.core.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/**
 * Full Drive scope — enables reading the user-picked folder AND uploading into it (the custom
 * Compose picker can't grant the narrower drive.file scope the way Google's native picker would).
 *
 * NOTE: this is a *restricted* scope. It works for test users without review; a production release
 * requires Google's OAuth app verification. Narrow to drive.readonly if you only need Phase 2 reads.
 */
private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive"

sealed interface AuthorizeOutcome {
    data class Authorized(val accessToken: String) : AuthorizeOutcome
    /** User consent required — launch this from an Activity result launcher. */
    data class NeedsConsent(val intentSender: IntentSender) : AuthorizeOutcome
}

/** Obtains an OAuth access token scoped to Drive via the Google Identity Authorization API. */
class DriveAuthManager(context: Context) {

    private val client = Identity.getAuthorizationClient(context)
    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_SCOPE)))
        .build()

    /** Returns a token silently if already granted, otherwise a consent [IntentSender]. */
    suspend fun authorize(): Result<AuthorizeOutcome> = runCatching {
        val result = client.authorize(request).await()
        if (result.hasResolution()) {
            AuthorizeOutcome.NeedsConsent(result.pendingIntent!!.intentSender)
        } else {
            AuthorizeOutcome.Authorized(result.accessToken!!)
        }
    }

    /** Pull the token out of the Intent returned after the consent screen. */
    fun tokenFromConsentResult(data: Intent): String? =
        client.getAuthorizationResultFromIntent(data).accessToken
}

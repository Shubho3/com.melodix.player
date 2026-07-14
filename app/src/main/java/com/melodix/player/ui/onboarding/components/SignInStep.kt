package com.melodix.player.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melodix.player.R
import com.melodix.player.core.auth.GoogleAuthClient
import com.melodix.player.core.components.MelodixButton
import com.melodix.player.core.components.MelodixOutlinedButton
import com.melodix.player.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Final onboarding step: optional Google sign-in for cloud sync. [onDone] advances past onboarding —
 * called on skip, and automatically once sign-in succeeds. Styled to match the other onboarding steps.
 */
@Composable
fun SignInStep(
    onDone: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel(),
    googleAuthClient: GoogleAuthClient = koinInject(),
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Signed in (now or already) → leave onboarding.
    LaunchedEffect(user) { if (user != null) onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudSync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.signin_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.signin_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(48.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            MelodixButton(
                text = stringResource(R.string.signin_google),
                onClick = {
                    scope.launch {
                        googleAuthClient.getGoogleIdToken(context)
                            .onSuccess { authViewModel.onGoogleIdToken(it) }
                            .onFailure { authViewModel.onSignInError(it.message ?: "Sign-in cancelled") }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            MelodixOutlinedButton(
                text = stringResource(R.string.signin_skip),
                onClick = onDone,
            )
        }

        uiState.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

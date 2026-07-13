package com.melodix.player.ui.onboarding.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.melodix.player.R
import com.melodix.player.core.components.MelodixButton
import com.melodix.player.core.components.MelodixOutlinedButton

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
fun PermissionStep(
    permissionGranted: Boolean,
    onPermissionResult: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    var hasRequestedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val allGranted = results.values.all { it }
        hasRequestedOnce = true
        onPermissionResult(allGranted)
    }

    val allAlreadyGranted = remember {
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    if (allAlreadyGranted && !permissionGranted) {
        onPermissionResult(true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(48.dp))

        if (permissionGranted) {
            MelodixButton(
                text = stringResource(R.string.permission_granted_continue),
                onClick = onSkip,
            )
        } else if (hasRequestedOnce) {
            MelodixButton(
                text = stringResource(R.string.permission_open_settings),
                onClick = onOpenSettings,
            )
            Spacer(Modifier.height(12.dp))
            MelodixOutlinedButton(
                text = stringResource(R.string.permission_skip),
                onClick = onSkip,
            )
        } else {
            MelodixButton(
                text = stringResource(R.string.permission_grant),
                onClick = { launcher.launch(requiredPermissions()) },
            )
            Spacer(Modifier.height(12.dp))
            MelodixOutlinedButton(
                text = stringResource(R.string.permission_skip),
                onClick = onSkip,
            )
        }
    }
}

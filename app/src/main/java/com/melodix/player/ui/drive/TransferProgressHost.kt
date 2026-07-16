package com.melodix.player.ui.drive

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.melodix.player.repo.sync.TransferSnapshot

/**
 * Renders a persistent snackbar while transfers are active and, on tap, the full dialog.
 * [openDialogSignal] is incremented by callers (e.g. Cloud Sync buttons) to force the dialog open.
 */
@Composable
fun TransferProgressHost(
    snapshot: TransferSnapshot,
    snackbarHostState: SnackbarHostState,
    openDialogSignal: Int,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(openDialogSignal) {
        if (openDialogSignal > 0) showDialog = true
    }

    // Keyed on showDialog too, so dismissing the dialog ("Hide") immediately re-surfaces the
    // persistent snackbar even if no transfer count changed in the meantime.
    LaunchedEffect(snapshot.isActive, snapshot.doneCount, snapshot.failedCount, snapshot.items.size, showDialog) {
        if (snapshot.items.isEmpty() || showDialog) return@LaunchedEffect
        val label = if (snapshot.isActive) {
            "Transferring ${snapshot.doneCount}/${snapshot.items.size} · ${snapshot.overallPercent}%"
        } else {
            "Done ${snapshot.doneCount}/${snapshot.items.size}" +
                if (snapshot.failedCount > 0) " · ${snapshot.failedCount} failed" else ""
        }
        val result = snackbarHostState.showSnackbar(
            message = label,
            actionLabel = "View",
            duration = if (snapshot.isActive) SnackbarDuration.Indefinite else SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) showDialog = true
    }

    if (showDialog) {
        TransferProgressDialog(
            snapshot = snapshot,
            onRetry = onRetry,
            onClose = { showDialog = false },
        )
    }

    SnackbarHost(hostState = snackbarHostState, modifier = modifier)
}

package com.melodix.player.ui.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melodix.player.core.components.MelodixButton
import com.melodix.player.core.components.MelodixOutlinedButton
import com.melodix.player.viewmodel.CloudSyncViewModel
import com.melodix.player.viewmodel.SyncStatus
import com.melodix.player.viewmodel.WorkQueue
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncScreen(
    onBack: () -> Unit,
    viewModel: CloudSyncViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Cloud Sync",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        ) {
            // ── Account ──
            SectionLabel("Account")
            Text(
                text = state.signedInEmail ?: "Not signed in — sign in from Settings → Cloud Sync.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.signedInEmail != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            // ── Firestore data (favorites / history / playlists) ──
            SectionLabel("Your data")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatChip("Favorites", state.favoritesCount, Modifier.weight(1f))
                StatChip("History", state.historyCount, Modifier.weight(1f))
                StatChip("Playlists", state.playlistCount, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            SyncStatusRow(state.syncStatus)
            Spacer(Modifier.height(12.dp))
            MelodixButton(
                text = "Sync now",
                onClick = { viewModel.syncNow() },
            )

            Spacer(Modifier.height(28.dp))

            // ── Google Drive ──
            SectionLabel("Google Drive")
            if (state.notConnected) {
                Text(
                    text = "Choose a Google Drive folder first (Settings → Cloud Sync → Google Drive folder).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                StatCard("In Drive folder", state.driveFileCount)
                Spacer(Modifier.height(10.dp))
                StatCard("Cached offline", state.cachedCount)
                Spacer(Modifier.height(10.dp))
                StatCard("Local songs not on Drive", state.localOnlyCount)

                if (state.downloads.total > 0) {
                    Spacer(Modifier.height(16.dp))
                    QueueRow("Downloads", state.downloads)
                }
                if (state.uploads.total > 0) {
                    Spacer(Modifier.height(12.dp))
                    QueueRow("Uploads", state.uploads)
                }

                Spacer(Modifier.height(24.dp))
                MelodixButton(text = "Download from Drive", onClick = { viewModel.downloadAll() })
                Spacer(Modifier.height(12.dp))
                MelodixOutlinedButton(text = "Back up local songs to Drive", onClick = { viewModel.backupLocalOnly() })
            }

            state.message?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun SyncStatusRow(status: SyncStatus) {
    when (status) {
        SyncStatus.Idle -> StatusText("Not synced yet", MaterialTheme.colorScheme.onSurfaceVariant)
        SyncStatus.Syncing -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(0.dp))
            StatusText("  Syncing…", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is SyncStatus.Synced -> {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(status.at))
            StatusText("Synced ✓  ·  $time", MaterialTheme.colorScheme.primary)
        }
        is SyncStatus.Error -> StatusText("Sync failed: ${status.message}", MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun StatusText(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
private fun StatChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun QueueRow(label: String, queue: WorkQueue) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "$label · ${queue.done}/${queue.total}" +
                    if (queue.failed > 0) "  (${queue.failed} failed)" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (queue.active) "${queue.percent}%" else if (queue.total > 0) "Done" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (queue.total == 0) 0f else queue.done.toFloat() / queue.total },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

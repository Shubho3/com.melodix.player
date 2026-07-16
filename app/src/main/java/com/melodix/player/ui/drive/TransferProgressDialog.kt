package com.melodix.player.ui.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.melodix.player.repo.sync.TransferItem
import com.melodix.player.repo.sync.TransferItemState
import com.melodix.player.repo.sync.TransferSnapshot

@Composable
fun TransferProgressDialog(
    snapshot: TransferSnapshot,
    onRetry: (String) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Transfers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${snapshot.doneCount}/${snapshot.items.size} done" +
                        if (snapshot.failedCount > 0) " · ${snapshot.failedCount} failed" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(snapshot.items, key = { it.uniqueName }) { item ->
                        TransferRow(item, onRetry)
                    }
                }
                Spacer(Modifier.size(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text(if (snapshot.isActive) "Hide" else "Close") }
                }
            }
        }
    }
}

@Composable
private fun TransferRow(item: TransferItem, onRetry: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.size(4.dp))
            LinearProgressIndicator(
                progress = { item.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
        Spacer(Modifier.size(12.dp))
        when (item.state) {
            TransferItemState.DONE -> Icon(
                Icons.Rounded.CheckCircle, contentDescription = "Done",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp),
            )
            TransferItemState.FAILED -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.ErrorOutline, contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp),
                )
                TextButton(onClick = { onRetry(item.uniqueName) }) { Text("Retry") }
            }
            TransferItemState.RUNNING -> Text(
                "${item.progress}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            TransferItemState.QUEUED -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

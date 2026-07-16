package com.melodix.player.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import android.text.format.Formatter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melodix.player.R
import com.melodix.player.core.auth.GoogleAuthClient
import com.melodix.player.core.components.ColorPicker
import com.melodix.player.core.components.MelodixButton
import com.melodix.player.core.theme.AppTheme
import com.melodix.player.core.theme.buildCustomScheme
import com.melodix.player.core.theme.displayName
import com.melodix.player.core.theme.toColorScheme
import com.melodix.player.model.CustomThemeColors
import com.melodix.player.model.MusicFolder
import com.melodix.player.viewmodel.AuthViewModel
import com.melodix.player.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onOpenDrivePicker: () -> Unit = {},
    onOpenCloudSync: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    googleAuthClient: GoogleAuthClient = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Tracks whether the app is exempt from battery optimization (so background playback survives).
    var batteryExempt by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    // Recompute the cache size and battery-exemption state each time Settings resumes (e.g. returning
    // from the Storage screen after clearing caches, or from the system battery dialog) so the rows
    // never show a stale figure.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshCacheSize()
                batteryExempt = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showThemePicker by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var showMinLength by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(false) }
    var showCustomTheme by remember { mutableStateOf(false) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    if (showThemePicker) {
        ThemePickerSheet(
            selected = state.selectedTheme,
            onSelect = { theme ->
                showThemePicker = false
                if (theme == AppTheme.CUSTOM) {
                    showCustomTheme = true
                } else {
                    viewModel.selectTheme(theme)
                }
            },
            onDismiss = { showThemePicker = false },
        )
    }

    if (showLicenses) {
        LicensesDialog(onDismiss = { showLicenses = false })
    }

    if (showMinLength) {
        MinLengthSheet(
            selectedSeconds = state.minDurationSec,
            onSelect = {
                viewModel.setMinDuration(it)
                showMinLength = false
            },
            onDismiss = { showMinLength = false },
        )
    }

    if (showFolders) {
        FoldersSheet(
            folders = state.folders,
            excludedIds = state.excludedFolderIds,
            onToggle = { id, excluded -> viewModel.setFolderExcluded(id, excluded) },
            onDismiss = { showFolders = false },
        )
    }

    if (showCustomTheme) {
        CustomThemeSheet(
            initial = state.customThemeColors,
            onSave = {
                viewModel.saveCustomTheme(it)
                showCustomTheme = false
            },
            onDismiss = { showCustomTheme = false },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
            )
        }

        item {
            SettingsGroup(title = stringResource(R.string.settings_playback)) {
                SettingsToggleItem(
                    icon = Icons.Rounded.HighQuality,
                    title = stringResource(R.string.settings_lossless),
                    subtitle = stringResource(R.string.settings_lossless_sub),
                    checked = state.losslessAudio,
                    onCheckedChange = { viewModel.toggleLossless() },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.settings_equalizer),
                    subtitle = stringResource(R.string.settings_equalizer_sub),
                    onClick = onOpenEqualizer,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = "Background playback",
                    subtitle = if (batteryExempt) {
                        "On — music keeps playing when the app is closed"
                    } else {
                        "Off — tap to keep playing after you close the app"
                    },
                    onClick = { requestIgnoreBatteryOptimizations(context) },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            SettingsGroup(title = stringResource(R.string.settings_appearance)) {
                SettingsItem(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_theme),
                    subtitle = state.selectedTheme.displayName(),
                    onClick = { showThemePicker = true },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            SettingsGroup(title = "Cloud Sync") {
                if (user != null) {
                    SettingsItem(
                        icon = Icons.Rounded.AccountCircle,
                        title = user?.displayName ?: "Signed in",
                        subtitle = user?.email ?: "",
                        onClick = { },
                        showChevron = false,
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        title = "Sign out",
                        subtitle = "Disconnect this account",
                        onClick = { authViewModel.signOut() },
                        showChevron = false,
                    )
                } else {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Rounded.Login,
                        title = "Sign in with Google",
                        subtitle = "Back up and sync across devices",
                        onClick = {
                            scope.launch {
                                googleAuthClient.getGoogleIdToken(context)
                                    .onSuccess { authViewModel.onGoogleIdToken(it) }
                                    .onFailure { authViewModel.onSignInError(it.message ?: "Sign-in cancelled") }
                            }
                        },
                    )
                }
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Rounded.FolderShared,
                    title = "Google Drive folder",
                    subtitle = if (state.driveFolderId != null) "Connected" else "Not connected",
                    onClick = onOpenDrivePicker,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Rounded.CloudSync,
                    title = "Backup & Download",
                    subtitle = "Sync songs with your Drive folder",
                    onClick = onOpenCloudSync,
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            SettingsGroup(title = "Library") {
                SettingsSyncItem(
                    songCount = state.songCount,
                    isSyncing = state.isSyncing,
                    didSync = state.didSync,
                    onClick = { viewModel.syncLibrary() },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Rounded.Timer,
                    title = "Minimum song length",
                    subtitle = minLengthLabel(state.minDurationSec),
                    onClick = { showMinLength = true },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Rounded.Folder,
                    title = "Folders",
                    subtitle = foldersSubtitle(state.folders, state.excludedFolderIds),
                    onClick = { showFolders = true },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            SettingsGroup(title = stringResource(R.string.settings_notifications_group)) {
                SettingsItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(R.string.settings_notifications_sub),
                    onClick = { openNotificationSettings(context) },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            SettingsGroup(title = stringResource(R.string.settings_storage)) {
                SettingsItem(
                    icon = Icons.Rounded.Storage,
                    title = stringResource(R.string.settings_cache),
                    subtitle = Formatter.formatShortFileSize(context, state.totalCacheBytes) + " used",
                    onClick = onOpenStorage,
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            SettingsGroup(title = stringResource(R.string.settings_about_group)) {
                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_version),
                    subtitle = versionName,
                    onClick = { },
                    showChevron = false,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.settings_open_source),
                    subtitle = stringResource(R.string.settings_open_source_sub),
                    onClick = { showLicenses = true },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    title = stringResource(R.string.settings_help),
                    subtitle = stringResource(R.string.settings_help_sub),
                    onClick = { openSupportEmail(context) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerSheet(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            AppTheme.entries.forEach { theme ->
                val scheme = theme.toColorScheme()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(theme) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.background)
                            .border(2.dp, scheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(scheme.primary),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = theme.displayName(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (theme == selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.settings_open_source)) },
        text = {
            Text(
                "Melodix is built with:\n\n" +
                    "• Jetpack Compose & Material 3\n" +
                    "• AndroidX Media3 / ExoPlayer\n" +
                    "• Koin (dependency injection)\n" +
                    "• Coil (image loading)\n" +
                    "• AndroidX DataStore\n\n" +
                    "Each is licensed under the Apache License 2.0.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

private fun openEqualizer(context: android.content.Context) {
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    // Silently no-op if the device has no system equalizer to handle it.
    runCatching { context.startActivity(intent) }
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    runCatching { context.startActivity(intent) }
}

/** True when the app is whitelisted from battery optimization (so its playback service survives). */
private fun isIgnoringBatteryOptimizations(context: android.content.Context): Boolean {
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * If not already exempt, shows the system "Allow background activity?" dialog for Melodix; otherwise
 * opens the battery-optimization list so the user can change it. Exempting the app is what stops the
 * OS (especially aggressive OEMs) from killing playback when it reclaims memory.
 */
@SuppressLint("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: android.content.Context) {
    val intent = if (isIgnoringBatteryOptimizations(context)) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
    }
    runCatching { context.startActivity(intent) }
}

private fun openSupportEmail(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@melodix.app"))
        putExtra(Intent.EXTRA_SUBJECT, "Melodix Support")
    }
    runCatching { context.startActivity(intent) }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(1.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsSyncItem(
    songCount: Int,
    isSyncing: Boolean,
    didSync: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSyncing, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Scan for new music",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    isSyncing -> "Syncing…"
                    didSync -> "$songCount songs · up to date"
                    else -> "$songCount songs in your library"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Sync",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 54.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

private fun minLengthLabel(seconds: Int): String = when {
    seconds <= 0 -> "Off"
    seconds < 60 -> "${seconds}s"
    else -> "${seconds / 60} min"
}

private fun foldersSubtitle(folders: List<MusicFolder>, excluded: Set<Long>): String {
    if (folders.isEmpty()) return "All folders"
    val included = folders.count { it.id !in excluded }
    return "$included of ${folders.size} folders shown"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinLengthSheet(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val options = listOf(0, 30, 60, 120, 300)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Minimum song length",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                text = "Hide tracks shorter than this everywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            options.forEach { seconds ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(seconds) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = minLengthLabel(seconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (seconds == selectedSeconds) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoldersSheet(
    folders: List<MusicFolder>,
    excludedIds: Set<Long>,
    onToggle: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = "Folders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                text = "Unchecked folders are hidden from your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            if (folders.isEmpty()) {
                Text(
                    text = "No music folders found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(folders, key = { it.id }) { folder ->
                        val included = folder.id !in excludedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(folder.id, included) }
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = included,
                                onCheckedChange = { checked -> onToggle(folder.id, !checked) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${folder.trackCount} songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomThemeSheet(
    initial: CustomThemeColors?,
    onSave: (CustomThemeColors) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var accent by remember { mutableStateOf(initial?.accent ?: 0xFF6750A4.toInt()) }
    var background by remember { mutableStateOf(initial?.background ?: 0xFFFFFBFE.toInt()) }
    var text by remember { mutableStateOf(initial?.text ?: 0xFF1C1B1F.toInt()) }
    val colors = CustomThemeColors(accent, background, text)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Custom theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                text = "Pick your colors — the rest of the palette is generated.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            CustomThemePreview(colors)
            Spacer(Modifier.height(20.dp))
            ColorPicker(label = "Accent", color = accent, onColorChange = { accent = it })
            Spacer(Modifier.height(16.dp))
            ColorPicker(label = "Background", color = background, onColorChange = { background = it })
            Spacer(Modifier.height(16.dp))
            ColorPicker(label = "Text", color = text, onColorChange = { text = it })
            Spacer(Modifier.height(24.dp))
            MelodixButton(text = "Save theme", onClick = { onSave(colors) })
        }
    }
}

@Composable
private fun CustomThemePreview(colors: CustomThemeColors) {
    val scheme = buildCustomScheme(colors)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
        )
        Text(
            text = "Sample subtitle text",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.primary)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimary,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(scheme.secondary),
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(scheme.tertiary),
            )
        }
    }
}

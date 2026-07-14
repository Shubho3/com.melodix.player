package com.melodix.player.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.melodix.player.R
import com.melodix.player.core.components.MiniPlayer
import com.melodix.player.model.Track
import com.melodix.player.ui.home.HomeScreen
import com.melodix.player.ui.library.LibraryScreen
import com.melodix.player.ui.search.SearchScreen
import com.melodix.player.ui.settings.SettingsScreen
import com.melodix.player.viewmodel.NowPlayingViewModel

private sealed class Tab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val activeIcon: ImageVector,
) {
    data object Home : Tab("tab_home", R.string.nav_home, Icons.Outlined.Home, Icons.Rounded.Home)
    data object Library : Tab("tab_library", R.string.nav_library, Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic)
    data object Search : Tab("tab_search", R.string.nav_search, Icons.Outlined.Search, Icons.Rounded.Search)
    data object Settings : Tab("tab_settings", R.string.nav_settings, Icons.Outlined.Settings, Icons.Rounded.Settings)
}

private val tabs = listOf(Tab.Home, Tab.Library, Tab.Search, Tab.Settings)

@Composable
fun MainScreen(
    nowPlayingViewModel: NowPlayingViewModel,
    onOpenNowPlaying: () -> Unit,
    onOpenAlbum: (Long) -> Unit = {},
    onOpenArtist: (Long) -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onOpenLikedSongs: () -> Unit = {},
    onOpenRecentlyPlayed: () -> Unit = {},
    onOpenDrivePicker: () -> Unit = {},
    onOpenCloudSync: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val nowPlayingState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()

    val onTrackClick: (Track, List<Track>) -> Unit = { track, queue ->
        nowPlayingViewModel.playTrack(track, queue)
    }

    val onAddToQueue: (Track) -> Unit = { track ->
        nowPlayingViewModel.addToQueue(track)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabScale",
                    )

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.activeIcon else tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                                modifier = Modifier
                                    .size(if (selected) 26.dp else 24.dp)
                                    .scale(scale),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = tabNavController,
                startDestination = Tab.Home.route,
                modifier = Modifier.fillMaxSize(),
                // Peer tabs cross-fade — smoother and cheaper than a slide.
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { fadeOut(tween(200)) },
            ) {
                composable(Tab.Home.route) {
                    HomeScreen(
                        onTrackClick = onTrackClick,
                        onAddToQueue = onAddToQueue,
                        onViewAllRecent = onOpenRecentlyPlayed,
                    )
                }
                composable(Tab.Library.route) {
                    LibraryScreen(
                        onTrackClick = onTrackClick,
                        onAddToQueue = onAddToQueue,
                        onOpenAlbum = onOpenAlbum,
                        onOpenArtist = onOpenArtist,
                        onOpenPlaylist = onOpenPlaylist,
                        onOpenLikedSongs = onOpenLikedSongs,
                    )
                }
                composable(Tab.Search.route) {
                    SearchScreen(
                        onTrackClick = onTrackClick,
                        onAddToQueue = onAddToQueue,
                        onOpenAlbum = onOpenAlbum,
                        onOpenArtist = onOpenArtist,
                    )
                }
                composable(Tab.Settings.route) {
                    SettingsScreen(
                        onOpenDrivePicker = onOpenDrivePicker,
                        onOpenCloudSync = onOpenCloudSync,
                        onOpenEqualizer = onOpenEqualizer,
                    )
                }
            }

            MiniPlayer(
                track = nowPlayingState.currentTrack,
                isPlaying = nowPlayingState.isPlaying,
                progress = nowPlayingState.progress,
                onPlayPause = { nowPlayingViewModel.togglePlayPause() },
                onSkipNext = { nowPlayingViewModel.skipNext() },
                onClick = onOpenNowPlaying,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

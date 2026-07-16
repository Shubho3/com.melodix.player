package com.melodix.player.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
    onOpenStorage: () -> Unit = {},
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
            FloatingPillNav(
                tabs = tabs,
                isSelected = { tab -> currentDestination?.hierarchy?.any { it.route == tab.route } == true },
                onTabSelected = { tab ->
                    tabNavController.navigate(tab.route) {
                        popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
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
                        onOpenStorage = onOpenStorage,
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
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * A modern, detached "floating pill" bottom navigation for the music-player aura: a rounded capsule
 * that hovers above the bottom edge, icon-only, with a soft accent glow that slides to the active tab.
 * All colors come from [MaterialTheme.colorScheme] so it adapts to every theme.
 */
@Composable
private fun FloatingPillNav(
    tabs: List<Tab>,
    isSelected: (Tab) -> Boolean,
    onTabSelected: (Tab) -> Unit,
) {
    val selectedIndex = tabs.indexOfFirst { isSelected(it) }.coerceAtLeast(0)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 40.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            BoxWithConstraints(contentAlignment = Alignment.CenterStart) {
                val slotWidth = maxWidth / tabs.size
                val indicatorWidth = 56.dp
                val targetX = slotWidth * selectedIndex + (slotWidth - indicatorWidth) / 2
                val indicatorX by animateDpAsState(
                    targetValue = targetX,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "navIndicator",
                )

                // Soft accent glow that slides behind the active icon.
                Box(
                    modifier = Modifier
                        .offset(x = indicatorX)
                        .width(indicatorWidth)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { tab ->
                        val selected = isSelected(tab)
                        val iconColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "navIconColor",
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "navIconScale",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onTabSelected(tab) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (selected) tab.activeIcon else tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                                tint = iconColor,
                                modifier = Modifier
                                    .size(26.dp)
                                    .scale(scale),
                            )
                        }
                    }
                }
            }
        }
    }
}

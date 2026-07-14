package com.melodix.player.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.melodix.player.ui.detail.DetailScreen
import com.melodix.player.ui.drive.CloudSyncScreen
import com.melodix.player.ui.drive.DriveFolderPickerScreen
import com.melodix.player.ui.main.MainScreen
import com.melodix.player.ui.nowplaying.NowPlayingScreen
import com.melodix.player.ui.settings.EqualizerScreen
import com.melodix.player.ui.onboarding.OnboardingScreen
import com.melodix.player.ui.queue.QueueScreen
import com.melodix.player.viewmodel.AlbumDetailViewModel
import com.melodix.player.viewmodel.ArtistDetailViewModel
import com.melodix.player.viewmodel.LikedSongsViewModel
import com.melodix.player.viewmodel.NowPlayingViewModel
import com.melodix.player.viewmodel.PlaylistDetailViewModel
import com.melodix.player.viewmodel.RecentlyPlayedViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NavGraph(startOnboarding: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startOnboarding) Routes.Onboarding.route else Routes.Main.route
    val nowPlayingViewModel: NowPlayingViewModel = koinViewModel()

    val slideDuration = 300

    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Forward pushes slide in from the right; the outgoing screen parallax-slides left.
        enterTransition = { slideInHorizontally(tween(slideDuration)) { it } + fadeIn(tween(slideDuration)) },
        exitTransition = { slideOutHorizontally(tween(slideDuration)) { -it / 4 } + fadeOut(tween(slideDuration)) },
        popEnterTransition = { slideInHorizontally(tween(slideDuration)) { -it / 4 } + fadeIn(tween(slideDuration)) },
        popExitTransition = { slideOutHorizontally(tween(slideDuration)) { it } + fadeOut(tween(slideDuration)) },
    ) {
        composable(Routes.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Main.route) {
            MainScreen(
                nowPlayingViewModel = nowPlayingViewModel,
                onOpenNowPlaying = { navController.navigate(Routes.NowPlaying.route) },
                onOpenAlbum = { navController.navigate(Routes.AlbumDetail.create(it)) },
                onOpenArtist = { navController.navigate(Routes.ArtistDetail.create(it)) },
                onOpenPlaylist = { navController.navigate(Routes.PlaylistDetail.create(it)) },
                onOpenLikedSongs = { navController.navigate(Routes.LikedSongs.route) },
                onOpenRecentlyPlayed = { navController.navigate(Routes.RecentlyPlayed.route) },
                onOpenDrivePicker = { navController.navigate(Routes.DrivePicker.route) },
                onOpenCloudSync = { navController.navigate(Routes.CloudSync.route) },
                onOpenEqualizer = { navController.navigate(Routes.Equalizer.route) },
            )
        }
        composable(Routes.Equalizer.route) {
            EqualizerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DrivePicker.route) {
            DriveFolderPickerScreen(
                onBack = { navController.popBackStack() },
                onFolderSelected = { navController.popBackStack() },
            )
        }
        composable(Routes.CloudSync.route) {
            CloudSyncScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.NowPlaying.route,
            enterTransition = { slideInVertically(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { slideOutVertically(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            NowPlayingScreen(
                onBack = { navController.popBackStack() },
                onOpenQueue = { navController.navigate(Routes.Queue.route) },
                viewModel = nowPlayingViewModel,
            )
        }
        composable(
            route = Routes.Queue.route,
            enterTransition = { slideInVertically(tween(350)) { it } + fadeIn(tween(350)) },
            popExitTransition = { slideOutVertically(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            QueueScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.AlbumDetail.route,
            arguments = listOf(navArgument(Routes.AlbumDetail.ARG) { type = NavType.LongType }),
        ) { entry ->
            val albumId = entry.arguments?.getLong(Routes.AlbumDetail.ARG) ?: return@composable
            val vm: AlbumDetailViewModel = koinViewModel()
            LaunchedEffect(albumId) { vm.load(albumId) }
            DetailRoute(vm.uiState.collectAsStateWithLifecycle().value, nowPlayingViewModel) {
                navController.popBackStack()
            }
        }

        composable(
            route = Routes.ArtistDetail.route,
            arguments = listOf(navArgument(Routes.ArtistDetail.ARG) { type = NavType.LongType }),
        ) { entry ->
            val artistId = entry.arguments?.getLong(Routes.ArtistDetail.ARG) ?: return@composable
            val vm: ArtistDetailViewModel = koinViewModel()
            LaunchedEffect(artistId) { vm.load(artistId) }
            DetailRoute(vm.uiState.collectAsStateWithLifecycle().value, nowPlayingViewModel) {
                navController.popBackStack()
            }
        }

        composable(
            route = Routes.PlaylistDetail.route,
            arguments = listOf(navArgument(Routes.PlaylistDetail.ARG) { type = NavType.StringType }),
        ) { entry ->
            val playlistId = entry.arguments?.getString(Routes.PlaylistDetail.ARG) ?: return@composable
            val vm: PlaylistDetailViewModel = koinViewModel()
            LaunchedEffect(playlistId) { vm.load(playlistId) }
            DetailRoute(vm.uiState.collectAsStateWithLifecycle().value, nowPlayingViewModel) {
                navController.popBackStack()
            }
        }

        composable(Routes.LikedSongs.route) {
            val vm: LikedSongsViewModel = koinViewModel()
            DetailRoute(vm.uiState.collectAsStateWithLifecycle().value, nowPlayingViewModel) {
                navController.popBackStack()
            }
        }

        composable(Routes.RecentlyPlayed.route) {
            val vm: RecentlyPlayedViewModel = koinViewModel()
            DetailRoute(vm.uiState.collectAsStateWithLifecycle().value, nowPlayingViewModel) {
                navController.popBackStack()
            }
        }
    }
}

@Composable
private fun DetailRoute(
    state: com.melodix.player.viewmodel.DetailUiState,
    nowPlayingViewModel: NowPlayingViewModel,
    onBack: () -> Unit,
) {
    val npState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()
    DetailScreen(
        state = state,
        currentTrackId = npState.currentTrack?.id,
        onBack = onBack,
        onTrackClick = { track, list -> nowPlayingViewModel.playTrack(track, list) },
        onShuffleAll = { nowPlayingViewModel.shufflePlay(it) },
        onAddToQueue = { nowPlayingViewModel.addToQueue(it) },
    )
}

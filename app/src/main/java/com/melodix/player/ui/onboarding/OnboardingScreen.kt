package com.melodix.player.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melodix.player.R
import com.melodix.player.core.components.StepIndicator
import com.melodix.player.ui.onboarding.components.FeatureIntroStep
import com.melodix.player.ui.onboarding.components.PermissionStep
import com.melodix.player.ui.onboarding.components.SignInStep
import com.melodix.player.ui.onboarding.components.ThemePickerStep
import com.melodix.player.viewmodel.OnboardingUiEffect
import com.melodix.player.viewmodel.OnboardingViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { state.totalSteps })

    LaunchedEffect(state.currentStep) {
        pagerState.animateScrollToPage(state.currentStep)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OnboardingUiEffect.NavigateToHome -> onFinished()
                OnboardingUiEffect.OpenAppSettings -> {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                }
                OnboardingUiEffect.RequestPermission -> { }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                TextButton(
                    onClick = { viewModel.completeOnboarding() },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = false,
            ) { page ->
                when (page) {
                    0 -> PermissionStep(
                        permissionGranted = state.permissionGranted,
                        onPermissionResult = { viewModel.onPermissionResult(it) },
                        onOpenSettings = { viewModel.openAppSettings() },
                        onSkip = { viewModel.onSkipPermission() },
                    )
                    1 -> ThemePickerStep(
                        themes = state.themes,
                        selectedTheme = state.selectedTheme,
                        onThemeSelected = { viewModel.selectTheme(it) },
                        onNext = { viewModel.nextStep() },
                    )
                    2 -> FeatureIntroStep(
                        onGetStarted = { viewModel.nextStep() },
                    )
                    3 -> SignInStep(
                        onDone = { viewModel.completeOnboarding() },
                    )
                }
            }

            StepIndicator(
                totalSteps = state.totalSteps,
                currentStep = state.currentStep,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

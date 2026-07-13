package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.core.theme.AppTheme
import com.melodix.player.repo.SettingsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 3,
    val permissionGranted: Boolean = false,
    val selectedTheme: AppTheme = AppTheme.MONO,
    val themes: ImmutableList<AppTheme> = persistentListOf(
        AppTheme.MONO,
        AppTheme.MIDNIGHT,
        AppTheme.SUNSHINE,
        AppTheme.AQUA,
        AppTheme.NATURE,
        AppTheme.TECHNO,
    ),
    val isComplete: Boolean = false,
)

sealed interface OnboardingUiEffect {
    data object RequestPermission : OnboardingUiEffect
    data object OpenAppSettings : OnboardingUiEffect
    data object NavigateToHome : OnboardingUiEffect
}

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getTheme().collect { theme ->
                _uiState.update { it.copy(selectedTheme = theme) }
            }
        }
    }

    fun requestPermission() {
        viewModelScope.launch {
            _effect.send(OnboardingUiEffect.RequestPermission)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted) {
            _uiState.update { it.copy(currentStep = 1) }
        }
    }

    fun onSkipPermission() {
        _uiState.update { it.copy(currentStep = 1) }
    }

    fun openAppSettings() {
        viewModelScope.launch {
            _effect.send(OnboardingUiEffect.OpenAppSettings)
        }
    }

    fun selectTheme(theme: AppTheme) {
        _uiState.update { it.copy(selectedTheme = theme) }
        viewModelScope.launch {
            settingsRepository.saveTheme(theme)
        }
    }

    fun nextStep() {
        _uiState.update {
            it.copy(currentStep = (it.currentStep + 1).coerceAtMost(it.totalSteps - 1))
        }
    }

    fun previousStep() {
        _uiState.update {
            it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0))
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete()
            _uiState.update { it.copy(isComplete = true) }
            _effect.send(OnboardingUiEffect.NavigateToHome)
        }
    }
}

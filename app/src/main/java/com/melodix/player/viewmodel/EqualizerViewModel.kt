package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.core.audio.EqMode
import com.melodix.player.repo.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val mode: StateFlow<EqMode> = settingsRepository.getEqMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EqMode.NORMAL)

    fun setMode(mode: EqMode) {
        viewModelScope.launch { settingsRepository.setEqMode(mode) }
    }
}

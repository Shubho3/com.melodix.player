package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.repo.CacheCategory
import com.melodix.player.repo.CacheType
import com.melodix.player.repo.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StorageUiState(
    val categories: List<CacheCategory> = emptyList(),
    val totalBytes: Long = 0L,
    val isLoading: Boolean = true,
)

class StorageViewModel(
    private val storageRepository: StorageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val categories = storageRepository.categories()
            val total = storageRepository.totalBytes()
            _uiState.value = StorageUiState(
                categories = categories,
                totalBytes = total,
                isLoading = false,
            )
        }
    }

    fun clear(type: CacheType) {
        viewModelScope.launch {
            storageRepository.clear(type)
            refresh()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            storageRepository.clearAll()
            refresh()
        }
    }
}

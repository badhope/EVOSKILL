package com.banana.toolbox.ui.screens.tools.cleaner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CleanerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState
}

data class CleanerUiState(
    val isLoading: Boolean = false
)

package com.banana.toolbox.ui.screens.tools.generator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GeneratorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState
}

data class GeneratorUiState(
    val isLoading: Boolean = false
)

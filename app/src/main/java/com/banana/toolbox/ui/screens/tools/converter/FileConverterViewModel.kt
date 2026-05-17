package com.banana.toolbox.ui.screens.tools.converter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileConverterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FileConverterUiState())
    val uiState: StateFlow<FileConverterUiState> = _uiState
}

data class FileConverterUiState(
    val isLoading: Boolean = false
)

package com.banana.toolbox.ui.screens.tools.inspector

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeepInspectorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DeepInspectorUiState())
    val uiState: StateFlow<DeepInspectorUiState> = _uiState
}

data class DeepInspectorUiState(
    val isLoading: Boolean = false
)

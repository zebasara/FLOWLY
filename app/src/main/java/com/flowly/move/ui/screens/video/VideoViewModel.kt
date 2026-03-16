package com.flowly.move.ui.screens.video

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class VideoUiState {
    object Idle      : VideoUiState()
    object Loading   : VideoUiState()
    object Success   : VideoUiState()
    data class Error(val msg: String) : VideoUiState()
}

class VideoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FlowlyRepository(app)
    private val prefs = UserPreferences(app)

    private val _uiState = MutableStateFlow<VideoUiState>(VideoUiState.Idle)
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    fun cobrarRecompensa(amount: Int = 50) {
        viewModelScope.launch {
            _uiState.value = VideoUiState.Loading
            val uid = prefs.userId.first()
            repo.cobrarVideo(uid, amount).fold(
                onSuccess = { _uiState.value = VideoUiState.Success },
                onFailure = { _uiState.value = VideoUiState.Error(it.message ?: "Error al cobrar recompensa") }
            )
        }
    }

    fun reset() { _uiState.value = VideoUiState.Idle }
}

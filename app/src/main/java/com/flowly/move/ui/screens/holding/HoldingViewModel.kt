package com.flowly.move.ui.screens.holding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.Holding
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class HoldingUiState {
    object Idle    : HoldingUiState()
    object Loading : HoldingUiState()
    object Success : HoldingUiState()
    data class Error(val msg: String) : HoldingUiState()
}

class HoldingViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FlowlyRepository(app)
    private val prefs = UserPreferences(app)

    private val _user     = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _holdings = MutableStateFlow<List<Holding>>(emptyList())
    val holdings: StateFlow<List<Holding>> = _holdings.asStateFlow()

    private val _uiState  = MutableStateFlow<HoldingUiState>(HoldingUiState.Idle)
    val uiState: StateFlow<HoldingUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var uid = ""

    init {
        viewModelScope.launch {
            uid = prefs.userId.first()
            repo.getUser(uid).onSuccess { _user.value = it }
            repo.getHoldings(uid).onSuccess { _holdings.value = it }
            _isLoading.value = false
        }
    }

    fun confirmarHolding(moveAmount: Int, meses: Int) {
        val saldoLibre = _user.value?.tokensActuales ?: 0
        if (saldoLibre < moveAmount) {
            _uiState.value = HoldingUiState.Error("Saldo insuficiente")
            return
        }
        viewModelScope.launch {
            _uiState.value = HoldingUiState.Loading
            val ahora     = System.currentTimeMillis()
            val fin       = ahora + (meses * 30L * 24 * 60 * 60 * 1000)
            val interes   = (moveAmount * tasaPorMeses(meses)).toInt()
            val holding = Holding(
                uid        = uid,
                moveAmount = moveAmount,
                meses      = meses,
                interesMove = interes,
                fechaInicio = ahora,
                fechaFin    = fin
            )
            repo.createHolding(uid, holding).fold(
                onSuccess = {
                    repo.getUser(uid).onSuccess { _user.value = it }
                    repo.getHoldings(uid).onSuccess { _holdings.value = it }
                    _uiState.value = HoldingUiState.Success
                },
                onFailure = { _uiState.value = HoldingUiState.Error(it.message ?: "Error al crear holding") }
            )
        }
    }

    fun clearState() { _uiState.value = HoldingUiState.Idle }

    companion object {
        fun tasaPorMeses(meses: Int): Float = when (meses) {
            3    -> 0.08f
            6    -> 0.12f
            9    -> 0.18f
            else -> 0.08f
        }
    }
}

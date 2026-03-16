package com.flowly.move.ui.screens.blockchain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.BlockchainConfig
import com.flowly.move.data.model.RetiroBlockchain
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.AuthRepository
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class BlockchainUiState {
    object Idle      : BlockchainUiState()
    object Loading   : BlockchainUiState()
    object Success   : BlockchainUiState()
    data class Error(val msg: String) : BlockchainUiState()
}

class BlockchainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo     = FlowlyRepository(app)
    private val authRepo = AuthRepository(app)
    private val prefs    = UserPreferences(app)

    // ── Estado de carga ──────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Config blockchain (de Firestore) ─────────────────────────────────
    private val _config = MutableStateFlow<BlockchainConfig?>(null)
    val config: StateFlow<BlockchainConfig?> = _config.asStateFlow()

    // ── Usuario ──────────────────────────────────────────────────────────
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // ── Historial de retiros ─────────────────────────────────────────────
    private val _retiros = MutableStateFlow<List<RetiroBlockchain>>(emptyList())
    val retiros: StateFlow<List<RetiroBlockchain>> = _retiros.asStateFlow()

    // ── Conteo de usuarios (para la pantalla bloqueada) ──────────────────
    private val _userCount = MutableStateFlow(0L)
    val userCount: StateFlow<Long> = _userCount.asStateFlow()

    // ── UI state (retiro) ────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<BlockchainUiState>(BlockchainUiState.Idle)
    val uiState: StateFlow<BlockchainUiState> = _uiState.asStateFlow()

    // ── Wallet guardada localmente (para actualizar la UI al instante) ───
    private val _walletSaveState = MutableStateFlow<BlockchainUiState>(BlockchainUiState.Idle)
    val walletSaveState: StateFlow<BlockchainUiState> = _walletSaveState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val uid = prefs.userId.first()

            // Cargar en paralelo
            val configResult  = repo.getBlockchainConfig()
            val userResult    = if (uid.isNotBlank()) repo.getUser(uid) else Result.success(null)
            val retirosResult = if (uid.isNotBlank()) repo.getRetiros(uid) else Result.success(emptyList())
            val countResult   = repo.getUserCount()

            _config.value    = configResult.getOrNull() ?: BlockchainConfig()
            _user.value      = userResult.getOrNull()
            _retiros.value   = retirosResult.getOrNull() ?: emptyList()
            _userCount.value = countResult.getOrNull() ?: 0L
            _isLoading.value = false
        }
    }

    fun saveWallet(address: String) {
        viewModelScope.launch {
            val uid = prefs.userId.first()
            if (uid.isBlank()) return@launch
            _walletSaveState.value = BlockchainUiState.Loading
            repo.saveWalletAddress(uid, address).fold(
                onSuccess = {
                    _user.value = _user.value?.copy(walletAddress = address.trim())
                    _walletSaveState.value = BlockchainUiState.Success
                },
                onFailure = { _walletSaveState.value = BlockchainUiState.Error(it.message ?: "Error") }
            )
        }
    }

    fun solicitarRetiro(moveAmount: Int, walletDestino: String) {
        viewModelScope.launch {
            val uid    = prefs.userId.first()
            val cfg    = _config.value ?: return@launch
            val user   = _user.value  ?: return@launch
            if (uid.isBlank()) return@launch

            // Validaciones
            val disponible = user.tokensActuales
            when {
                moveAmount < cfg.minRetiro  -> { _uiState.value = BlockchainUiState.Error("Mínimo ${cfg.minRetiro} MOVE"); return@launch }
                moveAmount > cfg.maxRetiro  -> { _uiState.value = BlockchainUiState.Error("Máximo ${cfg.maxRetiro} MOVE"); return@launch }
                moveAmount > disponible     -> { _uiState.value = BlockchainUiState.Error("Saldo insuficiente"); return@launch }
                walletDestino.isBlank()     -> { _uiState.value = BlockchainUiState.Error("Ingresá tu wallet"); return@launch }
            }

            val fee   = (moveAmount * cfg.feePercent / 100f).toInt().coerceAtLeast(1)
            val neto  = moveAmount - fee
            val retiro = RetiroBlockchain(
                walletDestino = walletDestino.trim(),
                red           = cfg.red,
                moveTotal     = moveAmount,
                moveFee       = fee,
                moveNeto      = neto,
                feePercent    = cfg.feePercent
            )

            _uiState.value = BlockchainUiState.Loading
            repo.solicitarRetiro(uid, retiro).fold(
                onSuccess = {
                    _user.value = _user.value?.copy(tokensActuales = disponible - moveAmount)
                    _retiros.value = listOf(retiro.copy(id = "nuevo")) + _retiros.value
                    _uiState.value = BlockchainUiState.Success
                },
                onFailure = { _uiState.value = BlockchainUiState.Error(it.message ?: "Error al procesar") }
            )
        }
    }

    fun resetUiState()       { _uiState.value = BlockchainUiState.Idle }
    fun resetWalletState()   { _walletSaveState.value = BlockchainUiState.Idle }
    fun refresh()            { load() }
}

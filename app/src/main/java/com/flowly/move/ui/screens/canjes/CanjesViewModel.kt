package com.flowly.move.ui.screens.canjes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.Canje
import com.flowly.move.data.model.CanjesConfig
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class CanjesUiState {
    object Idle    : CanjesUiState()
    object Loading : CanjesUiState()
    object Success : CanjesUiState()
    data class Error(val msg: String) : CanjesUiState()
}

class CanjesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FlowlyRepository(app)
    private val prefs = UserPreferences(app)

    private val _user     = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _canjes   = MutableStateFlow<List<Canje>>(emptyList())
    val canjes: StateFlow<List<Canje>> = _canjes.asStateFlow()

    private val _uiState  = MutableStateFlow<CanjesUiState>(CanjesUiState.Idle)
    val uiState: StateFlow<CanjesUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _mercadoPagoUrl = MutableStateFlow("")
    val mercadoPagoUrl: StateFlow<String> = _mercadoPagoUrl.asStateFlow()

    private val _canjesConfig = MutableStateFlow(CanjesConfig())
    val canjesConfig: StateFlow<CanjesConfig> = _canjesConfig.asStateFlow()

    private var uid = ""

    init {
        viewModelScope.launch {
            uid = prefs.userId.first()
            // Cargar usuario, historial y configs en paralelo
            launch { loadUser() }
            launch { loadCanjes() }
            launch { repo.getStoreConfig().onSuccess { _mercadoPagoUrl.value = it.mercadoPagoUrl } }
            launch { repo.getCanjesConfig().onSuccess { _canjesConfig.value = it } }
            _isLoading.value = false
        }
    }

    private suspend fun loadUser() {
        repo.getUser(uid).onSuccess { _user.value = it }
    }

    suspend fun loadCanjes() {
        repo.getCanjes(uid).onSuccess { _canjes.value = it }
    }

    fun confirmarCanje(montoLabel: String, moveAmount: Int, categoria: String = "cash") {
        val aliasMP = _user.value?.aliasMercadoPago ?: ""
        if (aliasMP.isBlank()) {
            _uiState.value = CanjesUiState.Error("Necesitás cargar tu alias en tu perfil")
            return
        }
        if ((_user.value?.tokensActuales ?: 0) < moveAmount) {
            _uiState.value = CanjesUiState.Error("Saldo insuficiente")
            return
        }
        viewModelScope.launch {
            _uiState.value = CanjesUiState.Loading
            val canje = Canje(
                uid            = uid,
                nombre         = montoLabel,
                categoria      = categoria,
                montoLabel     = montoLabel,
                moveDescontado = moveAmount,
                aliasDestino   = aliasMP
            )
            repo.createCanje(uid, canje).fold(
                onSuccess = {
                    loadUser()   // refresca saldo
                    loadCanjes() // refresca historial
                    _uiState.value = CanjesUiState.Success
                },
                onFailure = { _uiState.value = CanjesUiState.Error(it.message ?: "Error al procesar el canje") }
            )
        }
    }

    fun clearState() { _uiState.value = CanjesUiState.Idle }
}

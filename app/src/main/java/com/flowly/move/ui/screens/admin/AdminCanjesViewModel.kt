package com.flowly.move.ui.screens.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.model.CanjeOferta
import com.flowly.move.data.model.CanjesConfig
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AdminUiState {
    object Idle    : AdminUiState()
    object Loading : AdminUiState()
    object Success : AdminUiState()
    data class Error(val msg: String) : AdminUiState()
}

class AdminCanjesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FlowlyRepository(app)

    // ── Estado editable (fuente de verdad durante la sesión) ──────────────────

    private val _nivelMinimo  = MutableStateFlow(1)
    val nivelMinimo: StateFlow<Int> = _nivelMinimo.asStateFlow()

    private val _notaMensaje  = MutableStateFlow("Una recompensa por mes · disponible dentro del período de validación")
    val notaMensaje: StateFlow<String> = _notaMensaje.asStateFlow()

    private val _opciones     = MutableStateFlow<List<CanjeOferta>>(emptyList())
    val opciones: StateFlow<List<CanjeOferta>> = _opciones.asStateFlow()

    private val _uiState      = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _isLoading    = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repo.getCanjesConfig().onSuccess { cfg ->
                _nivelMinimo.value = cfg.nivelMinimo
                _notaMensaje.value = cfg.notaMensaje
                _opciones.value    = cfg.opciones
            }
            _isLoading.value = false
        }
    }

    // ── Mutaciones ────────────────────────────────────────────────────────────

    fun setNivelMinimo(v: Int) {
        _nivelMinimo.value = v.coerceIn(1, 10)
    }

    fun setNotaMensaje(v: String) {
        _notaMensaje.value = v
    }

    fun updateLabel(index: Int, label: String) {
        _opciones.value = _opciones.value.toMutableList().also {
            it[index] = it[index].copy(label = label)
        }
    }

    /** @param moveStr texto del campo — acepta "" (lo trata como 0) */
    fun updateMove(index: Int, moveStr: String) {
        val move = moveStr.filter { it.isDigit() }.toIntOrNull() ?: 0
        _opciones.value = _opciones.value.toMutableList().also {
            it[index] = it[index].copy(move = move)
        }
    }

    fun toggleActivo(index: Int) {
        _opciones.value = _opciones.value.toMutableList().also {
            it[index] = it[index].copy(activo = !it[index].activo)
        }
    }

    fun removeOferta(index: Int) {
        _opciones.value = _opciones.value.toMutableList().also { it.removeAt(index) }
    }

    fun addOferta() {
        _opciones.value = _opciones.value + CanjeOferta(
            id     = "opcion_${System.currentTimeMillis()}",
            label  = "",
            move   = 0,
            activo = true
        )
    }

    // ── Guardar ───────────────────────────────────────────────────────────────

    fun guardar() {
        // Validaciones básicas
        val invalidas = _opciones.value.filter { it.label.isBlank() || it.move <= 0 }
        if (invalidas.isNotEmpty()) {
            _uiState.value = AdminUiState.Error(
                "Hay ${invalidas.size} opción(es) con label vacío o MOVE = 0. Corregilas antes de guardar."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            val config = CanjesConfig(
                opciones    = _opciones.value,
                notaMensaje = _notaMensaje.value.trim(),
                nivelMinimo = _nivelMinimo.value
            )
            repo.saveCanjesConfig(config).fold(
                onSuccess = { _uiState.value = AdminUiState.Success },
                onFailure = { _uiState.value = AdminUiState.Error(it.message ?: "Error al guardar en Firestore") }
            )
        }
    }

    fun clearState() { _uiState.value = AdminUiState.Idle }
}

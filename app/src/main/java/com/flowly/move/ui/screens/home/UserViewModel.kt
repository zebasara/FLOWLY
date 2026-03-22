package com.flowly.move.ui.screens.home

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.AuthRepository
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UserViewModel(app: Application) : AndroidViewModel(app) {

    private val repository       = AuthRepository(app.applicationContext)
    private val flowlyRepository = FlowlyRepository(app.applicationContext)
    private val prefs            = UserPreferences(app.applicationContext)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Error de red ──────────────────────────────────────────────────────
    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError.asStateFlow()

    // ── Diálogos ──────────────────────────────────────────────────────────
    // La secuencia es: primero welcome (si aplica), luego badge uno por uno.
    // pendingBadge solo se activa DESPUÉS de que se cierre el welcome dialog.

    private val _showWelcomeDialog = MutableStateFlow(false)
    val showWelcomeDialog: StateFlow<Boolean> = _showWelcomeDialog.asStateFlow()

    private val _pendingBadge = MutableStateFlow<String?>(null)
    val pendingBadge: StateFlow<String?> = _pendingBadge.asStateFlow()

    init {
        loadUser()
    }

    /** Devuelve true si hay conexión a internet activa */
    private fun isConnected(): Boolean {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            val uid = prefs.userId.first()
            if (uid.isBlank()) { _isLoading.value = false; return@launch }

            if (!isConnected()) {
                _isLoading.value = false
                _networkError.value = "Sin conexión. Revisá tu internet y volvé a intentar."
                return@launch
            }

            repository.getUser(uid).fold(
                onSuccess = { u ->
                    var finalUser = u
                    // Para usuarios existentes sin badges, otorgar soy_move
                    if (u != null && u.badges.isEmpty()) {
                        flowlyRepository.garantizarBadgeBienvenida(uid)
                        finalUser = u.copy(badges = listOf("soy_move"))
                    }
                    // Verificar subida de nivel automática al abrir la app
                    if (u != null) {
                        val subio = flowlyRepository.verificarYSubirNivel(uid)
                        if (subio) {
                            // Recargar usuario con el nivel actualizado
                            finalUser = flowlyRepository.getUser(uid).getOrNull() ?: finalUser
                        }
                    }
                    _user.value = finalUser
                    finalUser?.let { checkDialogs(it) }
                    // Verificar y asignar campeón semanal (solo lunes >= 15:00)
                    flowlyRepository.checkAndAssignCampeon()
                },
                onFailure = { e ->
                    // Fallo de red: mostrar aviso no bloqueante
                    val msg = e.message?.lowercase() ?: ""
                    if (msg.contains("network") || msg.contains("timeout") ||
                        msg.contains("unable to resolve") || !isConnected()
                    ) {
                        _networkError.value = "Sin conexión. Verificá tu internet."
                    }
                }
            )
            _isLoading.value = false
        }
    }

    private suspend fun checkDialogs(user: User) {
        val welcomeShown = prefs.welcomeDialogShown.first()
        if (!welcomeShown) {
            // Mostrar welcome primero; badges se encolan para después
            _showWelcomeDialog.value = true
        } else {
            // Welcome ya fue visto: mostrar primera insignia pendiente
            checkPendingBadge(user)
        }
    }

    private suspend fun checkPendingBadge(user: User) {
        val shown   = prefs.shownBadges.first()
        val pending = user.badges.firstOrNull { it !in shown }
        _pendingBadge.value = pending
    }

    fun dismissWelcomeDialog() {
        viewModelScope.launch {
            prefs.setWelcomeDialogShown()
            _showWelcomeDialog.value = false
            // Ahora mostrar la primera insignia pendiente
            _user.value?.let { checkPendingBadge(it) }
        }
    }

    fun dismissBadgeDialog(badgeId: String) {
        viewModelScope.launch {
            prefs.markBadgeShown(badgeId)
            val shown   = prefs.shownBadges.first()
            val pending = _user.value?.badges?.firstOrNull { it !in shown }
            _pendingBadge.value = pending
        }
    }

    fun dismissNetworkError() { _networkError.value = null }

    fun refresh() = loadUser()

    /** Refresca datos sin mostrar el spinner de carga (para volver desde otras pantallas). */
    fun refreshSilently() {
        if (_isLoading.value) return   // carga inicial en curso, no duplicar
        viewModelScope.launch {
            val uid = prefs.userId.first()
            if (uid.isBlank()) return@launch
            repository.getUser(uid).fold(
                onSuccess = { u -> if (u != null) _user.value = u },
                onFailure = { }
            )
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.signOut()
            prefs.clearAll()
            onDone()
        }
    }
}

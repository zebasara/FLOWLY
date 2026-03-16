package com.flowly.move.ui.screens.home

import android.app.Application
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

    // Diálogos de bienvenida e insignias
    private val _showWelcomeDialog = MutableStateFlow(false)
    val showWelcomeDialog: StateFlow<Boolean> = _showWelcomeDialog.asStateFlow()

    // Cola de insignias pendientes de mostrar
    private val _pendingBadge = MutableStateFlow<String?>(null)
    val pendingBadge: StateFlow<String?> = _pendingBadge.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val uid = prefs.userId.first()
            if (uid.isBlank()) { _isLoading.value = false; return@launch }
            repository.getUser(uid).fold(
                onSuccess = { u ->
                    _user.value = u
                    u?.let { checkDialogs(it) }
                },
                onFailure = { /* red/perm issue: leave null */ }
            )
            _isLoading.value = false
        }
    }

    private suspend fun checkDialogs(user: User) {
        // 1. Diálogo de bienvenida (100 MOVE)
        val welcomeShown = prefs.welcomeDialogShown.first()
        if (!welcomeShown) {
            _showWelcomeDialog.value = true
        }

        // 2. Insignias nuevas pendientes de mostrar
        val shown = prefs.shownBadges.first()
        val pending = user.badges.firstOrNull { it !in shown }
        _pendingBadge.value = pending
    }

    fun dismissWelcomeDialog() {
        viewModelScope.launch {
            prefs.setWelcomeDialogShown()
            _showWelcomeDialog.value = false
        }
    }

    fun dismissBadgeDialog(badgeId: String) {
        viewModelScope.launch {
            prefs.markBadgeShown(badgeId)
            // Verificar si hay más insignias pendientes
            val shown   = prefs.shownBadges.first()
            val pending = _user.value?.badges?.firstOrNull { it !in shown }
            _pendingBadge.value = pending
        }
    }

    fun refresh() = loadUser()

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.signOut()
            prefs.clearAll()
            onDone()
        }
    }
}

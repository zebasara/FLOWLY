package com.flowly.move.ui.screens.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.Notificacion
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotificationsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FlowlyRepository(app)
    private val prefs = UserPreferences(app)

    private val _notifs    = MutableStateFlow<List<Notificacion>>(emptyList())
    val notifs: StateFlow<List<Notificacion>> = _notifs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var uid = ""

    init {
        viewModelScope.launch {
            uid = prefs.userId.first()
            reload()
        }
    }

    fun reload() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.getNotificaciones(uid).onSuccess { _notifs.value = it }
            _isLoading.value = false
        }
    }

    fun marcarLeida(notifId: String) {
        viewModelScope.launch {
            repo.marcarLeida(uid, notifId).onSuccess {
                _notifs.value = _notifs.value.map {
                    if (it.id == notifId) it.copy(leida = true) else it
                }
            }
        }
    }
}

package com.flowly.move.ui.screens.store

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.StoreConfig
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoreViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = FlowlyRepository(app.applicationContext)
    private val prefs      = UserPreferences(app)

    private val _user        = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // Tiempo real: se actualiza automáticamente cuando el admin cambia la config
    val storeConfig: StateFlow<StoreConfig?> = repository.storeConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _userCount   = MutableStateFlow(0L)
    val userCount: StateFlow<Long> = _userCount.asStateFlow()

    private val _isLoading   = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val uid      = prefs.userId.first()
            val userDef  = launch { _user.value      = repository.getUser(uid).getOrNull() }
            val countDef = launch { _userCount.value = repository.getUserCount().getOrElse { 0L } }
            userDef.join()
            countDef.join()
            _isLoading.value = false
        }
    }

    fun reload() = load()
}

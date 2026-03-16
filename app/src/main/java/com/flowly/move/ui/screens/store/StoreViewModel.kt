package com.flowly.move.ui.screens.store

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.model.StoreConfig
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoreViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = FlowlyRepository(app.applicationContext)
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _user        = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _storeConfig = MutableStateFlow<StoreConfig?>(null)
    val storeConfig: StateFlow<StoreConfig?> = _storeConfig.asStateFlow()

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
            // Cargar en paralelo
            val userDef   = launch { _user.value        = repository.getUser(uid).getOrNull() }
            val configDef = launch { _storeConfig.value = repository.getStoreConfig().getOrNull() }
            val countDef  = launch { _userCount.value   = repository.getUserCount().getOrElse { 0L } }
            userDef.join()
            configDef.join()
            countDef.join()
            _isLoading.value = false
        }
    }

    fun reload() = load()
}

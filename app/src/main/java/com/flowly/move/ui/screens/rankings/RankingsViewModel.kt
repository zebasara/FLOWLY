package com.flowly.move.ui.screens.rankings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.CampeonSemanal
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RankingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FlowlyRepository(app)
    private val prefs = UserPreferences(app)

    private val _currentUser  = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _rankings     = MutableStateFlow<List<User>>(emptyList())
    val rankings: StateFlow<List<User>> = _rankings.asStateFlow()

    private val _campeon      = MutableStateFlow<CampeonSemanal?>(null)
    val campeon: StateFlow<CampeonSemanal?> = _campeon.asStateFlow()

    /**
     * Siempre el #1 de Argentina (global), independiente del tab seleccionado.
     * Es el "potencial campeón" que verá la cuenta regresiva.
     */
    private val _topArgentina = MutableStateFlow<User?>(null)
    val topArgentina: StateFlow<User?> = _topArgentina.asStateFlow()

    private val _isLoading    = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var uid       = ""
    private var ciudad    = ""
    private var provincia = ""

    init {
        viewModelScope.launch {
            uid = prefs.userId.first()
            repo.getUser(uid).onSuccess {
                _currentUser.value = it
                ciudad    = it?.ciudad    ?: ""
                provincia = it?.provincia ?: ""
            }
            // Cargar campeón, top Argentina y rankings en paralelo
            launch { loadCampeon() }
            launch { loadTopArgentina() }
            loadRankings("all")
        }
    }

    private suspend fun loadCampeon() {
        repo.getCampeonSemanal().onSuccess { _campeon.value = it }
    }

    /** Carga solo el #1 global de Argentina (limit 1 del ranking general). */
    private suspend fun loadTopArgentina() {
        repo.getRankings("all", "", "").onSuccess {
            _topArgentina.value = it.firstOrNull()
        }
    }

    fun loadRankings(scope: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.getRankings(scope, ciudad, provincia).onSuccess {
                _rankings.value = it
                // Si volvemos al tab Argentina, actualizar también el top
                if (scope == "all") _topArgentina.value = it.firstOrNull()
            }
            _isLoading.value = false
        }
    }

    fun refreshCampeon() {
        viewModelScope.launch { loadCampeon() }
    }
}

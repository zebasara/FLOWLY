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

    /** Campeón nuevo que aún no vio este usuario (distinto al ganador). */
    private val _showNewCampeonDialog = MutableStateFlow<CampeonSemanal?>(null)
    val showNewCampeonDialog: StateFlow<CampeonSemanal?> = _showNewCampeonDialog.asStateFlow()

    /** El usuario logueado ES el campeón y aún no vio su celebración. */
    private val _showCampeonCelebration = MutableStateFlow(false)
    val showCampeonCelebration: StateFlow<Boolean> = _showCampeonCelebration.asStateFlow()

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
        repo.getCampeonSemanal().onSuccess { c ->
            if (c == null || c.uid.isBlank() || c.semana.isBlank()) {
                _campeon.value = c
                return@onSuccess
            }
            // Actualizar tokens con el valor real actual del usuario
            val tokensReales = repo.getUser(c.uid).getOrNull()?.tokensActuales ?: c.tokensActuales
            _campeon.value = c.copy(tokensActuales = tokensReales)

            // Clave compuesta semana|uid: detecta cambio de campeón dentro de la misma semana
            val campeonKey = "${c.semana}|${c.uid}"
            if (c.uid == uid) {
                // El usuario logueado es el campeón — mostrar celebración si no la vio
                val lastCelebrated = prefs.lastCelebratedCampeonSemana.first()
                if (lastCelebrated != campeonKey) _showCampeonCelebration.value = true
            } else {
                // Otro usuario es el campeón — mostrar anuncio si no lo vio
                val lastSeen = prefs.lastSeenCampeonSemana.first()
                if (lastSeen != campeonKey) _showNewCampeonDialog.value = c
            }
        }
    }

    fun dismissNewCampeonDialog() {
        val c = _campeon.value ?: return
        _showNewCampeonDialog.value = null
        viewModelScope.launch { prefs.markCampeonSemanaVista("${c.semana}|${c.uid}") }
    }

    fun dismissCelebration() {
        val c = _campeon.value ?: return
        _showCampeonCelebration.value = false
        viewModelScope.launch { prefs.markCampeonCelebrado("${c.semana}|${c.uid}") }
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
            _rankings.value = emptyList()
            repo.getRankings(scope, ciudad, provincia)
                .onSuccess {
                    _rankings.value = it
                    if (scope == "all") _topArgentina.value = it.firstOrNull()
                }
                .onFailure {
                    _rankings.value = emptyList()
                }
            _isLoading.value = false
        }
    }

    fun refreshCampeon() {
        viewModelScope.launch { loadCampeon() }
    }
}

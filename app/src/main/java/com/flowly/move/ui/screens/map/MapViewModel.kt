package com.flowly.move.ui.screens.map

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.SesionActiva
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import com.flowly.move.services.TrackingController
import com.flowly.move.services.TrackingForegroundService
import com.flowly.move.services.TrackingStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FlowlyRepository(app)
    private val prefs = UserPreferences(app)

    // ── Stats del GPS ──────────────────────────────────────────────────────
    val stats: StateFlow<TrackingStats> = TrackingController.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackingController.stats.value)

    // ── Usuario actual ─────────────────────────────────────────────────────
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // ── Sesiones activas en tiempo real (otros usuarios en el mapa) ────────
    val activeSessions: StateFlow<List<SesionActiva>> = repo.getSesionesActivasFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Consentimiento de compartir ubicación ──────────────────────────────
    val consentGiven: StateFlow<Boolean> = prefs.locationSharingConsented
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            val uid = prefs.userId.first()
            if (uid.isNotBlank()) {
                repo.getUser(uid).onSuccess { _currentUser.value = it }
            }
        }
    }

    /** Guarda el consentimiento de compartir ubicación en DataStore */
    fun acceptLocationConsent() {
        viewModelScope.launch { prefs.setLocationSharingConsented() }
    }

    /** Inicia MOVErme — pasa datos del usuario al servicio para que publique en Firestore */
    fun startTracking(context: Context) {
        viewModelScope.launch {
            TrackingController.reset()
            val uid      = prefs.userId.first()
            val user     = _currentUser.value
            val nombre   = user?.nombre?.takeIf { it.isNotBlank() } ?: prefs.userName.first()
            val photoUrl = user?.profilePhotoUrl ?: ""

            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = TrackingForegroundService.ACTION_START
                putExtra(TrackingForegroundService.EXTRA_UID,       uid)
                putExtra(TrackingForegroundService.EXTRA_NOMBRE,    nombre)
                putExtra(TrackingForegroundService.EXTRA_PHOTO_URL, photoUrl)
            }
            context.startForegroundService(intent)
        }
    }

    /** Detiene MOVErme y guarda la sesión en Firestore */
    fun stopTracking(context: Context) {
        val finalStats        = TrackingController.stats.value
        val remainingDistance = finalStats.distanceMeters - finalStats.distanceCreditedMeters

        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_STOP
        }
        context.startService(intent)

        if (finalStats.distanceMeters > 50f) {
            viewModelScope.launch {
                val uid = prefs.userId.first()
                if (uid.isNotBlank()) {
                    if (remainingDistance > 0f) {
                        // Distancia aún no acreditada (puede ser < 50 m al cierre de sesión)
                        // → crédito final + notificación + badges
                        repo.registrarSesionMovimiento(uid, remainingDistance)
                    } else {
                        // Todo fue acreditado progresivamente → solo verificar badges
                        repo.verificarBadgesDistancia(uid)
                    }
                }
            }
        }
    }
}

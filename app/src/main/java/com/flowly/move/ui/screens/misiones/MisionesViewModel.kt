package com.flowly.move.ui.screens.misiones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import com.flowly.move.data.repository.FlowlyRepository.Companion.VIDEO_REWARD_AMOUNT
import com.flowly.move.data.repository.FlowlyRepository.Companion.VIDEO_BONUS_AMOUNT
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── Modelo de misión ──────────────────────────────────────────────────────────

data class DailyMision(
    val id: String,
    val emoji: String,
    val titulo: String,
    val descripcion: String,
    val recompensaMove: Int,
    val progreso: Float,       // 0f..1f
    val completada: Boolean,
    val reclamada: Boolean
)

fun getMisionesDelDia(user: User): List<DailyMision> {
    val kmHoy      = user.kmHoy
    val videosHoy  = when {
        user.tokenVideosHoy <= 0 -> 0
        user.tokenVideosHoy <= FlowlyRepository.DAILY_LIMIT_VIDEOS -> (user.tokenVideosHoy / VIDEO_REWARD_AMOUNT).coerceAtLeast(1)
        else -> 4 + ((user.tokenVideosHoy - FlowlyRepository.DAILY_LIMIT_VIDEOS) / VIDEO_BONUS_AMOUNT)
    }
    val racha      = user.diasConsecutivosVideos
    val reclamadas = user.misionesReclamadasHoy

    return listOf(
        DailyMision(
            id             = "caminar_2km",
            emoji          = "🏃",
            titulo         = "Caminante del día",
            descripcion    = "Caminá 2 km hoy para ganar tu bonus",
            recompensaMove = 200,
            progreso       = (kmHoy / 2f).coerceIn(0f, 1f),
            completada     = kmHoy >= 2f,
            reclamada      = "caminar_2km" in reclamadas
        ),
        DailyMision(
            id             = "videos_4",
            emoji          = "🎬",
            titulo         = "Maratonista de contenido",
            descripcion    = "Ve 4 videos hoy para reclamar tu recompensa",
            recompensaMove = 100,
            progreso       = (videosHoy / 4f).coerceIn(0f, 1f),
            completada     = videosHoy >= 4,
            reclamada      = "videos_4" in reclamadas
        ),
        DailyMision(
            id             = "racha_3",
            emoji          = "🔥",
            titulo         = "En racha",
            descripcion    = "Mantené tu racha de videos por 3 días seguidos",
            recompensaMove = 150,
            progreso       = (racha / 3f).coerceIn(0f, 1f),
            completada     = racha >= 3,
            reclamada      = "racha_3" in reclamadas
        )
    )
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MisionesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FlowlyRepository(app)
    private val uid  get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _user      = MutableStateFlow<User?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _error     = MutableStateFlow<String?>(null)
    private val _success   = MutableStateFlow<String?>(null)

    val user:      StateFlow<User?>    = _user
    val isLoading: StateFlow<Boolean>  = _isLoading
    val error:     StateFlow<String?>  = _error
    val success:   StateFlow<String?>  = _success

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            // ensureDailyReset verifica el cambio de día y resetea misiones si corresponde
            repo.ensureDailyReset(uid).onSuccess { _user.value = it }
            _isLoading.value = false
        }
    }

    fun reclamar(mision: DailyMision) {
        if (!mision.completada || mision.reclamada) return
        viewModelScope.launch {
            repo.reclamarMision(uid, mision.id, mision.recompensaMove)
                .onSuccess {
                    _success.value = "+${mision.recompensaMove} MOVE reclamados 🎉"
                    load()
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearMessages() {
        _error.value   = null
        _success.value = null
    }
}

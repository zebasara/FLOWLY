package com.flowly.move.ui.screens.fondo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.FondoPremios
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class FondoPremiosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FlowlyRepository(app)
    private val prefs = UserPreferences(app)

    private val _fondo       = MutableStateFlow<FondoPremios?>(null)
    val fondo: StateFlow<FondoPremios?> = _fondo.asStateFlow()

    /** Monto total ya convertido a ARS usando el dólar blue actual. 0 si aún carga. */
    private val _montoARS    = MutableStateFlow(0L)
    val montoARS: StateFlow<Long> = _montoARS.asStateFlow()

    private val _ranking     = MutableStateFlow<List<User>>(emptyList())
    val ranking: StateFlow<List<User>> = _ranking.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading   = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Tipo de cambio blue cacheado — se obtiene una vez y se reutiliza en cada update del fondo
    private var cachedBlueRate = 0.0

    init {
        viewModelScope.launch {
            val uid = prefs.userId.first()
            launch { repo.getUser(uid).onSuccess { _currentUser.value = it } }
            launch { repo.getRankingMensual().onSuccess { _ranking.value = it } }

            // Obtener el blue rate una sola vez al abrir la pantalla
            cachedBlueRate = fetchBlueRate()

            // Escuchar cambios en tiempo real del fondo
            repo.fondoPremiosFlow()
                .onEach { f ->
                    _fondo.value = f
                    val rate = if (cachedBlueRate > 0.0) cachedBlueRate else f?.blueRateCache ?: 0.0
                    _montoARS.value = if (f != null && f.montoDolares > 0.0 && rate > 0.0)
                        (f.montoDolares * rate).toLong() else 0L
                    _isLoading.value = false
                }
                .launchIn(viewModelScope)
        }
    }

    /** Consulta el tipo de cambio del dólar blue desde la API pública de bluelytics.  */
    private suspend fun fetchBlueRate(): Double = withContext(Dispatchers.IO) {
        try {
            val json = URL("https://api.bluelytics.com.ar/v2/latest").readText()
            JSONObject(json).getJSONObject("blue").getDouble("value_avg")
        } catch (e: Exception) {
            0.0
        }
    }
}

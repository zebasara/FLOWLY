package com.flowly.move.ui.screens.auth

import android.app.Activity
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
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val uid: String, val isNewUser: Boolean = false) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repository       = AuthRepository(app.applicationContext)
    private val flowlyRepository = FlowlyRepository(app.applicationContext)
    private val prefs            = UserPreferences(app.applicationContext)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Perfil existente cargado desde Firestore.
     * Se usa para pre-llenar CompleteProfileScreen cuando el usuario reinstala la app.
     */
    private val _existingUser = MutableStateFlow<User?>(null)
    val existingUser: StateFlow<User?> = _existingUser.asStateFlow()

    // ── Email / Password ────────────────────────────────────────

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Completá todos los campos")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.loginWithEmail(email, password).fold(
                onSuccess = { uid ->
                    val profileOk = repository.isProfileComplete(uid)
                    prefs.setLoggedIn(uid, email, "")
                    if (profileOk) {
                        val user = flowlyRepository.getUser(uid).getOrNull()
                        prefs.setProfileComplete(user?.nombre ?: "")
                    }
                    _uiState.value = AuthUiState.Success(uid, isNewUser = !profileOk)
                },
                onFailure = { _uiState.value = AuthUiState.Error(friendlyError(it.message)) }
            )
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Completá todos los campos")
            return
        }
        if (password.length < 8) {
            _uiState.value = AuthUiState.Error("La contraseña debe tener al menos 8 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.registerWithEmail(email, password).fold(
                onSuccess = { uid ->
                    prefs.setLoggedIn(uid, email, "")
                    _uiState.value = AuthUiState.Success(uid, isNewUser = true)
                },
                onFailure = { _uiState.value = AuthUiState.Error(friendlyError(it.message)) }
            )
        }
    }

    // ── Google ──────────────────────────────────────────────────

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.signInWithGoogle(activity).fold(
                onSuccess = { (uid, isNew) ->
                    val email = repository.currentUser?.email ?: ""
                    prefs.setLoggedIn(uid, email, "")
                    if (!isNew) {
                        val profileOk = repository.isProfileComplete(uid)
                        if (profileOk) {
                            val user = flowlyRepository.getUser(uid).getOrNull()
                            prefs.setProfileComplete(user?.nombre ?: "")
                        }
                        _uiState.value = AuthUiState.Success(uid, isNewUser = !profileOk)
                    } else {
                        _uiState.value = AuthUiState.Success(uid, isNewUser = true)
                    }
                },
                onFailure = { _uiState.value = AuthUiState.Error(friendlyError(it.message)) }
            )
        }
    }

    // ── Carga perfil existente ────────────────────────────────────

    /**
     * Carga el perfil del usuario desde Firestore y lo expone en [existingUser].
     * Llamar desde CompleteProfileScreen para pre-llenar los campos si el usuario
     * ya tiene datos (reinstalación / cambio de teléfono).
     */
    fun loadExistingProfile(uid: String) {
        viewModelScope.launch {
            _existingUser.value = flowlyRepository.getUser(uid).getOrNull()
        }
    }

    // ── Save profile (after registration / Google) ───────────────

    /**
     * Guarda el perfil del usuario.
     *
     * ⚠️  REGLA CRÍTICA: si el usuario YA EXISTE en Firestore (reinstalación, cambio
     * de teléfono, etc.) se actualiza SOLO los campos de perfil con updateProfile().
     * Nunca se sobreescribe tokensActuales, nivel, limiteTokens, badges, historial,
     * createdAt ni ningún otro dato de juego — de lo contrario el usuario perdería
     * todo su progreso acumulado.
     *
     * Si el usuario es NUEVO se crea el documento completo con los valores iniciales.
     */
    fun saveProfile(
        uid: String,
        nombre: String,
        telefono: String,
        provincia: String,
        ciudad: String,
        alias: String = "",
        referralCode: String = ""
    ) {
        if (nombre.isBlank() || telefono.isBlank() || provincia.isBlank() || ciudad.isBlank()) {
            _uiState.value = AuthUiState.Error("Completá todos los campos obligatorios")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Preferir el usuario ya cargado; si no, consultarlo ahora
            val existingInFirestore = _existingUser.value
                ?: flowlyRepository.getUser(uid).getOrNull()

            val result = if (existingInFirestore != null) {
                // ─────────────────────────────────────────────────────────
                // USUARIO EXISTENTE (reinstalación / cambio de teléfono)
                // updateProfile() solo toca nombre, telefono, provincia,
                // ciudad, aliasMercadoPago — el resto queda intacto
                // ─────────────────────────────────────────────────────────
                flowlyRepository.updateProfile(
                    uid       = uid,
                    nombre    = nombre.trim(),
                    telefono  = telefono.trim(),
                    provincia = provincia.trim(),
                    ciudad    = ciudad.trim(),
                    alias     = alias.trim()
                )
            } else {
                // ─────────────────────────────────────────────────────────
                // USUARIO NUEVO — crear documento completo con defaults
                // ─────────────────────────────────────────────────────────
                val email = repository.currentUser?.email ?: ""
                val user = User(
                    uid              = uid,
                    nombre           = nombre.trim(),
                    email            = email,
                    telefono         = telefono.trim(),
                    provincia        = provincia.trim(),
                    ciudad           = ciudad.trim(),
                    aliasMercadoPago = alias.trim(),
                    nivel            = 1,
                    limiteTokens     = 20_000,   // Nivel 1 por defecto
                    tokensActuales   = 100,       // Bono de registro
                    badges           = listOf("soy_move"),
                    createdAt        = System.currentTimeMillis()
                )
                repository.saveUser(user)
            }

            result.fold(
                onSuccess = {
                    prefs.setProfileComplete(nombre.trim())
                    // Código de referido: solo para usuarios nuevos
                    // Acepta tanto código corto (8 chars) como URL completa
                    if (existingInFirestore == null && referralCode.isNotBlank()) {
                        val cleanCode = referralCode.trim().substringAfterLast("/").trim()
                        if (cleanCode.isNotBlank()) {
                            val referidorUid = flowlyRepository
                                .findUidByReferralCode(cleanCode)
                                .getOrNull()
                            if (referidorUid != null && referidorUid != uid) {
                                // +200 MOVE al referidor + insignias + notificación
                                flowlyRepository.registrarReferido(referidorUid)
                                    .onFailure { /* fallo silencioso: referido no acreditado */ }
                                // +100 MOVE al nuevo usuario por usar código válido
                                flowlyRepository.otorgarBonoReferido(uid)
                                    .onFailure { /* fallo silencioso: bono no acreditado */ }
                            }
                        }
                    }
                    _uiState.value = AuthUiState.Success(uid, isNewUser = false)
                },
                onFailure = { _uiState.value = AuthUiState.Error(friendlyError(it.message)) }
            )
        }
    }

    // ── Recuperar contraseña ─────────────────────────────────────

    private val _resetState = MutableStateFlow<String?>(null)
    val resetState: StateFlow<String?> = _resetState.asStateFlow()

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _resetState.value = "error:Ingresá tu email"
            return
        }
        viewModelScope.launch {
            repository.sendPasswordReset(email).fold(
                onSuccess = { _resetState.value = "ok" },
                onFailure = { _resetState.value = "error:${friendlyError(it.message)}" }
            )
        }
    }

    fun clearResetState() { _resetState.value = null }

    fun clearError() { _uiState.value = AuthUiState.Idle }

    private fun friendlyError(msg: String?): String = when {
        msg == null                          -> "Error desconocido"
        "password" in msg                    -> "Contraseña incorrecta"
        "no user" in msg.lowercase()         -> "No existe una cuenta con ese email"
        "email" in msg && "format" in msg    -> "Formato de email inválido"
        "already in use" in msg              -> "El email ya está registrado"
        "network" in msg.lowercase()         -> "Sin conexión a internet"
        "credential" in msg.lowercase()      -> "Error con las credenciales de Google"
        else                                 -> msg
    }
}

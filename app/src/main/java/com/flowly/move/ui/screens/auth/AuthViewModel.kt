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
                    if (profileOk) prefs.setProfileComplete()
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
                        if (profileOk) prefs.setProfileComplete()
                        _uiState.value = AuthUiState.Success(uid, isNewUser = !profileOk)
                    } else {
                        _uiState.value = AuthUiState.Success(uid, isNewUser = true)
                    }
                },
                onFailure = { _uiState.value = AuthUiState.Error(friendlyError(it.message)) }
            )
        }
    }

    // ── Save profile (after registration / Google) ───────────────

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
                limiteTokens     = 2_000,
                tokensActuales   = 100, // bono de registro
                badges           = listOf("soy_move"),
                createdAt        = System.currentTimeMillis()
            )
            repository.saveUser(user).fold(
                onSuccess = {
                    prefs.setProfileComplete(nombre)
                    // Procesar código de referido
                    if (referralCode.isNotBlank()) {
                        flowlyRepository.findUidByReferralCode(referralCode)
                            .getOrNull()
                            ?.let { referidorUid ->
                                if (referidorUid != uid) {
                                    flowlyRepository.registrarReferido(referidorUid)
                                    // Guardar quién nos refirió
                                    flowlyRepository.updateProfile(uid, nombre.trim(), "", "", "", "")
                                }
                            }
                    }
                    _uiState.value = AuthUiState.Success(uid, isNewUser = false)
                },
                onFailure = { _uiState.value = AuthUiState.Error(friendlyError(it.message)) }
            )
        }
    }

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

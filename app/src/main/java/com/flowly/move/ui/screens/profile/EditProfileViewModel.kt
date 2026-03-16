package com.flowly.move.ui.screens.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EditProfileUiState {
    object Idle    : EditProfileUiState()
    object Loading : EditProfileUiState()
    object Success : EditProfileUiState()
    data class Error(val msg: String) : EditProfileUiState()
}

class EditProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = FlowlyRepository(app.applicationContext)
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _user    = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    init {
        viewModelScope.launch {
            _user.value = repository.getUser(uid).getOrNull()
        }
    }

    fun saveProfile(nombre: String, telefono: String, provincia: String, ciudad: String, alias: String) {
        if (nombre.isBlank()) {
            _uiState.value = EditProfileUiState.Error("El nombre es obligatorio")
            return
        }
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            repository.updateProfile(uid, nombre, telefono, provincia, ciudad, alias).fold(
                onSuccess = { _uiState.value = EditProfileUiState.Success },
                onFailure = { _uiState.value = EditProfileUiState.Error(it.message ?: "Error al guardar") }
            )
        }
    }

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            repository.uploadProfilePhoto(uid, uri).fold(
                onSuccess = { url ->
                    _user.value = _user.value?.copy(profilePhotoUrl = url)
                    _isUploadingPhoto.value = false
                },
                onFailure = {
                    _uiState.value = EditProfileUiState.Error("Error al subir foto: ${it.message}")
                    _isUploadingPhoto.value = false
                }
            )
        }
    }

    fun clearState() { _uiState.value = EditProfileUiState.Idle }
}

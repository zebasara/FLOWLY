package com.flowly.move.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.flowly.move.data.model.iniciales
import com.flowly.move.data.model.PROVINCIAS_ARGENTINA
import com.flowly.move.data.model.ciudadesDe
import com.flowly.move.ui.components.*
import com.flowly.move.ui.theme.*

@Composable
fun EditProfileScreen(navController: NavController) {
    val vm: EditProfileViewModel = viewModel()
    val user             by vm.user.collectAsStateWithLifecycle()
    val uiState          by vm.uiState.collectAsStateWithLifecycle()
    val isUploadingPhoto by vm.isUploadingPhoto.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }

    // Inicializar campos con datos actuales
    var nombre    by remember(user) { mutableStateOf(user?.nombre    ?: "") }
    var telefono  by remember(user) { mutableStateOf(user?.telefono  ?: "") }
    var provincia by remember(user) { mutableStateOf(user?.provincia ?: "") }
    var ciudad    by remember(user) { mutableStateOf(user?.ciudad    ?: "") }
    var aliasMP   by remember(user) { mutableStateOf(user?.aliasMercadoPago ?: "") }

    // Ciudades disponibles según la provincia seleccionada
    val ciudadesDisponibles = remember(provincia) { ciudadesDe(provincia) }

    // Al cambiar provincia, limpiar ciudad si ya no pertenece a la nueva lista
    LaunchedEffect(provincia) {
        val nuevasCiudades = ciudadesDe(provincia)
        if (ciudad.isNotBlank() && ciudad !in nuevasCiudades) ciudad = ""
    }

    // Foto seleccionada localmente (preview antes de subir)
    var localPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            localPhotoUri = it
            vm.uploadPhoto(it)
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is EditProfileUiState.Success -> {
                snackbar.showSnackbar("Perfil actualizado ✓")
                vm.clearState()
                navController.popBackStack()
            }
            is EditProfileUiState.Error -> {
                snackbar.showSnackbar((uiState as EditProfileUiState.Error).msg)
                vm.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = FlowlyBg,
        snackbarHost   = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "←",
                    fontSize = 20.sp,
                    color    = FlowlyMuted,
                    modifier = Modifier
                        .clickable { navController.popBackStack() }
                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )
                Text(
                    "Editar perfil",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyText
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Foto de perfil ───────────────────────────────────────────
            Box(
                modifier         = Modifier.align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.dp, FlowlyAccent, CircleShape)
                        .background(FlowlyCard2)
                        .clickable { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val photoUrl = localPhotoUri?.toString() ?: user?.profilePhotoUrl ?: ""
                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model              = photoUrl,
                            contentDescription = "Foto de perfil",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Text(
                            user?.iniciales ?: "?",
                            fontSize   = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color      = FlowlyAccent
                        )
                    }
                    if (isUploadingPhoto) {
                        Box(
                            modifier         = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color    = FlowlyAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Botón cámara
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(FlowlyAccent, CircleShape)
                        .clickable { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 14.sp)
                }
            }

            Text(
                "Tocá para cambiar foto",
                fontSize = 11.sp,
                color    = FlowlyMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp, bottom = 20.dp)
            )

            // ── Campos del perfil ────────────────────────────────────────
            FlowlyInput(nombre,   { nombre = it },   "Nombre completo *", "Ej: Martín González")
            FlowlyInput(telefono, { telefono = it }, "Teléfono",          "+54 9 11 1234-5678")

            // ── Selector de Provincia ────────────────────────────────────
            LocationDropdown(
                selectedValue = provincia,
                onValueChange = { provincia = it },
                label         = "Provincia",
                options       = PROVINCIAS_ARGENTINA,
                placeholder   = "Seleccioná tu provincia"
            )

            // ── Selector de Ciudad ───────────────────────────────────────
            LocationDropdown(
                selectedValue = ciudad,
                onValueChange = { ciudad = it },
                label         = "Ciudad",
                options       = ciudadesDisponibles,
                enabled       = provincia.isNotBlank(),
                placeholder   = if (provincia.isBlank()) "Primero elegí la provincia" else "Seleccioná tu ciudad"
            )

            FlowlyInput(aliasMP, { aliasMP = it }, "Alias de cobro", "martin.gonzalez.mp")

            Spacer(Modifier.height(24.dp))

            FlowlyPrimaryButton(
                text    = "Guardar cambios",
                enabled = uiState !is EditProfileUiState.Loading && !isUploadingPhoto,
                onClick = { vm.saveProfile(nombre, telefono, provincia, ciudad, aliasMP) }
            )

            Spacer(Modifier.height(8.dp))

            FlowlySecondaryButton(
                text    = "Cancelar",
                onClick = { navController.popBackStack() }
            )

            Spacer(Modifier.height(32.dp))
        }

        if (uiState is EditProfileUiState.Loading) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(FlowlyBg.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = FlowlyAccent) }
        }
    }
}

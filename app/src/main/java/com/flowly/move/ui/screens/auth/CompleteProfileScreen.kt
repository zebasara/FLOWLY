package com.flowly.move.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

/**
 * Shown after Google Sign-In for new users OR after reinstall when local DataStore
 * is cleared but the Firestore profile already exists.
 *
 * Pre-populates all fields with existing Firestore data so returning users
 * only need to confirm/adjust without losing anything.
 */
@Composable
fun CompleteProfileScreen(uid: String, navController: NavController) {
    val context   = LocalContext.current
    val viewModel = viewModel<AuthViewModel>()
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val existingUser by viewModel.existingUser.collectAsStateWithLifecycle()

    var nombre       by remember { mutableStateOf("") }
    var telefono     by remember { mutableStateOf("") }
    var provincia    by remember { mutableStateOf("") }
    var ciudad       by remember { mutableStateOf("") }
    var aliasMP      by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }

    // Cargar perfil existente desde Firestore al iniciar
    LaunchedEffect(uid) {
        viewModel.loadExistingProfile(uid)
    }

    // Pre-llenar campos cuando llega el usuario de Firestore (reinstalación)
    LaunchedEffect(existingUser) {
        existingUser?.let { u ->
            if (nombre.isBlank()    && u.nombre.isNotBlank())           nombre    = u.nombre
            if (telefono.isBlank()  && u.telefono.isNotBlank())         telefono  = u.telefono
            if (provincia.isBlank() && u.provincia.isNotBlank())        provincia = u.provincia
            if (ciudad.isBlank()    && u.ciudad.isNotBlank())           ciudad    = u.ciudad
            if (aliasMP.isBlank()   && u.aliasMercadoPago.isNotBlank()) aliasMP   = u.aliasMercadoPago
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success && !(uiState as AuthUiState.Success).isNewUser) {
            navController.navigate(Routes.HOME) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Determinar si es reinstalación (ya tiene datos) o usuario nuevo
    val isReinstall = existingUser != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowlyBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            // Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(FlowlyCard2, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isReinstall) "🔄" else "✏️", fontSize = 28.sp)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                if (isReinstall) "Confirmá tus datos" else "Completar perfil",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = FlowlyText
            )
            Text(
                if (isReinstall)
                    "Detectamos que ya tenés una cuenta. Confirmá o actualizá tus datos. Tus MOVE y nivel están guardados."
                else
                    "Solo tarda un minuto. Necesitamos estos datos para tu cuenta.",
                fontSize = 13.sp,
                color = FlowlyMuted,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            if (isReinstall) {
                FlowlyCard2(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        "✅ Tu saldo de MOVE, nivel y todo tu historial se mantienen intactos.",
                        fontSize = 12.sp,
                        color = FlowlySuccess,
                        lineHeight = 18.sp
                    )
                }
            }

            FlowlyInput(nombre,    { nombre = it },    "Nombre completo *",             "Ej: Martín González")
            FlowlyInput(telefono,  { telefono = it },  "Teléfono *",                    "+54 9 11 1234-5678")
            FlowlyInput(provincia, { provincia = it }, "Provincia *",                   "Buenos Aires")
            FlowlyInput(ciudad,    { ciudad = it },    "Ciudad *",                      "Tandil")
            FlowlyInput(aliasMP,   { aliasMP = it },   "Alias Mercado Pago (opcional)", "martin.gonzalez.mp")

            // Código de referido solo para usuarios nuevos
            if (!isReinstall) {
                FlowlyInput(referralCode, { referralCode = it }, "Código de referido (opcional)", "Ej: abc12345")
            }

            FlowlySeparator()

            FlowlyCard2 {
                Text(
                    "💡 El alias de Mercado Pago es necesario para recibir tus canjes. Podés agregarlo ahora o más tarde desde tu perfil.",
                    fontSize = 12.sp,
                    color = FlowlyMuted,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            FlowlyPrimaryButton(
                text    = if (isReinstall) "Confirmar y continuar" else "Guardar y continuar",
                onClick = {
                    viewModel.saveProfile(
                        uid          = uid,
                        nombre       = nombre,
                        telefono     = telefono,
                        provincia    = provincia,
                        ciudad       = ciudad,
                        alias        = aliasMP,
                        referralCode = referralCode
                    )
                },
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(Modifier.height(32.dp))
        }

        if (uiState is AuthUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FlowlyBg.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FlowlyAccent)
            }
        }

        if (uiState is AuthUiState.Error) {
            val msg = (uiState as AuthUiState.Error).message
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = FlowlyCard2,
                contentColor   = FlowlyDanger,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = FlowlyAccent, fontSize = 12.sp)
                    }
                }
            ) { Text(msg, fontSize = 13.sp) }
        }
    }
}

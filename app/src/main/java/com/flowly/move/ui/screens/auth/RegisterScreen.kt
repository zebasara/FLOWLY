package com.flowly.move.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import android.app.Activity

@Composable
fun RegisterScreen(navController: NavController) {
    val context   = LocalContext.current
    val viewModel = viewModel<AuthViewModel>()
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()

    var nombre    by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var telefono  by remember { mutableStateOf("") }
    var provincia by remember { mutableStateOf("") }
    var ciudad    by remember { mutableStateOf("") }

    // Flujo de registro en dos pasos:
    // 1. register() → Success(isNewUser=true)  → guarda perfil automáticamente
    // 2. saveProfile() → Success(isNewUser=false) → navega a HOME
    // Para Google: signInWithGoogle() ya retorna isNewUser según Firebase
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.Success -> {
                if (s.isNewUser) {
                    // Paso 1: guardar perfil con los datos ya ingresados
                    viewModel.saveProfile(
                        uid       = s.uid,
                        nombre    = nombre,
                        telefono  = telefono,
                        provincia = provincia,
                        ciudad    = ciudad
                    )
                } else {
                    // Paso 2: perfil guardado (o usuario existente) → ir a HOME
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            }
            else -> Unit
        }
    }

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
            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "←",
                    fontSize = 20.sp,
                    color = FlowlyMuted,
                    modifier = Modifier
                        .clickable { navController.popBackStack() }
                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )
                Column {
                    Text("Crear cuenta", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
                    Text("Empezá a ganar MOVE hoy", fontSize = 12.sp, color = FlowlyMuted)
                }
            }

            Spacer(Modifier.height(24.dp))

            FlowlyInput(nombre, { nombre = it }, "Nombre completo", "Ej: Martín González")
            FlowlyInput(email, { email = it }, "Email", "tumail@gmail.com")
            FlowlyInput(password, { password = it }, "Contraseña", "Mínimo 8 caracteres", isPassword = true)
            FlowlyInput(telefono, { telefono = it }, "Teléfono", "+54 9 11 1234-5678")
            FlowlyInput(provincia, { provincia = it }, "Provincia", "Buenos Aires")
            FlowlyInput(ciudad, { ciudad = it }, "Ciudad", "Tandil")

            FlowlySeparator()

            Text(
                "Al registrarte aceptás los Términos. El alias de Mercado Pago lo cargás después.",
                fontSize = 12.sp,
                color = FlowlyMuted,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FlowlyPrimaryButton(
                text = "Crear mi cuenta",
                onClick = { viewModel.register(email, password) },
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = FlowlyBorder)
                Text("o", fontSize = 12.sp, color = FlowlyMuted)
                HorizontalDivider(modifier = Modifier.weight(1f), color = FlowlyBorder)
            }

            Spacer(Modifier.height(16.dp))

            FlowlySecondaryButton(
                text = "Registrarme con Google",
                onClick = { viewModel.signInWithGoogle(context as Activity) }
            )

            Spacer(Modifier.height(32.dp))
        }

        // Loading
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

        // Error
        if (uiState is AuthUiState.Error) {
            val msg = (uiState as AuthUiState.Error).message
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = FlowlyCard2,
                contentColor = FlowlyDanger,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = FlowlyAccent, fontSize = 12.sp)
                    }
                }
            ) { Text(msg, fontSize = 13.sp) }
        }
    }
}

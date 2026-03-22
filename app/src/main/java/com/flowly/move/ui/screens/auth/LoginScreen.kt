package com.flowly.move.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

@Composable
fun LoginScreen(navController: NavController) {
    val context    = LocalContext.current
    val viewModel  = viewModel<AuthViewModel>()
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val resetState by viewModel.resetState.collectAsStateWithLifecycle()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ── Dialog recuperar contraseña ───────────────────────────────
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail      by remember { mutableStateOf("") }
    var resetFeedback   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resetState) {
        when {
            resetState == "ok" -> {
                resetFeedback = "✅ Revisá tu email para restablecer la contraseña"
                viewModel.clearResetState()
            }
            resetState?.startsWith("error:") == true -> {
                resetFeedback = resetState?.removePrefix("error:")
                viewModel.clearResetState()
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false; resetEmail = ""; resetFeedback = null },
            containerColor   = com.flowly.move.ui.theme.FlowlyCard,
            title = {
                Text(
                    "Recuperar contraseña",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = com.flowly.move.ui.theme.FlowlyText
                )
            },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ingresá tu email y te enviamos un link para restablecer tu contraseña.",
                        fontSize = 13.sp,
                        color    = com.flowly.move.ui.theme.FlowlyMuted
                    )
                    OutlinedTextField(
                        value         = resetEmail,
                        onValueChange = { resetEmail = it; resetFeedback = null },
                        label         = { Text("Email", fontSize = 13.sp) },
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = com.flowly.move.ui.theme.FlowlyAccent,
                            unfocusedBorderColor = com.flowly.move.ui.theme.FlowlyBorder,
                            focusedLabelColor    = com.flowly.move.ui.theme.FlowlyAccent,
                            unfocusedLabelColor  = com.flowly.move.ui.theme.FlowlyMuted,
                            cursorColor          = com.flowly.move.ui.theme.FlowlyAccent,
                            focusedTextColor     = com.flowly.move.ui.theme.FlowlyText,
                            unfocusedTextColor   = com.flowly.move.ui.theme.FlowlyText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!resetFeedback.isNullOrBlank()) {
                        Text(
                            resetFeedback!!,
                            fontSize = 12.sp,
                            color    = if (resetFeedback!!.startsWith("✅"))
                                com.flowly.move.ui.theme.FlowlySuccess
                            else
                                com.flowly.move.ui.theme.FlowlyDanger
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.sendPasswordReset(resetEmail) }) {
                    Text("Enviar", color = com.flowly.move.ui.theme.FlowlyAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false; resetEmail = ""; resetFeedback = null }) {
                    Text("Cancelar", color = com.flowly.move.ui.theme.FlowlyMuted)
                }
            }
        )
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.Success -> {
                if (s.isNewUser) {
                    navController.navigate(Routes.completeProfile(s.uid)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            Text(
                "Flowly",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = FlowlyAccent,
                letterSpacing = (-1).sp
            )
            Text(
                "bienvenido de vuelta",
                fontSize = 13.sp,
                color = FlowlyMuted,
                modifier = Modifier.padding(bottom = 36.dp)
            )

            FlowlyInput(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "tumail@gmail.com"
            )

            FlowlyInput(
                value = password,
                onValueChange = { password = it },
                label = "Contraseña",
                placeholder = "Tu contraseña",
                isPassword = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Olvidé mi contraseña",
                    fontSize = 12.sp,
                    color = FlowlyAccent2,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = 16.dp)
                        .clickable { showResetDialog = true; resetEmail = email }
                )
            }

            FlowlyPrimaryButton(
                text = "Ingresar",
                onClick = { viewModel.login(email, password) },
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = FlowlyBorder)
                Text("  o  ", fontSize = 12.sp, color = FlowlyMuted)
                HorizontalDivider(modifier = Modifier.weight(1f), color = FlowlyBorder)
            }

            Spacer(Modifier.height(16.dp))

            FlowlySecondaryButton(
                text = "Continuar con Google",
                onClick = { viewModel.signInWithGoogle(context as Activity) }
            )

            Spacer(Modifier.height(24.dp))

            Row {
                Text("¿No tenés cuenta? ", fontSize = 13.sp, color = FlowlyMuted)
                Text(
                    "Registrarte",
                    fontSize = 13.sp,
                    color = FlowlyAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Routes.REGISTER) }
                )
            }

            Spacer(Modifier.height(40.dp))
        }

        // Loading overlay
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

        // Error snackbar
        if (uiState is AuthUiState.Error) {
            val msg = (uiState as AuthUiState.Error).message
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = FlowlyCard2,
                contentColor = FlowlyDanger,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = FlowlyAccent, fontSize = 12.sp)
                    }
                }
            ) {
                Text(msg, fontSize = 13.sp)
            }
        }
    }
}

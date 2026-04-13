package com.flowly.move.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
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

// Glow radial para el fondo de auth — firma visual de Flowly
private val AuthTopGlow = object : ShaderBrush() {
    override fun createShader(size: androidx.compose.ui.geometry.Size) =
        RadialGradientShader(
            colors  = listOf(Color(0x2A7EE621), Color.Transparent),
            center  = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
            radius  = size.width * 0.75f
        )
}

@Composable
fun LoginScreen(navController: NavController) {
    val context    = LocalContext.current
    val viewModel  = viewModel<AuthViewModel>()
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val resetState by viewModel.resetState.collectAsStateWithLifecycle()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            containerColor   = FlowlyCard,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Recuperar contraseña",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ingresá tu email y te enviamos un link para restablecer tu contraseña.",
                        fontSize   = 13.sp,
                        color      = FlowlyMuted,
                        lineHeight = 20.sp
                    )
                    OutlinedTextField(
                        value         = resetEmail,
                        onValueChange = { resetEmail = it; resetFeedback = null },
                        label         = { Text("Email", fontSize = 13.sp) },
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = FlowlyAccent,
                            unfocusedBorderColor    = FlowlyBorder,
                            focusedLabelColor       = FlowlyAccent,
                            unfocusedLabelColor     = FlowlyMuted,
                            cursorColor             = FlowlyAccent,
                            focusedTextColor        = FlowlyText,
                            unfocusedTextColor      = FlowlyText,
                            focusedContainerColor   = FlowlyCard2,
                            unfocusedContainerColor = FlowlyCard2
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!resetFeedback.isNullOrBlank()) {
                        Text(
                            resetFeedback!!,
                            fontSize = 12.sp,
                            color    = if (resetFeedback!!.startsWith("✅")) FlowlySuccess
                                       else FlowlyDanger
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.sendPasswordReset(resetEmail) }) {
                    Text("Enviar", color = FlowlyAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false; resetEmail = ""; resetFeedback = null }) {
                    Text("Cancelar", color = FlowlyMuted)
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
        // ── Glow atmosférico superior — firma visual Flowly ────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(AuthTopGlow)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // ── Logo ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(listOf(FlowlyAccent, FlowlyAccentDark)),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 30.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Flowly",
                fontSize      = 36.sp,
                fontWeight    = FontWeight.Bold,
                color         = FlowlyAccent,
                letterSpacing = (-1.5).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Bienvenido de vuelta",
                fontSize   = 14.sp,
                color      = FlowlyMuted,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // ── Formulario ────────────────────────────────────────────────
            FlowlyInput(
                value         = email,
                onValueChange = { email = it },
                label         = "EMAIL",
                placeholder   = "tumail@gmail.com"
            )

            FlowlyInput(
                value         = password,
                onValueChange = { password = it },
                label         = "CONTRASEÑA",
                placeholder   = "Tu contraseña",
                isPassword    = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Olvidé mi contraseña",
                    fontSize = 12.sp,
                    color    = FlowlyAccent2,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showResetDialog = true; resetEmail = email }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            FlowlyPrimaryButton(
                text    = "Ingresar",
                onClick = { viewModel.login(email, password) },
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(Modifier.height(24.dp))

            // ── Separador ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier  = Modifier.weight(1f),
                    color     = FlowlyBorder
                )
                Text(
                    "  ó  ",
                    fontSize = 12.sp,
                    color    = FlowlyMuted
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color    = FlowlyBorder
                )
            }

            Spacer(Modifier.height(20.dp))

            FlowlySecondaryButton(
                text    = "Continuar con Google",
                onClick = { viewModel.signInWithGoogle(context as Activity) }
            )

            Spacer(Modifier.height(32.dp))

            Row {
                Text("¿No tenés cuenta? ", fontSize = 14.sp, color = FlowlyMuted)
                Text(
                    "Registrarte",
                    fontSize   = 14.sp,
                    color      = FlowlyAccent,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable { navController.navigate(Routes.REGISTER) }
                )
            }

            Spacer(Modifier.height(48.dp))
        }

        // ── Loading overlay ────────────────────────────────────────────────
        if (uiState is AuthUiState.Loading) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(FlowlyBg.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color         = FlowlyAccent,
                    strokeWidth   = 2.5.dp
                )
            }
        }

        // ── Error snackbar ─────────────────────────────────────────────────
        if (uiState is AuthUiState.Error) {
            val msg = (uiState as AuthUiState.Error).message
            Snackbar(
                modifier       = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = FlowlyCard2,
                contentColor   = FlowlyDanger,
                shape          = RoundedCornerShape(14.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = FlowlyAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Text(msg, fontSize = 13.sp)
            }
        }
    }
}

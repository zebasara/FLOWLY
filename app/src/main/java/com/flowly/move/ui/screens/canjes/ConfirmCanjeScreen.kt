package com.flowly.move.ui.screens.canjes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

@Composable
fun ConfirmCanjeScreen(amount: String, move: String, navController: NavController) {
    val vm: CanjesViewModel = viewModel()
    val user     by vm.user.collectAsStateWithLifecycle()
    val uiState  by vm.uiState.collectAsStateWithLifecycle()

    val moveInt       = move.toIntOrNull() ?: 0
    val tokensActuales = user?.tokensActuales ?: 0
    val saldoTras     = tokensActuales - moveInt
    val aliasMP       = user?.aliasMercadoPago ?: ""

    val snackbar = remember { SnackbarHostState() }

    // Navegación cuando el canje se confirma con éxito
    LaunchedEffect(uiState) {
        when (uiState) {
            is CanjesUiState.Success -> {
                vm.clearState()
                navController.navigate(Routes.MY_CANJES) {
                    popUpTo(Routes.CANJES) { inclusive = false }
                }
            }
            is CanjesUiState.Error -> {
                snackbar.showSnackbar((uiState as CanjesUiState.Error).msg)
                vm.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = FlowlyBg,
        snackbarHost   = { SnackbarHost(snackbar) }
    ) { padding ->
        if (uiState is CanjesUiState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlowlyAccent)
            }
            return@Scaffold
        }

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
                    color = FlowlyMuted,
                    modifier = Modifier
                        .clickable { navController.popBackStack() }
                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )
                Text("Confirmar canje", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FlowlyCard2, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("vas a recibir", fontSize = 12.sp, color = FlowlyMuted)
                    Text(amount, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = FlowlySuccess)
                    Text("en tu Mercado Pago", fontSize = 12.sp, color = FlowlyMuted, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            FlowlyCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MOVE a descontar", fontSize = 12.sp, color = FlowlyMuted)
                    Text("- %,d MOVE".format(moveInt), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyDanger)
                }
                FlowlySeparator()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo actual", fontSize = 12.sp, color = FlowlyMuted)
                    Text("%,d MOVE".format(tokensActuales), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyText)
                }
                FlowlySeparator()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo tras el canje", fontSize = 12.sp, color = FlowlyMuted)
                    Text("%,d MOVE".format(saldoTras), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyAccent2)
                }
            }

            Spacer(Modifier.height(10.dp))

            FlowlyCard {
                Text("Destino del pago", fontSize = 12.sp, color = FlowlyMuted)
                if (aliasMP.isBlank()) {
                    Text(
                        "⚠️ No tenés alias MP configurado. Andá a Perfil → Editar.",
                        fontSize = 13.sp, color = FlowlyWarn,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(aliasMP, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyText, modifier = Modifier.padding(top = 4.dp))
                    Text("Mercado Pago · alias registrado", fontSize = 12.sp, color = FlowlyMuted, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            FlowlyCard2 {
                Text(
                    "⚠️ Tokens descontados inmediatamente. El pago llega en menos de 48hs hábiles.",
                    fontSize = 12.sp, color = FlowlyWarn, lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            FlowlyPrimaryButton(
                text    = "Confirmar canje",
                enabled = aliasMP.isNotBlank(),
                onClick = { vm.confirmarCanje(amount, moveInt) }
            )

            Spacer(Modifier.height(8.dp))

            FlowlySecondaryButton(text = "Cancelar", onClick = { navController.popBackStack() })

            Spacer(Modifier.height(24.dp))
        }
    }
}

package com.flowly.move.ui.screens.holding

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
import java.util.Calendar

@Composable
fun ConfirmHoldingScreen(move: Int, months: Int, navController: NavController) {
    val vm: HoldingViewModel = viewModel()
    val uiState   by vm.uiState.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    val tasa          = when (months) { 3 -> 8; 6 -> 12; else -> 18 }
    val interes       = (move * tasa / 100)
    val totalAlVencer = move + interes

    val calendar   = Calendar.getInstance()
    val monthNames = listOf("ene","feb","mar","abr","may","jun","jul","ago","sep","oct","nov","dic")
    val fechaInicio = "${calendar.get(Calendar.DAY_OF_MONTH)} ${monthNames[calendar.get(Calendar.MONTH)]} ${calendar.get(Calendar.YEAR)}"
    calendar.add(Calendar.MONTH, months)
    val fechaVenc = "${calendar.get(Calendar.DAY_OF_MONTH)} ${monthNames[calendar.get(Calendar.MONTH)]} ${calendar.get(Calendar.YEAR)}"

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is HoldingUiState.Success -> {
                vm.clearState()
                navController.navigate(Routes.HOLDING) {
                    popUpTo(Routes.HOLDING) { inclusive = true }
                }
            }
            is HoldingUiState.Error -> {
                snackbar.showSnackbar((uiState as HoldingUiState.Error).msg)
                vm.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = FlowlyBg,
        snackbarHost   = { SnackbarHost(snackbar) }
    ) { padding ->
        if (uiState is HoldingUiState.Loading) {
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
                Text("Confirmar holding", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
            }

            Spacer(Modifier.height(20.dp))

            // Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FlowlyCard2, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("vas a bloquear", fontSize = 12.sp, color = FlowlyMuted)
                    Text(
                        "%,d MOVE".format(move),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlowlyAccent2
                    )
                    Text(
                        "por $months meses · interés $tasa%",
                        fontSize = 12.sp,
                        color = FlowlyMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            FlowlyCard {
                listOf(
                    Triple("MOVE a bloquear",       "%,d MOVE".format(move),          FlowlyAccent2),
                    Triple("Interés ($tasa%)",        "+ %,d MOVE".format(interes),     FlowlyAccent),
                    Triple("Total al vencer",         "%,d MOVE".format(totalAlVencer), FlowlyAccent),
                    Triple("Fecha de inicio",         fechaInicio,                      FlowlyText),
                    Triple("Fecha de vencimiento",    fechaVenc,                        FlowlyAccent2)
                ).forEachIndexed { i, (label, value, color) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, fontSize = 12.sp, color = FlowlyMuted)
                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
                    }
                    if (i < 4) FlowlySeparator()
                }
            }

            Spacer(Modifier.height(10.dp))

            FlowlyCard2 {
                Text(
                    "⚠️ MOVE bloqueados hasta el vencimiento. Si cancelás antes perdés el interés pero recuperás los %,d MOVE.".format(move),
                    fontSize = 12.sp,
                    color = FlowlyWarn,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            FlowlyPrimaryButton(
                text    = "Confirmar y bloquear",
                enabled = !isLoading,
                onClick = { vm.confirmarHolding(move, months) }
            )

            Spacer(Modifier.height(8.dp))

            FlowlySecondaryButton(text = "Cancelar", onClick = { navController.popBackStack() })

            Spacer(Modifier.height(24.dp))
        }
    }
}

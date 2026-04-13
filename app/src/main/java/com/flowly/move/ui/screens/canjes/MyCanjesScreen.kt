package com.flowly.move.ui.screens.canjes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyCanjesScreen(navController: NavController) {
    val vm: CanjesViewModel   = viewModel()
    val canjes    by vm.canjes.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    val sdf = remember { SimpleDateFormat("d MMM yyyy", Locale("es", "AR")) }

    FlowlyScaffold(navController = navController, currentRoute = Routes.CANJES) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mis recompensas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FlowlyAccent)
                }
            } else if (canjes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Todavía no solicitaste ninguna recompensa.",
                        fontSize = 14.sp,
                        color = FlowlyMuted
                    )
                }
            } else {
                canjes.forEach { canje ->
                    val fecha = sdf.format(Date(canje.createdAt))
                    val statusLabel = when (canje.estado) {
                        "completado" -> "Completado"
                        "enviado"    -> "Enviado"
                        "cancelado"  -> "Cancelado"
                        else         -> "Pendiente"
                    }
                    val detail = when (canje.estado) {
                        "completado" -> "Recompensa acreditada en tu cuenta"
                        "cancelado"  -> "MOVE devueltos a tu cuenta"
                        "enviado"    -> "En camino · revisá el estado con el soporte"
                        else         -> "En proceso · disponible dentro del período de validación"
                    }

                    FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${canje.montoLabel} · ${canje.aliasDestino}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FlowlyText
                                )
                                Text(
                                    "$fecha · %,d MOVE".format(canje.moveDescontado),
                                    fontSize = 12.sp,
                                    color = FlowlyMuted,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            when (canje.estado) {
                                "completado" -> TagGreen(statusLabel)
                                "pendiente"  -> TagAmber(statusLabel)
                                "enviado"    -> TagBlue(statusLabel)
                                "cancelado"  -> TagRed(statusLabel)
                                else         -> TagAmber(statusLabel)
                            }
                        }
                        Text(
                            detail,
                            fontSize = 12.sp,
                            color = FlowlyMuted,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

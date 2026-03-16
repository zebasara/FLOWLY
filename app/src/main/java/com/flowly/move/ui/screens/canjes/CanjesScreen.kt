package com.flowly.move.ui.screens.canjes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

private data class CanjeOption(
    val label: String,
    val move: Int,
    val available: Boolean,
    val missing: Int = 0
)

@Composable
fun CanjesScreen(navController: NavController) {
    val vm: CanjesViewModel = viewModel()
    val user           by vm.user.collectAsStateWithLifecycle()
    val isLoading      by vm.isLoading.collectAsStateWithLifecycle()
    val mercadoPagoUrl by vm.mercadoPagoUrl.collectAsStateWithLifecycle()
    val uriHandler      = LocalUriHandler.current

    val tokensLibres = user?.tokensActuales ?: 0
    val aliasMP      = user?.aliasMercadoPago ?: ""

    val options = listOf(
        CanjeOption("\$2.000 ARS",  33_600,  tokensLibres >= 33_600,  maxOf(0, 33_600  - tokensLibres)),
        CanjeOption("\$5.000 ARS",  84_000,  tokensLibres >= 84_000,  maxOf(0, 84_000  - tokensLibres)),
        CanjeOption("\$10.000 ARS", 168_000, tokensLibres >= 168_000, maxOf(0, 168_000 - tokensLibres)),
        CanjeOption("\$20.000 ARS", 336_000, tokensLibres >= 336_000, maxOf(0, 336_000 - tokensLibres))
    )

    FlowlyScaffold(navController = navController, currentRoute = Routes.CANJES) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlowlyAccent)
            }
            return@FlowlyScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Canjes", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
                    Text("saldo libre: %,d MOVE".format(tokensLibres), fontSize = 12.sp, color = FlowlyMuted)
                }
            }

            // Alias MP
            FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alias Mercado Pago", fontSize = 12.sp, color = FlowlyMuted)
                    Text(aliasMP, fontSize = 12.sp, color = FlowlyAccent)
                }
            }

            SectionTitle(modifier = Modifier.padding(horizontal = 16.dp), text = "transferencia a mercado pago")

            options.forEach { opt ->
                CanjeItem(
                    label     = opt.label,
                    move      = opt.move,
                    available = opt.available,
                    missing   = opt.missing,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (opt.available) {
                        navController.navigate(Routes.confirmCanje(opt.label, opt.move.toString()))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(8.dp))

            FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Un canje por mes · procesado en menos de 48hs hábiles",
                    fontSize = 12.sp,
                    color = FlowlyMuted,
                    lineHeight = 18.sp
                )
            }

            // Botón Mercado Pago (solo si hay URL configurada desde el admin)
            if (mercadoPagoUrl.isNotBlank()) {
                SectionTitle(modifier = Modifier.padding(horizontal = 16.dp), text = "mercado pago")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(FlowlyCard2, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { uriHandler.openUri(mercadoPagoUrl) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("💳", fontSize = 26.sp)
                        Column {
                            Text(
                                "Descargar Mercado Pago",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = FlowlyText
                            )
                            Text(
                                "Necesario para recibir tus canjes",
                                fontSize = 11.sp,
                                color    = FlowlyMuted
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(FlowlyAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Descargar", fontSize = 11.sp, color = FlowlyAccent, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            SectionTitle(modifier = Modifier.padding(horizontal = 16.dp), text = "más opciones")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(FlowlyCard2, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Routes.MY_CANJES) }
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("📋", fontSize = 22.sp)
                    Text("Mis canjes", fontSize = 12.sp, color = FlowlyMuted)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(FlowlyCard2, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Routes.HOLDING) }
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🔒", fontSize = 22.sp)
                    Text("Holding", fontSize = 12.sp, color = FlowlyMuted)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CanjeItem(
    label: String,
    move: Int,
    available: Boolean,
    missing: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (available) FlowlyCard2 else FlowlyCard2.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (available) FlowlyAccent else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = available, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (available) FlowlyText else FlowlyMuted
            )
            Text(
                if (available) "%,d MOVE · disponible".format(move)
                else "%,d MOVE · te faltan %,d".format(move, missing),
                fontSize = 12.sp,
                color = FlowlyMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (available) {
            Box(
                modifier = Modifier
                    .background(FlowlyAccent, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Canjear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FlowlyBg)
            }
        } else {
            Box(
                modifier = Modifier
                    .background(FlowlyCard, RoundedCornerShape(10.dp))
                    .border(1.dp, FlowlyBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Bloqueado", fontSize = 12.sp, color = FlowlyMuted)
            }
        }
    }
}

@Composable
private fun SectionTitle(modifier: Modifier = Modifier, text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF4B6B4B),
        letterSpacing = 1.sp,
        modifier = modifier.padding(top = 14.dp, bottom = 8.dp)
    )
}

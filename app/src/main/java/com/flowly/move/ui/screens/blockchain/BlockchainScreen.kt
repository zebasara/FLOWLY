package com.flowly.move.ui.screens.blockchain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

// BlockchainScreen shows locked state (admin can enable it remotely)
// When activated = true, shows withdrawal form
@Composable
fun BlockchainScreen(navController: NavController) {
    val activated    = false   // TODO: read from Firebase RemoteConfig
    val tokensLibres = 42_800
    val usuariosActivos  = 1_247
    val metaUsuarios     = 5_000
    val progreso = usuariosActivos.toFloat() / metaUsuarios

    FlowlyScaffold(navController = navController, currentRoute = Routes.CANJES) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (activated) "Retiro BNB Chain" else "Retiro blockchain",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowlyText
                )
                if (activated) TagGreen("Activo")
            }

            if (!activated) {
                // Locked state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                androidx.compose.ui.graphics.Color(0xFF1A1A3A),
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("🔒", fontSize = 36.sp) }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Próximamente en blockchain",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlowlyText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Llevamos el token MOVE a BNB Chain. Cuando lleguemos a nuestra meta podés retirar tus MOVE a tu wallet y tradearlos.",
                        fontSize = 13.sp,
                        color = FlowlyMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                // Progress
                FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("progreso hacia el lanzamiento", fontSize = 12.sp, color = FlowlyMuted)
                        Text(
                            "%,d / %,d".format(usuariosActivos, metaUsuarios),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlowlyAccent2,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text("usuarios activos", fontSize = 12.sp, color = FlowlyMuted)
                        Spacer(Modifier.height(12.dp))
                        FlowlyProgressBar(progress = progreso)
                        Spacer(Modifier.height(4.dp))
                        Text("%.0f%% del objetivo".format(progreso * 100), fontSize = 12.sp, color = FlowlyMuted)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Features preview
                FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "CUANDO SE ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color(0xFF4B6B4B),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    FeatureRow("📤", "Retirar MOVE a tu wallet", "MetaMask, Trust Wallet")
                    FlowlySeparator()
                    FeatureRow("📈", "Tradear en PancakeSwap", "Par MOVE/BNB · BNB Chain")
                    FlowlySeparator()
                    FeatureRow("💸", "Fee mínimo", "Gas BNB Smart Chain ~USD 0,10")
                }
            } else {
                // Activated state — withdrawal form
                FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Red", fontSize = 12.sp, color = FlowlyMuted)
                        Text("BNB Smart Chain (BSC)", fontSize = 12.sp, color = FlowlyAccent2)
                    }
                    FlowlySeparator()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fee estimado", fontSize = 12.sp, color = FlowlyMuted)
                        Text("~0,001 BNB (~USD 0,30)", fontSize = 12.sp, color = FlowlyWarn)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Amount + wallet fields (simplified)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("CANTIDAD A RETIRAR", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFF4B6B4B), letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                    FlowlyInput("", {}, "Mínimo 1.000 MOVE", "0")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Disponible: %,d MOVE".format(tokensLibres), fontSize = 12.sp, color = FlowlyMuted)
                        Text("Máximo", fontSize = 12.sp, color = FlowlyAccent)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("WALLET DESTINO (BNB CHAIN)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFF4B6B4B), letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                    FlowlyInput("", {}, "Dirección 0x…", "0x…")
                    Spacer(Modifier.height(12.dp))
                    FlowlyPrimaryButton(text = "Retirar a mi wallet", onClick = { /* TODO */ })
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyText)
            Text(subtitle, fontSize = 12.sp, color = FlowlyMuted)
        }
    }
}

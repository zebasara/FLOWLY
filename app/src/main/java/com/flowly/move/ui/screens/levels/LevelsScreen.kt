package com.flowly.move.ui.screens.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.data.model.NIVEL_LIMITES
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.screens.home.UserViewModel
import com.flowly.move.ui.theme.*

private data class NivelData(
    val nivel: Int,
    val limiteMove: Int,
    val estado: String    // "done" | "curr" | "pending"
)

@Composable
fun LevelsScreen(navController: NavController) {
    val vm: UserViewModel = viewModel()
    val user      by vm.user.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    val nivelActual    = user?.nivel ?: 1
    val tokensActuales = user?.tokensActuales ?: 0
    // NIVEL_LIMITES es la fuente de verdad — ignora el valor viejo de Firestore
    val limiteActual   = NIVEL_LIMITES.getOrElse(nivelActual - 1) { NIVEL_LIMITES.first() }
    val saldoMinimo    = limiteActual * 3 / 4
    val videosConsec   = (user?.diasConsecutivosVideos ?: 0).coerceAtMost(5)
    val nivelSiguiente = nivelActual + 1
    val videosOk       = videosConsec >= 5
    val saldoOk        = tokensActuales >= saldoMinimo

    val niveles = NIVEL_LIMITES.mapIndexed { i, limite ->
        val n = i + 1
        NivelData(
            nivel      = n,
            limiteMove = limite,
            estado     = when {
                n < nivelActual  -> "done"
                n == nivelActual -> "curr"
                else             -> "pending"
            }
        )
    }

    FlowlyScaffold(navController = navController, currentRoute = Routes.PROFILE) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
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

            Text(
                "Sistema de niveles",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = FlowlyText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Tarjeta nivel actual
            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("tu nivel actual", fontSize = 12.sp, color = FlowlyMuted)
                    Text(
                        "Nivel $nivelActual",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlowlyAccent2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "límite: %,d MOVE".format(limiteActual),
                        fontSize = 12.sp,
                        color = FlowlyMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (nivelActual < 10) {
                Spacer(Modifier.height(12.dp))

                Text(
                    "PARA SUBIR AL NIVEL $nivelSiguiente NECESITÁS LAS DOS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B6B4B),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Condición 1: Videos consecutivos
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(FlowlyCard2, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF1C3A08), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("📺", fontSize = 14.sp) }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Videos consecutivos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = FlowlyText
                            )
                            Text(
                                "$videosConsec de 5 días completados",
                                fontSize = 12.sp,
                                color = FlowlyMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (videosOk) {
                            Text("✓ Cumplido", fontSize = 12.sp, color = FlowlySuccess, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("$videosConsec/5", fontSize = 13.sp, color = FlowlyMuted, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowlyProgressBar(progress = videosConsec / 5f, color = FlowlyAccent)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (videosOk) "Completado ✓"
                        else "Faltan ${5 - videosConsec} días más viendo 2 videos por día",
                        fontSize = 12.sp,
                        color = if (videosOk) FlowlySuccess else FlowlyMuted
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Condición 2: Saldo mínimo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(FlowlyCard2, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF2D1A00), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("💰", fontSize = 14.sp) }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Saldo mínimo requerido",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = FlowlyText
                            )
                            Text(
                                "%,d de %,d MOVE (75%%)".format(tokensActuales, saldoMinimo),
                                fontSize = 12.sp,
                                color = FlowlyMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (saldoOk) {
                            Text("✓ Cumplido", fontSize = 12.sp, color = FlowlySuccess, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowlyProgressBar(
                        progress = minOf(1f, if (saldoMinimo > 0) tokensActuales.toFloat() / saldoMinimo else 1f),
                        color = FlowlyAccent2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (saldoOk) "Tenés más del 75% del límite acumulado"
                        else "Te faltan %,d MOVE para el saldo mínimo".format(saldoMinimo - tokensActuales),
                        fontSize = 12.sp,
                        color = if (saldoOk) FlowlySuccess else FlowlyMuted
                    )
                }

                Spacer(Modifier.height(8.dp))

                FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        when {
                            videosOk && saldoOk ->
                                "✅ ¡Cumplís los dos requisitos! Subís al nivel $nivelSiguiente automáticamente."
                            !videosOk && !saldoOk ->
                                "⏳ Te faltan los videos consecutivos y el saldo mínimo para subir de nivel."
                            !videosOk ->
                                "⏳ Completá los 5 días de videos. Cuando ambas barras lleguen al 100% subís."
                            else ->
                                "💰 Te faltan %,d MOVE para el saldo mínimo.".format(saldoMinimo - tokensActuales)
                        },
                        fontSize = 12.sp,
                        color = if (videosOk && saldoOk) FlowlySuccess else FlowlyWarn,
                        lineHeight = 18.sp
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "🏆 ¡Alcanzaste el nivel máximo! Seguí acumulando MOVE.",
                        fontSize = 12.sp,
                        color = FlowlyAccent,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "TODOS LOS NIVELES",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4B6B4B),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            niveles.forEach { n ->
                NivelRow(nivel = n, isActual = n.nivel == nivelActual)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NivelRow(nivel: NivelData, isActual: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(
                if (isActual)
                    Modifier
                        .background(FlowlyAccent2.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                        .border(1.dp, FlowlyAccent2.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp)
                else
                    Modifier
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    when (nivel.estado) {
                        "done" -> Color(0xFF1C3A08)
                        "curr" -> Color(0xFF2D1A00)
                        else   -> FlowlyCard2
                    },
                    CircleShape
                )
                .border(
                    2.dp,
                    when (nivel.estado) {
                        "done" -> FlowlyAccent
                        "curr" -> FlowlyAccent2
                        else   -> FlowlyBorder
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (nivel.estado == "done") "✓" else "${nivel.nivel}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when (nivel.estado) {
                    "done" -> FlowlyAccent
                    "curr" -> FlowlyAccent2
                    else   -> FlowlyMuted
                }
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isActual) "Nivel ${nivel.nivel} · actual" else "Nivel ${nivel.nivel}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = when (nivel.estado) {
                    "done" -> FlowlyText
                    "curr" -> FlowlyAccent2
                    else   -> FlowlyMuted
                }
            )
            Text("%,d MOVE".format(nivel.limiteMove), fontSize = 12.sp, color = FlowlyMuted)
        }

        when (nivel.estado) {
            "done" -> TagGreen("Completado")
            "curr" -> TagAmber("Actual")
            else   -> if (nivel.nivel == 10) TagAmber("Meta final")
        }
    }
}

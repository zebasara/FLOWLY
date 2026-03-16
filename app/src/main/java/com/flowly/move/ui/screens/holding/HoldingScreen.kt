package com.flowly.move.ui.screens.holding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.data.model.Holding
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HoldingScreen(navController: NavController) {
    val vm: HoldingViewModel = viewModel()
    val user      by vm.user.collectAsStateWithLifecycle()
    val holdings  by vm.holdings.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    val saldoLibre = user?.tokensActuales ?: 0
    val enHolding  = user?.moveEnHolding  ?: 0

    val montos = listOf(5_000, 10_000, 25_000, 50_000)

    var selectedMonto by remember { mutableIntStateOf(0) }
    var selectedPlazo by remember { mutableIntStateOf(0) }

    // Interés dinámico según el monto seleccionado
    val plazos = listOf(
        Triple(3, 8,  "+%,d MOVE".format((montos[selectedMonto] * 0.08).toInt())),
        Triple(6, 12, "+%,d MOVE".format((montos[selectedMonto] * 0.12).toInt())),
        Triple(9, 18, "+%,d MOVE".format((montos[selectedMonto] * 0.18).toInt()))
    )

    val sdf = remember { SimpleDateFormat("d MMM yyyy", Locale("es", "AR")) }

    FlowlyScaffold(navController = navController, currentRoute = Routes.CANJES) { padding ->
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Holding", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
                    Text("bloqueá y ganá interés", fontSize = 12.sp, color = FlowlyMuted)
                }
            }

            // Resumen de balance
            FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo libre", fontSize = 12.sp, color = FlowlyMuted)
                    Text("%,d MOVE".format(saldoLibre), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyAccent)
                }
                FlowlySeparator()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("En holding", fontSize = 12.sp, color = FlowlyMuted)
                    Text("%,d MOVE bloqueados".format(enHolding), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyAccent2)
                }
            }

            // Selector de monto
            Text(
                "ELEGÍ CUÁNTO HOLDEAR",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4B6B4B),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                montos.take(2).forEachIndexed { i, monto ->
                    MontoChip(
                        monto    = monto,
                        selected = selectedMonto == i,
                        enabled  = saldoLibre >= monto,
                        modifier = Modifier.weight(1f)
                    ) { selectedMonto = i }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                montos.drop(2).forEachIndexed { i, monto ->
                    MontoChip(
                        monto    = monto,
                        selected = selectedMonto == i + 2,
                        enabled  = saldoLibre >= monto,
                        modifier = Modifier.weight(1f)
                    ) { selectedMonto = i + 2 }
                }
            }

            // Selector de plazo
            Text(
                "ELEGÍ EL PLAZO",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4B6B4B),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(FlowlyCard2, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
            ) {
                plazos.forEachIndexed { i, (meses, tasa, ganancia) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlazo = i }
                            .background(
                                if (selectedPlazo == i) FlowlyAccent.copy(alpha = 0.05f)
                                else Color.Transparent
                            )
                            .padding(12.dp, 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "$meses meses · $tasa%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = FlowlyText
                            )
                            Text(
                                "$ganancia de interés",
                                fontSize = 12.sp,
                                color = FlowlyMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        when (i) {
                            0 -> TagGreen("Popular")
                            1 -> TagAmber("Más ganancia")
                            2 -> TagPurple("Máximo")
                        }
                    }
                    if (i < plazos.size - 1) HorizontalDivider(color = FlowlyBorder)
                }
            }

            Spacer(Modifier.height(16.dp))

            FlowlyPrimaryButton(
                text    = "Confirmar holding",
                enabled = saldoLibre >= montos[selectedMonto],
                onClick = {
                    val move   = montos[selectedMonto]
                    val months = plazos[selectedPlazo].first
                    navController.navigate(Routes.confirmHolding(move, months))
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Holdings activos
            val activosHoldings = holdings.filter { it.estado == "activo" }
            if (activosHoldings.isNotEmpty()) {
                Text(
                    "MIS HOLDINGS ACTIVOS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B6B4B),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                activosHoldings.forEach { holding ->
                    HoldingCard(holding = holding, sdf = sdf)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HoldingCard(holding: Holding, sdf: SimpleDateFormat) {
    val now         = System.currentTimeMillis()
    val daysLeft    = ((holding.fechaFin - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)
    val progress    = if (holding.fechaFin > holding.fechaInicio) {
        ((now - holding.fechaInicio).toFloat() / (holding.fechaFin - holding.fechaInicio)).coerceIn(0f, 1f)
    } else 1f
    val fechaVenc   = sdf.format(Date(holding.fechaFin))
    val totalVencer = holding.moveAmount + holding.interesMove

    FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    "%,d MOVE · ${holding.meses} meses".format(holding.moveAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FlowlyText
                )
                Text(
                    "Vence el $fechaVenc",
                    fontSize = 12.sp,
                    color = FlowlyMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            TagAmber("$daysLeft días")
        }
        Spacer(Modifier.height(8.dp))
        FlowlyProgressBar(progress = progress, color = FlowlyAccent2)
        Spacer(Modifier.height(4.dp))
        Text(
            "Cobrarás %,d MOVE al vencer · +%,d MOVE".format(totalVencer, holding.interesMove),
            fontSize = 12.sp,
            color = FlowlyMuted
        )
    }
}

@Composable
private fun MontoChip(
    monto: Int,
    selected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(FlowlyCard2, RoundedCornerShape(12.dp))
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) FlowlyAccent else FlowlyBorder,
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "%,d".format(monto),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    !enabled -> FlowlyMuted
                    selected -> FlowlyAccent
                    else     -> FlowlyText
                }
            )
            Text("MOVE", fontSize = 11.sp, color = FlowlyMuted)
        }
    }
}

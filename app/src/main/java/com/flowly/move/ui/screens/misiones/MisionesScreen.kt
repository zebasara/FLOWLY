package com.flowly.move.ui.screens.misiones

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.ui.components.FlowlyProgressBar
import com.flowly.move.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisionesScreen(navController: NavController) {
    val vm: MisionesViewModel = viewModel()
    val user      by vm.user.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error     by vm.error.collectAsStateWithLifecycle()
    val success   by vm.success.collectAsStateWithLifecycle()

    val misiones  = user?.let { getMisionesDelDia(it) } ?: emptyList()
    val completadas = misiones.count { it.completada }
    val reclamadas  = misiones.count { it.reclamada }
    val totalMove   = misiones.filter { it.reclamada }.sumOf { it.recompensaMove }

    // ── Contrarreloj hasta medianoche ──────────────────────────────────────
    var countdownText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val cal      = java.util.Calendar.getInstance()
            val h        = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val m        = cal.get(java.util.Calendar.MINUTE)
            val s        = cal.get(java.util.Calendar.SECOND)
            val secsLeft = (23 - h) * 3600L + (59 - m) * 60L + (60 - s)
            countdownText = "%02d:%02d:%02d".format(
                secsLeft / 3600, (secsLeft % 3600) / 60, secsLeft % 60
            )
            kotlinx.coroutines.delay(1000L)
        }
    }

    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error, success) {
        val msg = success ?: error
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FlowlyBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Misiones del día 🎯",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = FlowlyText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = FlowlyText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowlyBg)
            )
        }
    ) { padding ->

        if (isLoading) {
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
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Resumen del día ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(FlowlyCard, RoundedCornerShape(20.dp))
                    .border(1.dp, FlowlyBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("⏱ Se reinician en ", fontSize = 12.sp, color = FlowlyMuted)
                    Text(
                        countdownText,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = FlowlyAccent
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$completadas/${misiones.size}",
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color      = FlowlyAccent
                        )
                        Text("completadas", fontSize = 11.sp, color = FlowlyMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$reclamadas/${misiones.size}",
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color      = FlowlyAccent2
                        )
                        Text("reclamadas", fontSize = 11.sp, color = FlowlyMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "+$totalMove",
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color      = FlowlySuccess
                        )
                        Text("MOVE ganados", fontSize = 11.sp, color = FlowlyMuted)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tarjetas de misiones ────────────────────────────────────
            misiones.forEach { mision ->
                MisionCard(
                    mision   = mision,
                    onClamar = { vm.reclamar(mision) }
                )
                Spacer(Modifier.height(10.dp))
            }

            // ── Tip motivacional ────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(FlowlyCard2, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "💡 Completá todas las misiones diarias para ganar hasta 450 MOVE extra. " +
                    "¡Las misiones se renuevan cada día a medianoche!",
                    fontSize   = 12.sp,
                    color      = FlowlyMuted,
                    lineHeight = 18.sp,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Tarjeta individual de misión ──────────────────────────────────────────────

@Composable
private fun MisionCard(mision: DailyMision, onClamar: () -> Unit) {
    val cardBorder = when {
        mision.reclamada  -> FlowlyAccent.copy(alpha = 0.4f)
        mision.completada -> FlowlyAccent
        else              -> FlowlyBorder
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(FlowlyCard, RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji en círculo
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(FlowlyCard2, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(mision.emoji, fontSize = 22.sp)
                }
                Column {
                    Text(
                        mision.titulo,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = FlowlyText
                    )
                    Text(
                        mision.descripcion,
                        fontSize = 12.sp,
                        color    = FlowlyMuted
                    )
                }
            }
            // Badge de recompensa
            Box(
                modifier = Modifier
                    .background(
                        if (mision.reclamada) FlowlyCard2 else FlowlyAccent.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "+${mision.recompensaMove}",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (mision.reclamada) FlowlyMuted else FlowlyAccent
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Barra de progreso
        val progressColor = when {
            mision.reclamada  -> FlowlyMuted
            mision.completada -> FlowlyAccent
            else              -> FlowlyAccent3
        }
        FlowlyProgressBar(progress = mision.progreso, color = progressColor)

        Spacer(Modifier.height(6.dp))

        // Progreso en texto + botón
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    mision.reclamada  -> "✅ Recompensa reclamada"
                    mision.completada -> "¡Misión completada!"
                    else              -> "${(mision.progreso * 100).toInt()}% completado"
                },
                fontSize   = 12.sp,
                color      = when {
                    mision.reclamada  -> FlowlyMuted
                    mision.completada -> FlowlyAccent
                    else              -> FlowlyMuted
                }
            )

            if (mision.completada && !mision.reclamada) {
                Button(
                    onClick  = onClamar,
                    modifier = Modifier.height(32.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = FlowlyAccent),
                    shape    = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text(
                        "Reclamar 🎁",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.Black
                    )
                }
            }
        }
    }
}

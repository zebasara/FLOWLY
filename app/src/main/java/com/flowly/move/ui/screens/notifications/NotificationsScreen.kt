package com.flowly.move.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.flowly.move.data.model.Notificacion
import com.flowly.move.ui.components.FlowlyScaffold
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

private fun formatRelativeTime(timestamp: Long): String {
    val diffMs   = System.currentTimeMillis() - timestamp
    val diffDays = diffMs / (1000L * 60 * 60 * 24)
    return when {
        diffDays == 0L -> "Hoy"
        diffDays == 1L -> "Ayer"
        diffDays < 7L  -> "Hace $diffDays días"
        diffDays < 30L -> "Hace ${diffDays / 7} semanas"
        else           -> "Hace ${diffDays / 30} meses"
    }
}

private fun dotColorForTipo(tipo: String): Color = when (tipo) {
    "video"      -> Color(0xFF7EE621) // FlowlyAccent  – verde
    "pago"       -> Color(0xFF4ADE80) // FlowlySuccess – verde claro
    "logro"      -> Color(0xFF818CF8) // FlowlyAccent3 – violeta
    "referido"   -> Color(0xFFA855F7) // FlowlyPurple
    "movimiento" -> Color(0xFFFB923C) // naranja  – actividad física
    "mision"     -> Color(0xFFFACC15) // amarillo – misiones diarias
    "nivel"      -> Color(0xFF38BDF8) // celeste  – subida de nivel
    "campeon"    -> Color(0xFFFBBF24) // dorado   – campeón semanal
else         -> Color(0xFF6B7280) // FlowlyMuted
}

@Composable
fun NotificationsScreen(navController: NavController) {
    val vm: NotificationsViewModel = viewModel()
    val notifs    by vm.notifs.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    FlowlyScaffold(navController = navController, currentRoute = Routes.NOTIFICATIONS) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Notificaciones",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = FlowlyText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FlowlyAccent)
                }
            } else if (notifs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No tenés notificaciones todavía.",
                        fontSize = 14.sp,
                        color = FlowlyMuted
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    notifs.forEach { notif ->
                        NotifRow(
                            notif    = notif,
                            onTap    = { if (!notif.leida) vm.marcarLeida(notif.id) }
                        )
                        HorizontalDivider(color = FlowlyBorder)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NotifRow(notif: Notificacion, onTap: () -> Unit) {
    val dotColor   = dotColorForTipo(notif.tipo)
    val timeString = formatRelativeTime(notif.createdAt)
    val dimmed     = notif.leida

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dot (sin leer = color, leída = transparente)
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .background(
                    if (dimmed) Color.Transparent else dotColor,
                    CircleShape
                )
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                notif.titulo,
                fontSize = 14.sp,
                fontWeight = if (dimmed) FontWeight.Normal else FontWeight.SemiBold,
                color = if (dimmed) FlowlyMuted else FlowlyText
            )
            Text(
                notif.cuerpo,
                fontSize = 12.sp,
                color = FlowlyMuted,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                timeString,
                fontSize = 11.sp,
                color = FlowlyMuted.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

package com.flowly.move.ui.screens.canjes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/** UI-only model: combina la oferta de Firestore con el estado del usuario. */
private data class CanjeOption(
    val label: String,
    val move: Int,
    val available: Boolean,
    val missing: Int = 0,
    /** true si el usuario no cumple el nivel mínimo (y tiene suficiente MOVE) */
    val nivelBloqueado: Boolean = false
)

@Composable
fun CanjesScreen(navController: NavController) {
    val vm: CanjesViewModel = viewModel()
    val user           by vm.user.collectAsStateWithLifecycle()
    val isLoading      by vm.isLoading.collectAsStateWithLifecycle()
    val mercadoPagoUrl by vm.mercadoPagoUrl.collectAsStateWithLifecycle()
    val canjesConfig   by vm.canjesConfig.collectAsStateWithLifecycle()
    val uriHandler      = LocalUriHandler.current

    val tokensLibres = user?.tokensActuales ?: 0
    val nivelUsuario = user?.nivel          ?: 1
    val aliasMP      = user?.aliasMercadoPago ?: ""

    var showAliasDialog by remember { mutableStateOf(false) }

    // Construir opciones desde la config dinámica de Firestore
    val options = canjesConfig.opciones
        .filter { it.activo }
        .map { oferta ->
            val cumpleMove  = tokensLibres >= oferta.move
            val cumpleNivel = nivelUsuario >= canjesConfig.nivelMinimo
            CanjeOption(
                label          = oferta.label,
                move           = oferta.move,
                available      = cumpleMove && cumpleNivel,
                missing        = if (!cumpleMove) maxOf(0, oferta.move - tokensLibres) else 0,
                nivelBloqueado = cumpleMove && !cumpleNivel
            )
        }

    // Dialog cuando el alias de MP está vacío
    if (showAliasDialog) {
        AlertDialog(
            onDismissRequest = { showAliasDialog = false },
            containerColor   = FlowlyCard2,
            title = { Text("Alias requerido", color = FlowlyText, fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Para recibir recompensas necesitás configurar tu alias en tu perfil.",
                    color = FlowlyMuted, fontSize = 14.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAliasDialog = false
                    navController.navigate(Routes.PROFILE)
                }) {
                    Text("Ir al perfil", color = FlowlyAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAliasDialog = false }) {
                    Text("Cancelar", color = FlowlyMuted)
                }
            }
        )
    }

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

            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Recompensas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
                    Text("saldo libre: %,d MOVE".format(tokensLibres), fontSize = 12.sp, color = FlowlyMuted)
                }
                // Badge de nivel
                Box(
                    modifier = Modifier
                        .background(FlowlyCard2, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "Nivel $nivelUsuario",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = FlowlyAccent
                    )
                }
            }

            // ── Alias MP ─────────────────────────────────────────────────────
            FlowlyCard2(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .then(
                        if (aliasMP.isBlank())
                            Modifier.clickable { navController.navigate(Routes.PROFILE) }
                        else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Método de recompensa", fontSize = 12.sp, color = FlowlyMuted)
                    if (aliasMP.isBlank()) {
                        Text(
                            "⚠ Tocar para configurar →",
                            fontSize = 12.sp,
                            color = FlowlyWarn
                        )
                    } else {
                        Text(aliasMP, fontSize = 12.sp, color = FlowlyAccent)
                    }
                }
            }

            // ── Aviso de nivel mínimo ─────────────────────────────────────────
            if (canjesConfig.nivelMinimo > 1 && nivelUsuario < canjesConfig.nivelMinimo) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(FlowlyWarn.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔒", fontSize = 16.sp)
                    Text(
                        "Necesitás alcanzar el Nivel ${canjesConfig.nivelMinimo} para canjear. Seguí viendo videos y moviéndote.",
                        fontSize   = 12.sp,
                        color      = FlowlyWarn,
                        lineHeight = 17.sp,
                        modifier   = Modifier.weight(1f)
                    )
                }
            }

            SectionTitle(modifier = Modifier.padding(horizontal = 16.dp), text = "recompensas por validación")

            // ── Opciones dinámicas desde Firestore ───────────────────────────
            if (options.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay opciones de recompensa disponibles en este momento.",
                        fontSize = 13.sp,
                        color = FlowlyMuted
                    )
                }
            } else {
                options.forEach { opt ->
                    CanjeItem(
                        label          = opt.label,
                        move           = opt.move,
                        available      = opt.available,
                        missing        = opt.missing,
                        nivelBloqueado = opt.nivelBloqueado,
                        nivelMinimo    = canjesConfig.nivelMinimo,
                        modifier       = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (opt.available) {
                            if (aliasMP.isBlank()) {
                                showAliasDialog = true
                            } else {
                                navController.navigate(Routes.confirmCanje(opt.label, opt.move.toString()))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Nota dinámica desde Firestore ────────────────────────────────
            FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    canjesConfig.notaMensaje,
                    fontSize   = 12.sp,
                    color      = FlowlyMuted,
                    lineHeight = 18.sp
                )
            }

            // ── Botón Mercado Pago (solo si hay URL configurada desde admin) ─
            if (mercadoPagoUrl.isNotBlank()) {
                SectionTitle(modifier = Modifier.padding(horizontal = 16.dp), text = "método de acreditación")

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
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("💳", fontSize = 26.sp)
                        Column {
                            Text(
                                "Configurar método de acreditación",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = FlowlyText
                            )
                            Text(
                                "Necesario para recibir tus recompensas",
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
                    Text("Mis recompensas", fontSize = 12.sp, color = FlowlyMuted)
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

// ── Composables privados ──────────────────────────────────────────────────────

@Composable
private fun CanjeItem(
    label: String,
    move: Int,
    available: Boolean,
    missing: Int,
    nivelBloqueado: Boolean,
    nivelMinimo: Int,
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
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                label,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (available) FlowlyText else FlowlyMuted
            )
            val subtitle = when {
                available      -> "%,d MOVE · disponible".format(move)
                nivelBloqueado -> "%,d MOVE · requiere Nivel $nivelMinimo".format(move)
                else           -> "%,d MOVE · te faltan %,d".format(move, missing)
            }
            Text(
                subtitle,
                fontSize = 12.sp,
                color    = FlowlyMuted,
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
                Text(
                    if (nivelBloqueado) "Nivel $nivelMinimo" else "Bloqueado",
                    fontSize = 12.sp,
                    color    = FlowlyMuted
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(modifier: Modifier = Modifier, text: String) {
    Text(
        text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.Bold,
        color      = FlowlyTextSub,
        modifier   = modifier.padding(top = 20.dp, bottom = 10.dp)
    )
}

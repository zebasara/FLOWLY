package com.flowly.move.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.data.model.NIVEL_LIMITES
import com.flowly.move.data.model.TODAS_LAS_INSIGNIAS
import com.flowly.move.data.repository.FlowlyRepository
import com.flowly.move.ui.screens.misiones.getMisionesDelDia
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

@Composable
fun HomeScreen(navController: NavController) {
    val vm: UserViewModel = viewModel()
    val user              by vm.user.collectAsStateWithLifecycle()
    val isLoading         by vm.isLoading.collectAsStateWithLifecycle()
    val showWelcome       by vm.showWelcomeDialog.collectAsStateWithLifecycle()
    val pendingBadge      by vm.pendingBadge.collectAsStateWithLifecycle()

    val nombre         = user?.nombre?.substringBefore(" ") ?: ""
    val ciudad         = listOfNotNull(
        user?.ciudad?.takeIf { it.isNotBlank() },
        user?.provincia?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
    val tokensActuales = user?.tokensActuales ?: 0
    val nivel          = user?.nivel ?: 1
    val limiteTokens   = NIVEL_LIMITES.getOrElse(nivel - 1) { NIVEL_LIMITES.first() }
    val rachaDias      = user?.diasConsecutivosVideos ?: 0
    val misiones       = user?.let { getMisionesDelDia(it) } ?: emptyList()
    val misionesComp   = misiones.count { it.completada }
    val misionesTotal  = misiones.size
    val movHoy         = user?.tokenMovimientoHoy ?: 0
    val kmHoy          = user?.kmHoy ?: 0f
    val moveVideos     = user?.tokenVideosHoy ?: 0
    val videosHoy      = if (moveVideos > 0) (moveVideos / 50).coerceAtLeast(1) else 0
    val moveEnHolding  = user?.moveEnHolding ?: 0
    val iniciales      = (user?.nombre ?: "")
        .split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }

    // Refrescar datos cada vez que el usuario vuelve a esta pantalla (ej: desde el mapa)
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == Routes.HOME) vm.refreshSilently()
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    // Diálogo de bienvenida — 100 MOVE de registro
    if (showWelcome) {
        WelcomeDialog(
            tokensActuales = tokensActuales,
            onDismiss      = { vm.dismissWelcomeDialog() }
        )
    }

    // Diálogo de insignia
    pendingBadge?.let { badgeId ->
        TODAS_LAS_INSIGNIAS[badgeId]?.let { insignia ->
            BadgeDialog(
                emoji       = insignia.emoji,
                titulo      = insignia.titulo,
                descripcion = insignia.descripcion,
                onDismiss   = { vm.dismissBadgeDialog(badgeId) }
            )
        }
    }

    FlowlyScaffold(navController = navController, currentRoute = Routes.HOME) { padding ->
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
                    Text(
                        if (nombre.isBlank()) "Hola 👋" else "Hola, $nombre 👋",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlowlyText
                    )
                    if (ciudad.isNotBlank())
                        Text(ciudad, fontSize = 12.sp, color = FlowlyMuted)
                }
                FlowlyAvatar(
                    initials = iniciales,
                    photoUrl = user?.profilePhotoUrl ?: "",
                    size     = 38.dp,
                    modifier = Modifier.clickable { navController.navigate(Routes.PROFILE) }
                )
            }

            // Hero card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(FlowlyCard, RoundedCornerShape(20.dp))
                    .border(1.dp, FlowlyBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("tu saldo MOVE", fontSize = 12.sp, color = FlowlyMuted)
                Text(
                    "%,d".format(tokensActuales),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowlyAccent,
                    letterSpacing = (-1).sp
                )
                Text("tokens acumulados · nivel $nivel", fontSize = 12.sp, color = FlowlyMuted)

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = FlowlyBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nivel $nivel", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent2)
                        Text("%,d límite".format(limiteTokens), fontSize = 12.sp, color = FlowlyMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$rachaDias/5 días", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent)
                        Text("racha videos", fontSize = 12.sp, color = FlowlyMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$movHoy", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FlowlySuccess)
                        Text("mov. hoy", fontSize = 12.sp, color = FlowlyMuted)
                    }
                }
            }

            // Hoy
            SectionTitle(modifier = Modifier.padding(horizontal = 16.dp), text = "hoy")

            val movLimiteDiario = FlowlyRepository.DAILY_LIMIT_MOVIMIENTO
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("$movHoy", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent2)
                    Text("MOVE movimiento", fontSize = 11.sp, color = FlowlyMuted)
                    FlowlyProgressBar(progress = (movHoy / movLimiteDiario.toFloat()).coerceIn(0f, 1f), color = FlowlyAccent)
                    Text(
                        if (kmHoy > 0f) "%.2f km recorridos".format(kmHoy)
                        else "${(movHoy * 100 / movLimiteDiario).coerceIn(0, 100)}% del límite",
                        fontSize = 11.sp,
                        color    = FlowlyMuted
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(FlowlyCard2, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("$moveVideos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent3)
                    Text("MOVE de videos", fontSize = 11.sp, color = FlowlyMuted)
                    FlowlyProgressBar(progress = (moveVideos.toFloat() / FlowlyRepository.DAILY_LIMIT_VIDEOS).coerceIn(0f, 1f), color = FlowlyAccent3)
                    Text(
                        if (videosHoy > 0) "$videosHoy ${if (videosHoy == 1) "video" else "videos"} hoy"
                        else "sin videos hoy",
                        fontSize = 11.sp,
                        color    = FlowlyMuted
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Misiones del día
            FlowlyCard2(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { navController.navigate(Routes.MISIONES) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 22.sp)
                        Column {
                            Text(
                                "Misiones del día",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = FlowlyText
                            )
                            Text(
                                if (misionesComp == misionesTotal && misionesTotal > 0)
                                    "¡Todas completadas! 🏆"
                                else
                                    "$misionesComp/$misionesTotal completadas · hasta 450 MOVE",
                                fontSize = 12.sp,
                                color    = if (misionesComp == misionesTotal && misionesTotal > 0)
                                    FlowlyAccent else FlowlyMuted
                            )
                        }
                    }
                    Text("→", fontSize = 18.sp, color = FlowlyMuted)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Holding
            FlowlyCard2(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { navController.navigate(Routes.HOLDING) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("En holding", fontSize = 12.sp, color = FlowlyMuted)
                    Text(
                        "%,d MOVE bloqueados".format(moveEnHolding),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = FlowlyAccent2
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // CTAs
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                FlowlyPrimaryButton(
                    text = "Ver video y ganar 50 MOVE",
                    onClick = { navController.navigate(Routes.VIDEO) }
                )
                Spacer(Modifier.height(8.dp))
                FlowlyOutlineButton(
                    text = "Ver mi recorrido en mapa",
                    onClick = { navController.navigate(Routes.MAP) }
                )
                Spacer(Modifier.height(8.dp))
                FlowlySecondaryButton(
                    text = "Sistema de niveles",
                    onClick = { navController.navigate(Routes.LEVELS) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Quick access row
            SectionTitle(modifier = Modifier.padding(horizontal = 16.dp), text = "accesos rápidos")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAccessItem("🔔", "Notifs", modifier = Modifier.weight(1f)) {
                    navController.navigate(Routes.NOTIFICATIONS)
                }
                QuickAccessItem("👥", "Referir", modifier = Modifier.weight(1f)) {
                    navController.navigate(Routes.REFERRALS)
                }
                QuickAccessItem("⛓", "Blockchain", modifier = Modifier.weight(1f)) {
                    navController.navigate(Routes.BLOCKCHAIN)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Diálogo de bienvenida ──────────────────────────────────────────

@Composable
private fun WelcomeDialog(tokensActuales: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(FlowlyCard, RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "¡Felicitaciones!",
                fontSize    = 22.sp,
                fontWeight  = FontWeight.Bold,
                color       = FlowlyText,
                textAlign   = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Ganaste 100 MOVE por registrarte en MOVE.",
                fontSize   = 14.sp,
                color      = FlowlyMuted,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "+100 MOVE",
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyAccent
            )
            Text(
                "acreditados en tu cuenta",
                fontSize = 12.sp,
                color    = FlowlyMuted
            )
            Spacer(Modifier.height(16.dp))
            FlowlyCard2 {
                Text(
                    "🏪 La tienda se abrirá cuando haya 500 usuarios en MOVE. ¡Invitá amigos!",
                    fontSize   = 12.sp,
                    color      = FlowlyMuted,
                    lineHeight = 18.sp,
                    textAlign  = TextAlign.Center
                )
            }
            Spacer(Modifier.height(20.dp))
            FlowlyPrimaryButton(text = "¡Empezar!", onClick = onDismiss)
        }
    }
}

// ── Diálogo de insignia ────────────────────────────────────────────

@Composable
private fun BadgeDialog(emoji: String, titulo: String, descripcion: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(FlowlyCard, RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "¡Nueva insignia!",
                fontSize   = 13.sp,
                color      = FlowlyAccent,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                titulo,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyText,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                descripcion,
                fontSize   = 14.sp,
                color      = FlowlyMuted,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))
            FlowlyPrimaryButton(text = "¡Gracias!", onClick = onDismiss)
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────

@Composable
private fun QuickAccessItem(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(FlowlyCard2, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 22.sp)
        Text(label, fontSize = 11.sp, color = FlowlyMuted)
    }
}

@Composable
private fun SectionTitle(modifier: Modifier = Modifier, text: String) {
    Text(
        text.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = FlowlyMuted.copy(alpha = 0.7f),
        letterSpacing = 1.sp,
        modifier      = modifier.padding(top = 14.dp, bottom = 8.dp)
    )
}

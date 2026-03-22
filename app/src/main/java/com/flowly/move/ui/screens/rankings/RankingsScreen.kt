package com.flowly.move.ui.screens.rankings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.flowly.move.data.model.CampeonSemanal
import com.flowly.move.data.model.User
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar

// ── Helpers de countdown (top-level, sin estado) ─────────────────────────────

/** Calcula el string de cuenta regresiva al próximo lunes a las 15:00. */
private fun calcCountdown(): String {
    val now     = Calendar.getInstance()
    val nowDay  = now.get(Calendar.DAY_OF_WEEK)
    val nowHour = now.get(Calendar.HOUR_OF_DAY)
    val nowMin  = now.get(Calendar.MINUTE)

    // Justo en el momento de asignación
    if (nowDay == Calendar.MONDAY && nowHour == 15 && nowMin == 0) {
        return "¡Asignando ahora! 🏆"
    }

    // Target = próximo lunes a las 15:00:00
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 15)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysToAdd = when (nowDay) {
        Calendar.MONDAY    -> if (nowHour < 15) 0 else 7
        Calendar.TUESDAY   -> 6
        Calendar.WEDNESDAY -> 5
        Calendar.THURSDAY  -> 4
        Calendar.FRIDAY    -> 3
        Calendar.SATURDAY  -> 2
        Calendar.SUNDAY    -> 1
        else               -> 7
    }
    target.add(Calendar.DAY_OF_YEAR, daysToAdd)

    val diffMs = target.timeInMillis - System.currentTimeMillis()
    if (diffMs <= 0L) return "¡Asignando ahora! 🏆"

    val totalSecs = diffMs / 1000L
    val days      = totalSecs / 86400L
    val hours     = (totalSecs % 86400L) / 3600L
    val minutes   = (totalSecs % 3600L) / 60L
    val seconds   = totalSecs % 60L

    return buildString {
        if (days > 0) append("${days}d ")
        append("%02dh %02dm %02ds".format(hours, minutes, seconds))
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun RankingsScreen(navController: NavController) {
    val vm: RankingsViewModel = viewModel()
    val currentUser  by vm.currentUser.collectAsStateWithLifecycle()
    val rankings     by vm.rankings.collectAsStateWithLifecycle()
    val campeon      by vm.campeon.collectAsStateWithLifecycle()
    val topArgentina by vm.topArgentina.collectAsStateWithLifecycle()
    val isLoading    by vm.isLoading.collectAsStateWithLifecycle()

    // Countdown en vivo: se actualiza cada segundo
    var countdown by remember { mutableStateOf(calcCountdown()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            countdown = calcCountdown()
        }
    }

    // Default tab 2 = Argentina ("all")
    var tabIndex by remember { mutableIntStateOf(2) }
    val tabs      = listOf("Mi ciudad", "Mi provincia", "Argentina")
    val scopeKeys = listOf("ciudad",    "provincia",    "all")

    val subtitle = when (tabIndex) {
        0 -> currentUser?.ciudad?.ifBlank { "tu ciudad" }       ?: "tu ciudad"
        1 -> currentUser?.provincia?.ifBlank { "tu provincia" } ?: "tu provincia"
        else -> "Argentina"
    }

    FlowlyScaffold(navController = navController, currentRoute = Routes.RANKINGS) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Rankings",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyText,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // ── Potencial Campeón (líder actual + countdown) ──────────────
            topArgentina?.let { lider ->
                PotencialCampeonCard(
                    lider     = lider,
                    countdown = countdown,
                    isMe      = lider.uid == currentUser?.uid,
                    esDefensor = lider.uid == campeon?.uid,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Campeón Semanal confirmado ────────────────────────────────
            campeon?.let { c ->
                if (c.uid.isNotBlank()) {
                    CampeonCard(
                        campeon  = c,
                        isMe     = c.uid == currentUser?.uid,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Tabs ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(FlowlyCard2, RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                tabs.forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (tabIndex == i) FlowlyCard else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                tabIndex = i
                                vm.loadRankings(scopeKeys[i])
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (tabIndex == i) FlowlyText else FlowlyMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "$subtitle · Top 50",
                fontSize = 12.sp,
                color    = FlowlyMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (isLoading) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FlowlyAccent)
                }
            } else if (rankings.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay usuarios en este ranking todavía.",
                        fontSize = 14.sp,
                        color    = FlowlyMuted
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    rankings.forEachIndexed { index, user ->
                        val isMe = user.uid == currentUser?.uid
                        RankingRow(pos = index + 1, user = user, isMe = isMe)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ── Card: Potencial Campeón (líder actual + countdown) ────────────────────────

@Composable
private fun PotencialCampeonCard(
    lider: User,
    countdown: String,
    isMe: Boolean,
    esDefensor: Boolean,   // true = es el campeón actual Y va ganando
    modifier: Modifier = Modifier
) {
    val label = when {
        isMe && esDefensor -> "🔄 Vas a renovar tu corona"
        isMe               -> "🏅 ¡Vas liderando!"
        esDefensor         -> "🔄 ${lider.nombre.split(" ").first()} va a renovar su corona"
        else               -> "🏅 Va liderando · potencial campeón"
    }

    val location = listOfNotNull(
        lider.ciudad.takeIf { it.isNotBlank() },
        lider.provincia.takeIf { it.isNotBlank() }
    ).joinToString(", ")

    // Iniciales del líder
    val iniciales = lider.nombre.trim().split(" ")
        .filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0D2A1A), Color(0xFF0A1A2A)))
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(FlowlyAccent.copy(alpha = 0.8f), FlowlyAccent3.copy(alpha = 0.6f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        // Fila principal: avatar + info + MOVE
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(FlowlyAccent, Color(0xFF4A8A10)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (lider.profilePhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model              = lider.profilePhotoUrl,
                        contentDescription = lider.nombre,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Text(
                        iniciales,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF0A120A)
                    )
                }
            }

            // Nombre y ubicación
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = FlowlyAccent
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isMe) "¡Vos! · ${lider.nombre}" else lider.nombre,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isMe) FlowlyAccent else FlowlyText,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (location.isNotBlank()) {
                    Text(
                        "📍 $location",
                        fontSize = 11.sp,
                        color    = FlowlyMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // MOVE
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%,d".format(lider.tokensActuales),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyAccent
                )
                Text("MOVE", fontSize = 9.sp, color = FlowlyMuted)
            }
        }

        // Divisor sutil
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FlowlyAccent.copy(alpha = 0.15f))
        )
        Spacer(Modifier.height(8.dp))

        // Countdown
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "⏱ Próxima corona:",
                fontSize   = 11.sp,
                color      = FlowlyMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                countdown,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = if (countdown.contains("Asignando")) Color(0xFFF59E0B) else FlowlyText
            )
        }
    }
}

// ── Card: Campeón Semanal confirmado ──────────────────────────────────────────

@Composable
private fun CampeonCard(
    campeon: CampeonSemanal,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val iniciales = campeon.nombre.trim().split(" ")
        .filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF2A1F00), Color(0xFF1A2A00)))
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFF59E0B), Color(0xFF7EE621))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar dorado
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (campeon.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model              = campeon.photoUrl,
                        contentDescription = campeon.nombre,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Text(iniciales, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "👑 Campeón Semanal",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFFF59E0B)
                    )
                    if (campeon.racha > 1) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "×${campeon.racha} 🔥",
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isMe) "¡Vos! · ${campeon.nombre}" else campeon.nombre,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isMe) FlowlyAccent else FlowlyText,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                val location = listOfNotNull(
                    campeon.ciudad.takeIf { it.isNotBlank() },
                    campeon.provincia.takeIf { it.isNotBlank() }
                ).joinToString(", ")
                if (location.isNotBlank()) {
                    Text(
                        "📍 $location",
                        fontSize = 11.sp,
                        color    = FlowlyMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // MOVE
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%,d".format(campeon.tokensActuales),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFF59E0B)
                )
                Text("MOVE", fontSize = 9.sp, color = FlowlyMuted)
            }
        }
    }
}

// ── Fila individual de ranking ────────────────────────────────────────────────

@Composable
private fun RankingRow(pos: Int, user: User, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMe)
                    Modifier
                        .background(FlowlyAccent.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                        .border(1.dp, FlowlyAccent.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                else
                    Modifier.padding(vertical = 10.dp)
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Posición
        Text(
            "#$pos",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when (pos) {
                1    -> Color(0xFFF59E0B)
                2    -> Color(0xFF9CA3AF)
                3    -> Color(0xFFCD7F32)
                else -> if (isMe) FlowlyAccent else FlowlyMuted
            },
            modifier = Modifier.width(28.dp)
        )

        // Avatar
        if (user.profilePhotoUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(FlowlyCard2)
            ) {
                AsyncImage(
                    model              = user.profilePhotoUrl,
                    contentDescription = "Foto de ${user.nombre}",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            }
        } else {
            FlowlyAvatar(initials = user.iniciales, size = 34.dp)
        }

        // Nombre y MOVE
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    if (isMe) "Vos · ${user.nombre}" else user.nombre,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = if (isMe) FlowlyAccent else FlowlyText,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                // Coronita si tiene racha de campeón
                if (user.campeonSemanalRacha > 0) {
                    Text("👑", fontSize = 12.sp)
                }
            }
            Text(
                "%,d MOVE".format(user.tokensActuales),
                fontSize = 12.sp,
                color    = FlowlyMuted
            )
        }

        // Medalla
        when (pos) {
            1    -> Text("🥇", fontSize = 18.sp)
            2    -> Text("🥈", fontSize = 18.sp)
            3    -> Text("🥉", fontSize = 18.sp)
            else -> if (isMe) TagGreen("tú")
        }
    }
}

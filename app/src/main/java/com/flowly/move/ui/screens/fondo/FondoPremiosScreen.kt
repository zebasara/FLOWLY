package com.flowly.move.ui.screens.fondo

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.flowly.move.data.model.DISTRIBUCION_FONDO
import com.flowly.move.data.model.User
import com.flowly.move.data.model.iniciales
import com.flowly.move.ui.components.FlowlyAvatar
import com.flowly.move.ui.components.FlowlyPrimaryButton
import com.flowly.move.ui.components.FlowlyScaffold
import com.flowly.move.ui.screens.store.StoreViewModel
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun Long.formatARS(): String =
    "%,d".format(this).replace(",", ".")

private fun mesActualNombre(): String {
    val meses = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
    return meses[Calendar.getInstance().get(Calendar.MONTH)]
}

private fun cierreDelMes(): String {
    val cal = Calendar.getInstance()
    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return "31 de ${mesActualNombre()}"
        .let { if (lastDay != 31) "$lastDay de ${mesActualNombre()}" else it }
}

private fun calcCountdown(): String {
    val now = Calendar.getInstance()
    val fin = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMs = fin.timeInMillis - now.timeInMillis
    if (diffMs <= 0L) return "¡Repartiendo premios!"
    val totalSecs = diffMs / 1000L
    val days  = totalSecs / 86400L
    val hours = (totalSecs % 86400L) / 3600L
    val mins  = (totalSecs % 3600L) / 60L
    val secs  = totalSecs % 60L
    return buildString {
        if (days > 0) append("${days}d ")
        append("%02dh %02dm %02ds".format(hours, mins, secs))
    }
}

private fun calcPremio(montoTotal: Long, posIndex: Int): Long {
    val pct = DISTRIBUCION_FONDO.getOrElse(posIndex) { 0 }
    return montoTotal * pct / 100L
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FondoPremiosScreen(navController: NavController) {
    val vm: FondoPremiosViewModel = viewModel()
    val storeVm: StoreViewModel   = viewModel()
    val fondo            by vm.fondo.collectAsStateWithLifecycle()
    val montoARS         by vm.montoARS.collectAsStateWithLifecycle()
    val ranking          by vm.ranking.collectAsStateWithLifecycle()
    val currentUser      by vm.currentUser.collectAsStateWithLifecycle()
    val isLoading        by vm.isLoading.collectAsStateWithLifecycle()
    val storeConfig      by storeVm.storeConfig.collectAsStateWithLifecycle()
    val comprobantesUrls by vm.comprobantesUrls.collectAsStateWithLifecycle()

    val baseUrl      = storeConfig?.referralBaseUrl?.trimEnd('/') ?: "https://flowly.app/r"
    val userCode     = currentUser?.uid?.take(8) ?: ""
    val referralLink = if (userCode.isNotBlank()) "$baseUrl/$userCode" else ""

    var countdown by remember { mutableStateOf(calcCountdown()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1_000L); countdown = calcCountdown() }
    }

    var showComprobantes by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    FlowlyScaffold(navController = navController, currentRoute = Routes.FONDO_PREMIOS) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlowlyAccent)
            }
            return@FlowlyScaffold
        }

        Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // Título
            Text(
                "🏆 Fondo de Beneficios",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyText,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // ── Card principal — fondo del mes ────────────────────────────
            FondoPrincipalCard(
                montoARS  = montoARS,
                countdown = countdown,
                modifier  = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Cómo se reparte ───────────────────────────────────────────
            DistribucionCard(
                montoTotal = montoARS,
                modifier   = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Ranking mensual top 10 ────────────────────────────────────
            RankingMensualCard(
                ranking     = ranking,
                currentUser = currentUser,
                montoTotal  = montoARS,
                modifier    = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Tu posición ───────────────────────────────────────────────
            val miPos = ranking.indexOfFirst { it.uid == currentUser?.uid }
            if (miPos >= 0) {
                MiPosicionCard(
                    posIndex    = miPos,
                    montoTotal  = montoARS,
                    move30Dias  = currentUser?.move30Dias ?: 0,
                    modifier    = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Cómo funciona ─────────────────────────────────────────────
            ComoFuncionaCard(
                pct      = fondo?.porcentajeAdmob ?: 35,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Compartir y sumar al fondo ────────────────────────────────
            CompartirCard(
                referralLink = referralLink,
                modifier     = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))
        }

        // ── Burbuja flotante de comprobantes ──────────────────────────────────
        if (comprobantesUrls.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 18.dp)
                    .shadow(8.dp, CircleShape)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(FlowlyAccentDark, Color(0xFF184000)))
                    )
                    .border(1.5.dp, FlowlyAccent.copy(alpha = 0.8f), CircleShape)
                    .clickable { showComprobantes = true },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧾", fontSize = 18.sp)
                    Text(
                        "Premios",
                        fontSize   = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = FlowlyAccent,
                        lineHeight = 10.sp
                    )
                }
            }
        }

        } // cierre Box

        // ── Modal Bottom Sheet: carousel de comprobantes ──────────────────────
        if (showComprobantes && comprobantesUrls.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest  = { showComprobantes = false },
                sheetState        = sheetState,
                containerColor    = FlowlyCard,
                dragHandle        = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(FlowlyAccent.copy(alpha = 0.5f))
                    )
                }
            ) {
                ComprobantesCarousel(urls = comprobantesUrls)
            }
        }
    }
}

// ── Card principal ────────────────────────────────────────────────────────────

@Composable
private fun FondoPrincipalCard(
    montoARS: Long,
    countdown: String,
    modifier: Modifier = Modifier
) {
    val sinDatos = montoARS == 0L

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF2A1A00), Color(0xFF1A2600)))
            )
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(Color(0xFFF59E0B), FlowlyAccent)),
                RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

            Text(
                "beneficios disponibles este mes",
                fontSize      = 12.sp,
                color         = Color(0xFFF59E0B).copy(alpha = 0.8f),
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))

            if (sinDatos) {
                Text(
                    "Actualizándose…",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyMuted
                )
            } else {
                Text(
                    montoARS.formatARS(),
                    fontSize      = 44.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = Color(0xFFF59E0B),
                    letterSpacing = (-1).sp
                )
            }

            Text(
                "en beneficios · ${mesActualNombre()}",
                fontSize = 13.sp,
                color    = FlowlyMuted
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF59E0B).copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Se reparte el",
                        fontSize = 11.sp,
                        color    = FlowlyMuted
                    )
                    Text(
                        cierreDelMes(),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = FlowlyText
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "⏱ Tiempo restante",
                        fontSize = 10.sp,
                        color    = FlowlyMuted
                    )
                    Text(
                        countdown,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (countdown.contains("Repartiendo")) Color(0xFFF59E0B) else FlowlyText
                    )
                }
            }
        }
    }
}

// ── Distribución ──────────────────────────────────────────────────────────────

private data class FilaPremio(
    val emoji: String,
    val label: String,
    val pct: Int,
    val monto: Long,
    val color: Color
)

@Composable
private fun DistribucionCard(montoTotal: Long, modifier: Modifier = Modifier) {
    val filas = listOf(
        FilaPremio("🥇", "1er puesto",  40, calcPremio(montoTotal, 0), Color(0xFFF59E0B)),
        FilaPremio("🥈", "2do puesto",  20, calcPremio(montoTotal, 1), Color(0xFF9CA3AF)),
        FilaPremio("🥉", "3er puesto",  12, calcPremio(montoTotal, 2), Color(0xFFCD7F32)),
        FilaPremio("4°–10°", "c/u",      4, calcPremio(montoTotal, 3), FlowlyMuted)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlowlyCard)
            .border(1.dp, FlowlyBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            "Distribución de beneficios",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = FlowlyText
        )
        Spacer(Modifier.height(12.dp))

        filas.forEachIndexed { i, fila ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(fila.emoji, fontSize = 20.sp)
                    Column {
                        Text(
                            fila.label,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = fila.color
                        )
                        Text(
                            "${fila.pct}% del fondo",
                            fontSize = 11.sp,
                            color    = FlowlyMuted
                        )
                    }
                }
                Text(
                    if (montoTotal > 0L) fila.monto.formatARS() else "—",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = fila.color
                )
            }
            if (i < filas.size - 1)
                HorizontalDivider(color = FlowlyBorder.copy(alpha = 0.5f))
        }
    }
}

// ── Ranking mensual ───────────────────────────────────────────────────────────

@Composable
private fun RankingMensualCard(
    ranking: List<User>,
    currentUser: User?,
    montoTotal: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlowlyCard)
            .border(1.dp, FlowlyBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "Top 10 del mes · Argentina",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = FlowlyText
            )
            Text(
                "MOVE 30d",
                fontSize = 11.sp,
                color    = FlowlyMuted
            )
        }

        Spacer(Modifier.height(10.dp))

        if (ranking.isEmpty()) {
            Text(
                "Sin actividad registrada este mes.",
                fontSize = 13.sp,
                color    = FlowlyMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            ranking.forEachIndexed { index, user ->
                val isMe = user.uid == currentUser?.uid
                MensualRow(
                    posIndex   = index,
                    user       = user,
                    isMe       = isMe,
                    montoTotal = montoTotal
                )
                if (index < ranking.size - 1)
                    HorizontalDivider(
                        color    = FlowlyBorder.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
            }
        }
    }
}

@Composable
private fun MensualRow(posIndex: Int, user: User, isMe: Boolean, montoTotal: Long) {
    val medalColor = when (posIndex) {
        0    -> Color(0xFFF59E0B)
        1    -> Color(0xFF9CA3AF)
        2    -> Color(0xFFCD7F32)
        else -> if (isMe) FlowlyAccent else FlowlyMuted
    }
    val medalEmoji = when (posIndex) {
        0 -> "🥇"
        1 -> "🥈"
        2 -> "🥉"
        else -> "#${posIndex + 1}"
    }
    val premio = calcPremio(montoTotal, posIndex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMe) Modifier
                    .background(FlowlyAccent.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .border(1.dp, FlowlyAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
                else Modifier.padding(vertical = 7.dp)
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Posición
        Text(
            medalEmoji,
            fontSize   = if (posIndex < 3) 18.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            color      = medalColor,
            modifier   = Modifier.width(30.dp)
        )

        // Avatar
        if (user.profilePhotoUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FlowlyCard2)
            ) {
                AsyncImage(
                    model              = user.profilePhotoUrl,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            }
        } else {
            FlowlyAvatar(initials = user.iniciales, size = 32.dp)
        }

        // Nombre y MOVE
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isMe) "Vos · ${user.nombre}" else user.nombre,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = if (isMe) FlowlyAccent else FlowlyText,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                "%,d MOVE este mes".format(user.move30Dias),
                fontSize = 11.sp,
                color    = FlowlyMuted
            )
        }

        // Premio estimado
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (montoTotal > 0L) premio.formatARS() else "—",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = medalColor
            )
            Text("est.", fontSize = 9.sp, color = FlowlyMuted)
        }
    }
}

// ── Mi posición ───────────────────────────────────────────────────────────────

@Composable
private fun MiPosicionCard(
    posIndex: Int,
    montoTotal: Long,
    move30Dias: Int,
    modifier: Modifier = Modifier
) {
    val premio = calcPremio(montoTotal, posIndex)
    val medalColor = when (posIndex) {
        0    -> Color(0xFFF59E0B)
        1    -> Color(0xFF9CA3AF)
        2    -> Color(0xFFCD7F32)
        else -> FlowlyAccent
    }
    val medalEmoji = when (posIndex) {
        0 -> "🥇"
        1 -> "🥈"
        2 -> "🥉"
        else -> "🏅"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlowlyAccent.copy(alpha = 0.07f))
            .border(1.dp, FlowlyAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    "$medalEmoji Tu posición este mes",
                    fontSize   = 12.sp,
                    color      = FlowlyAccent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "#${posIndex + 1} · %,d MOVE".format(move30Dias),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyText
                )
                Text(
                    "beneficio estimado si terminás aquí",
                    fontSize = 11.sp,
                    color    = FlowlyMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (montoTotal > 0L) premio.formatARS() else "—",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = medalColor
                )
                Text("est.", fontSize = 10.sp, color = FlowlyMuted)
            }
        }
    }
}

// ── Compartir ─────────────────────────────────────────────────────────────────

@Composable
private fun CompartirCard(referralLink: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlowlyCard)
            .border(1.dp, FlowlyBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("🚀", fontSize = 22.sp)
            Column {
                Text(
                    "Invitá amigos y crecé juntos",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = FlowlyText
                )
                Text(
                    "Más usuarios = fondo más grande para todos",
                    fontSize = 11.sp,
                    color    = FlowlyMuted
                )
            }
        }
        FlowlyPrimaryButton(
            text    = "Compartir Flowly con mi código",
            onClick = {
                if (referralLink.isBlank()) return@FlowlyPrimaryButton
                val texto = "¡Sumate a Flowly y ganá recompensas exclusivas! Sumá MOVE caminando y viendo videos. Registrate con mi código: $referralLink"
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, texto)
                        },
                        "Compartir Flowly"
                    )
                )
            }
        )
    }
}

// ── Cómo funciona ─────────────────────────────────────────────────────────────

@Composable
private fun ComoFuncionaCard(pct: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlowlyCard2)
            .border(1.dp, FlowlyBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            "💡 ¿Cómo funciona?",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = FlowlyText
        )
        Spacer(Modifier.height(10.dp))

        listOf(
            "📺" to "Cada vez que ves un anuncio en la app, generás actividad para el fondo de beneficios.",
            "💰" to "Flowly destina el $pct% de esos ingresos al fondo de beneficios mensual.",
            "🏆" to "Al cierre del mes, el top 10 de Argentina (por MOVE del mes) accede a los beneficios del período.",
            "📈" to "Más usuarios = más actividad = fondo más grande. ¡Invitá a tus amigos!"
        ).forEach { (emoji, texto) ->
            Row(
                modifier              = Modifier.padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(emoji, fontSize = 16.sp)
                Text(
                    texto,
                    fontSize    = 12.sp,
                    color       = FlowlyMuted,
                    lineHeight  = 18.sp,
                    modifier    = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FlowlyAccent.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(
                "El ranking mensual se basa en los MOVE ganados en los últimos 30 días, no en el saldo total. Esto le da chances a todos.",
                fontSize   = 11.sp,
                color      = FlowlyAccent.copy(alpha = 0.9f),
                textAlign  = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Carousel de comprobantes de pago ──────────────────────────────────────────

@Composable
private fun ComprobantesCarousel(urls: List<String>) {
    val filtered = urls.filter { it.isNotBlank() }
    val pagerState = rememberPagerState(pageCount = { filtered.size })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "🧾 Historial de beneficios",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyAccent
            )
            Text(
                "Beneficios entregados a los 10 mejores del mes anterior",
                fontSize = 12.sp,
                color    = FlowlyMuted
            )
        }

        Spacer(Modifier.height(12.dp))

        // Pager de imágenes
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FlowlyCard2)
                    .border(1.dp, FlowlyAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(filtered[page])
                        .size(Size(800, 1066))          // decodifica a 800×1066 px máx (3:4)
                        .crossfade(300)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .allowHardware(true)
                        .build(),
                    contentDescription = "Comprobante ${page + 1}",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize(),
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color       = FlowlyAccent,
                                strokeWidth = 2.dp,
                                modifier    = Modifier.size(32.dp)
                            )
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Indicadores de página
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(filtered.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                FlowlyAccent
                            else
                                FlowlyAccent.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "${pagerState.currentPage + 1} / ${filtered.size}",
            fontSize = 11.sp,
            color    = FlowlyMuted
        )
    }
}

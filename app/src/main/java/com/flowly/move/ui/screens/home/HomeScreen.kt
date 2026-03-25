package com.flowly.move.ui.screens.home

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.flowly.move.data.model.NIVEL_LIMITES
import com.flowly.move.data.model.TODAS_LAS_INSIGNIAS
import com.flowly.move.data.model.VideoQuestion
import com.flowly.move.data.repository.FlowlyRepository
import com.flowly.move.ui.screens.misiones.getMisionesDelDia
import com.flowly.move.ui.screens.store.StoreViewModel
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

@Composable
fun HomeScreen(navController: NavController) {
    val vm: UserViewModel   = viewModel()
    val storeVm: StoreViewModel = viewModel()
    val user              by vm.user.collectAsStateWithLifecycle()
    val storeConfig       by storeVm.storeConfig.collectAsStateWithLifecycle()
    val isLoading         by vm.isLoading.collectAsStateWithLifecycle()
    val showWelcome       by vm.showWelcomeDialog.collectAsStateWithLifecycle()
    val pendingBadge      by vm.pendingBadge.collectAsStateWithLifecycle()
    val networkError      by vm.networkError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar snackbar si hay error de red
    LaunchedEffect(networkError) {
        if (!networkError.isNullOrBlank()) {
            snackbarHostState.showSnackbar(networkError!!)
            vm.dismissNetworkError()
        }
    }

    // ── Contrarreloj misiones ──────────────────────────────────────────────
    var misionesCountdown by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val cal      = java.util.Calendar.getInstance()
            val h        = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val m        = cal.get(java.util.Calendar.MINUTE)
            val s        = cal.get(java.util.Calendar.SECOND)
            val secsLeft = (23 - h) * 3600L + (59 - m) * 60L + (60 - s)
            misionesCountdown = "%02d:%02d:%02d".format(
                secsLeft / 3600, (secsLeft % 3600) / 60, secsLeft % 60
            )
            kotlinx.coroutines.delay(1000L)
        }
    }

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
    val videosHoy      = when {
        moveVideos <= 0 -> 0
        moveVideos <= FlowlyRepository.DAILY_LIMIT_VIDEOS -> (moveVideos / FlowlyRepository.VIDEO_REWARD_AMOUNT).coerceAtLeast(1)
        else -> 4 + ((moveVideos - FlowlyRepository.DAILY_LIMIT_VIDEOS) / FlowlyRepository.VIDEO_BONUS_AMOUNT)
    }
    val moveEnHolding  = user?.moveEnHolding ?: 0
    val youtubeUrl     = storeConfig?.youtubeUrl ?: ""
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

        Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Contenido desplazable — ocupa todo el espacio disponible
            Column(
                modifier = Modifier
                    .weight(1f)
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

            // Video destacado (solo si hay URL cargada desde el panel admin)
            if (youtubeUrl.isNotBlank()) {
                val quizQuestions       = storeConfig?.videoQuiz ?: emptyList()
                val quizVersion         = storeConfig?.videoQuizVersion ?: ""
                val userAnsweredVersion = user?.videoQuizAnsweredVersion ?: ""
                Spacer(Modifier.height(12.dp))
                YouTubeCard(
                    url                 = youtubeUrl,
                    quizQuestions       = quizQuestions,
                    quizVersion         = quizVersion,
                    userAnsweredVersion = userAnsweredVersion,
                    onQuizComplete      = { vm.completeVideoQuiz(quizVersion) },
                    modifier            = Modifier.padding(horizontal = 16.dp)
                )
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
                            if (misionesCountdown.isNotBlank()) {
                                Text(
                                    "⏱ Reinicio en $misionesCountdown",
                                    fontSize = 10.sp,
                                    color    = FlowlyMuted.copy(alpha = 0.7f)
                                )
                            }
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

            // Banner Fondo de Premios
            FondoPremiosBanner(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { navController.navigate(Routes.FONDO_PREMIOS) }
            )

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
                QuickAccessItem("🎯", "Misiones", modifier = Modifier.weight(1f)) {
                    navController.navigate(Routes.MISIONES)
                }
            }

                Spacer(Modifier.height(20.dp))
            } // fin columna scrolleable

        } // fin columna principal

        // Snackbar de error de red — se muestra sobre el contenido
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
        } // fin Box
    }
}

// ── Banner Fondo de Premios ────────────────────────────────────────

@Composable
private fun FondoPremiosBanner(modifier: Modifier = Modifier) {
    val meses = listOf(
        "enero","febrero","marzo","abril","mayo","junio",
        "julio","agosto","septiembre","octubre","noviembre","diciembre"
    )
    val mesNombre = meses[java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(androidx.compose.ui.graphics.Color(0xFF2A1A00), androidx.compose.ui.graphics.Color(0xFF0A2010))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(androidx.compose.ui.graphics.Color(0xFFF59E0B), FlowlyAccent)
                ),
                androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("💰", fontSize = 28.sp)
                Column {
                    Text(
                        "Fondo de Premios",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = androidx.compose.ui.graphics.Color(0xFFF59E0B)
                    )
                    Text(
                        "Competí por ARS reales · $mesNombre",
                        fontSize = 11.sp,
                        color    = FlowlyMuted
                    )
                }
            }
            Text("→", fontSize = 18.sp, color = FlowlyMuted)
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

// ── YouTube Card ───────────────────────────────────────────────────

private fun extractYouTubeVideoId(url: String): String? = try {
    val uri = android.net.Uri.parse(url)
    when {
        uri.host?.contains("youtu.be") == true -> uri.lastPathSegment
        else -> uri.getQueryParameter("v")
    }
} catch (e: Exception) { null }

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

@Composable
private fun YouTubeCard(
    url: String,
    quizQuestions: List<VideoQuestion>  = emptyList(),
    quizVersion: String                 = "",
    userAnsweredVersion: String         = "",
    onQuizComplete: () -> Unit          = {},
    modifier: Modifier                  = Modifier
) {
    val videoId = remember(url) { extractYouTubeVideoId(url) } ?: return

    // MutableState como objeto para capturarlo por referencia en lambdas de View
    val showPlayerState = remember { mutableStateOf(false) }
    var showPlayer by showPlayerState

    // Quiz: mostrar si hay preguntas, versión válida y el user no respondió esta versión
    val showQuizButton = quizVersion.isNotBlank()
        && quizQuestions.size == 3
        && userAnsweredVersion != quizVersion
    var showQuiz by remember { mutableStateOf(false) }

    val context      = androidx.compose.ui.platform.LocalContext.current
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    }

    // Audio focus: silencia música al abrir, la reanuda al cerrar
    DisposableEffect(showPlayer) {
        if (showPlayer) {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        onDispose { @Suppress("DEPRECATION") audioManager.abandonAudioFocus(null) }
    }

    // ── Overlay sobre android.R.id.content (Window principal de la Activity) ──
    // MIUI throttlea el SurfaceTexture del video si el WebView está en una
    // Window secundaria (Dialog). Agregar el overlay directamente al content
    // view garantiza que comparte el pipeline de hardware de la Window principal.
    if (showPlayer) {
        DisposableEffect(videoId) {
            val activity = context as? android.app.Activity
                ?: return@DisposableEffect onDispose {}
            val rootView = activity.findViewById<android.widget.FrameLayout>(android.R.id.content)
            val density  = context.resources.displayMetrics.density
            fun dp(v: Int) = (v * density).toInt()

            val embedHtml = """<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
<style>*{margin:0;padding:0}html,body,iframe{width:100%;height:100%;background:#000;border:none;overflow:hidden}</style>
</head><body>
<iframe src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1"
 allow="autoplay;fullscreen;accelerometer;encrypted-media;gyroscope;picture-in-picture"
 allowfullscreen></iframe>
</body></html>"""

            val webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled                = true
                    domStorageEnabled                = true
                    loadWithOverviewMode             = true
                    useWideViewPort                  = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode                 = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString                  =
                        "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView, request: android.webkit.WebResourceRequest
                    ): Boolean {
                        // Solo permitir la URL del embed — bloquear TODO lo demás
                        // (recomendaciones, canal, búsqueda, etc.)
                        return !request.url.toString().contains("/embed/")
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    // Bloquear apertura de links en nueva ventana (_blank, target="_top", etc.)
                    override fun onCreateWindow(
                        view: WebView, isDialog: Boolean,
                        isUserGesture: Boolean, resultMsg: android.os.Message?
                    ) = false
                }
                loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null
                )
            }

            // Botón ✕ nativo (sin pasar por Compose)
            val closeBtn = android.widget.TextView(context).apply {
                text      = "✕"
                textSize  = 17f
                gravity   = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.argb(180, 0, 0, 0))
                setOnClickListener { showPlayerState.value = false }
            }

            val overlay = android.widget.FrameLayout(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                addView(webView, android.widget.FrameLayout.LayoutParams(-1, -1))
                addView(closeBtn, android.widget.FrameLayout.LayoutParams(dp(40), dp(40)).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                    setMargins(0, dp(8), dp(8), 0)
                })
            }

            val screenW  = context.resources.displayMetrics.widthPixels
            val videoH   = screenW * 9 / 16
            rootView.addView(overlay, android.widget.FrameLayout.LayoutParams(-1, videoH).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            })

            onDispose {
                webView.stopLoading()
                webView.destroy()
                rootView.removeView(overlay)
            }
        }
    }

    // ── Thumbnail con botón ▶ (siempre visible en el scroll) ──────────────
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, FlowlyBorder, RoundedCornerShape(16.dp))
            .aspectRatio(16f / 9f)
    ) {
        AsyncImage(
            model              = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            contentDescription = "Video",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { showPlayer = true },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier         = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", fontSize = 22.sp, color = Color.Black)
            }
        }

        // Banner de quiz — aparece en la punta superior si hay preguntas disponibles
        if (showQuizButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FlowlyAccent)
                    .clickable { showQuiz = true }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    "🎯 ¡Respondé y ganá 250 MOVE!",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black
                )
            }
        }
    }

    // Dialog del quiz
    if (showQuiz) {
        VideoQuizDialog(
            questions  = quizQuestions,
            onComplete = { onQuizComplete(); showQuiz = false },
            onDismiss  = { showQuiz = false }
        )
    }
}

// ── Quiz del video ─────────────────────────────────────────────────

@Composable
private fun VideoQuizDialog(
    questions: List<VideoQuestion>,
    onComplete: () -> Unit,
    onDismiss:  () -> Unit
) {
    var step     by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var failed   by remember { mutableStateOf(false) }
    var done     by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FlowlyCard, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                // ── Éxito: todas correctas ─────────────────────────
                done -> {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("¡Perfecto!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Respondiste las 3 correctamente",
                        fontSize  = 14.sp, color = FlowlyMuted, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("+250 MOVE", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent)
                    Text("acreditados en tu cuenta", fontSize = 12.sp, color = FlowlyMuted)
                    Spacer(Modifier.height(22.dp))
                    FlowlyPrimaryButton(text = "¡Genial!", onClick = onDismiss)
                }

                // ── Error: respuesta incorrecta ────────────────────
                failed -> {
                    Text("❌", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Incorrecto", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FlowlyText)
                    Spacer(Modifier.height(8.dp))
                    val q = questions[step]
                    Text(
                        "La respuesta correcta era:\n\"${q.opciones.getOrElse(q.correcta) { "" }}\"",
                        fontSize  = 13.sp, color = FlowlyMuted,
                        textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    FlowlyOutlineButton(text = "Cerrar", onClick = onDismiss)
                }

                // ── Pregunta activa ────────────────────────────────
                else -> {
                    val q = questions[step]
                    Text(
                        "🎯  Pregunta ${step + 1} de ${questions.size}",
                        fontSize = 12.sp, color = FlowlyAccent, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        q.pregunta,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = FlowlyText, textAlign = TextAlign.Center, lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(18.dp))

                    q.opciones.forEachIndexed { i, opcion ->
                        val isSelected = selected == i
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) FlowlyAccent.copy(alpha = 0.12f)
                                    else FlowlyCard2
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) FlowlyAccent else FlowlyBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selected = i }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(
                                        if (isSelected) FlowlyAccent else FlowlyBorder,
                                        RoundedCornerShape(50)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected)
                                    Text("✓", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Text(opcion, fontSize = 14.sp, color = FlowlyText)
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    FlowlyPrimaryButton(
                        text    = if (step == questions.lastIndex) "Finalizar" else "Siguiente →",
                        enabled = selected >= 0,
                        onClick = {
                            val q2 = questions[step]
                            if (selected == q2.correcta) {
                                if (step == questions.lastIndex) {
                                    done = true
                                    onComplete()
                                } else {
                                    step++
                                    selected = -1
                                }
                            } else {
                                failed = true
                            }
                        }
                    )
                }
            }
        }
    }
}

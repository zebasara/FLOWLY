package com.flowly.move.ui.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flowly.move.data.model.iniciales
import com.flowly.move.services.TrackingController
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Carto Dark Matter — tiles oscuros estilo Uber
private val CARTO_DARK = XYTileSource(
    "CartoDarkMatter",
    1, 20, 256, ".png",
    arrayOf(
        "https://cartodb-basemaps-a.global.ssl.fastly.net/dark_all/",
        "https://cartodb-basemaps-b.global.ssl.fastly.net/dark_all/",
        "https://cartodb-basemaps-c.global.ssl.fastly.net/dark_all/"
    ),
    "© CartoDB © OpenStreetMap contributors"
)

@Composable
fun MapScreen(navController: NavController) {
    val vm: MapViewModel = viewModel()
    val stats          by vm.stats.collectAsStateWithLifecycle()
    val activeSessions by vm.activeSessions.collectAsStateWithLifecycle()
    val consentGiven   by vm.consentGiven.collectAsStateWithLifecycle()
    val currentUser    by vm.currentUser.collectAsStateWithLifecycle()
    val context         = LocalContext.current

    val currentUid = remember(currentUser) { currentUser?.uid ?: "" }

    // ── Permisos ──────────────────────────────────────────────────────────
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Timer de sesión ───────────────────────────────────────────────────
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(stats.isTracking) {
        if (stats.isTracking) {
            while (true) {
                val t0 = TrackingController.startTimeMs
                elapsedSeconds = if (t0 > 0L) (System.currentTimeMillis() - t0) / 1000L else 0L
                delay(1_000)
            }
        } else {
            elapsedSeconds = stats.durationSeconds
        }
    }

    // avatarBitmaps solo necesario para overlay propio — otros usuarios van en lista UI

    // ── Referencias al MapView y overlay ─────────────────────────────────
    val mapViewRef   = remember { mutableStateOf<MapView?>(null) }
    val myOverlayRef = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // ── Foto de perfil propia ─────────────────────────────────────────────
    var ownAvatarBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentUser?.profilePhotoUrl) {
        val photoUrl  = currentUser?.profilePhotoUrl ?: ""
        val iniciales = currentUser?.iniciales ?: "??"
        val base = if (photoUrl.isNotBlank()) {
            runCatching {
                val req = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .allowHardware(false)
                    .build()
                val result = coil.Coil.imageLoader(context).execute(req)
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) makeCircularBitmap(context, bmp) else null
            }.getOrNull()
        } else null
        ownAvatarBitmap = base ?: makeInitialsBitmap(context, iniciales)
    }

    // ── Sincronizar avatar → overlay cuando cualquiera de los dos cambie ─
    // Necesario porque el factory de AndroidView y el LaunchedEffect de carga
    // pueden terminar en cualquier orden; este efecto garantiza que el ícono
    // se aplique siempre que ambos estén listos.
    LaunchedEffect(ownAvatarBitmap, myOverlayRef.value) {
        val bmp     = ownAvatarBitmap ?: return@LaunchedEffect
        val overlay = myOverlayRef.value ?: return@LaunchedEffect
        overlay.setPersonIcon(bmp)
        overlay.setDirectionArrow(bmp, bmp) // mismo bitmap → nunca muestra el triángulo
        mapViewRef.value?.invalidate()
    }

    DisposableEffect(Unit) {
        onDispose { mapViewRef.value?.onDetach() }
    }

    // ── Dialog de consentimiento ──────────────────────────────────────────
    var showConsentDialog by remember { mutableStateOf(false) }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            containerColor   = FlowlyCard2,
            titleContentColor = FlowlyText,
            textContentColor  = FlowlyMuted,
            title = { Text("📍 Activar MOVErme", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Mientras MOVErme esté activo, tu ubicación será usada para registrar " +
                    "tu actividad y acreditarte puntos MOVE.\n\n" +
                    "Tu ubicación no es compartida con otros usuarios. Solo se muestra quién está activo en este momento.\n\n" +
                    "Al detener la sesión, el registro se detiene automáticamente.\n\n" +
                    "Podés revisar nuestra Política de Privacidad en flowly.app/privacidad",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.acceptLocationConsent()
                        showConsentDialog = false
                        vm.startTracking(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowlyAccent)
                ) { Text("Acepto", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("No, gracias", color = FlowlyMuted)
                }
            }
        )
    }

    // ── UI principal ──────────────────────────────────────────────────────
    FlowlyScaffold(navController = navController, currentRoute = Routes.MAP) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasLocationPermission) {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📍", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "MOVE necesita acceso a tu ubicación para registrar tu actividad y acreditarte MOVE.",
                        fontSize   = 14.sp,
                        color      = FlowlyMuted,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    FlowlyPrimaryButton(
                        text    = "Permitir ubicación",
                        onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                    )
                }
                return@FlowlyScaffold
            }

            // ── Mapa OSMDroid (Dark Matter) ───────────────────────────────
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { ctx ->
                    Configuration.getInstance().apply {
                        userAgentValue    = ctx.packageName
                        osmdroidTileCache = ctx.cacheDir
                        // Cache generoso para evitar re-descargar tiles
                        tileDownloadThreads       = 4
                        tileDownloadMaxQueueSize  = 40
                        expirationOverrideDuration = 1000L * 60 * 60 * 24 * 7 // 7 días
                    }
                    MapView(ctx).also { map ->
                        mapViewRef.value = map
                        map.setTileSource(CARTO_DARK)
                        map.setMultiTouchControls(true)

                        // ── Fix cuadros blancos ──────────────────────────────
                        // Pintamos el fondo y los tiles en carga con el mismo
                        // color oscuro de la app para que no se vean cuadros blancos
                        val darkBg = 0xFF0A120A.toInt()
                        map.setBackgroundColor(darkBg)
                        map.overlayManager.tilesOverlay.apply {
                            loadingBackgroundColor = darkBg
                            loadingLineColor       = darkBg
                        }

                        // Zoom inicial moderado centrado en Argentina (no en una ciudad)
                        // La overlay MyLocation hará zoom/follow cuando obtenga el GPS
                        map.controller.setZoom(5.5)
                        map.controller.setCenter(GeoPoint(-38.4, -63.6)) // centro geográfico AR

                        val myLocationOverlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(ctx), map
                        ).apply {
                            enableMyLocation()
                            // runOnFirstFix: hace zoom al usuario cuando se obtiene el primer fix GPS
                            runOnFirstFix {
                                ctx.mainExecutor.execute {
                                    map.controller.setZoom(16.0)
                                    myLocation?.let { map.controller.setCenter(it) }
                                }
                            }
                            ownAvatarBitmap?.let {
                                setPersonIcon(it)
                                setDirectionArrow(it, it) // reemplaza el triángulo cuando se mueve
                            }
                        }
                        myOverlayRef.value = myLocationOverlay
                        map.overlays.add(myLocationOverlay)
                    }
                },
                update = { mapView ->
                    ownAvatarBitmap?.let {
                        myOverlayRef.value?.setPersonIcon(it)
                        myOverlayRef.value?.setDirectionArrow(it, it)
                    }
                    mapView.invalidate()
                }
            )

            // ── Badge de sesión activa (top-right) ────────────────────────
            if (stats.isTracking) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .background(Color(0xCC1A1A1A), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(FlowlyDanger, CircleShape)
                    )
                    Text("EN VIVO", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            // ── Banner de última sesión (top-left) ────────────────────────
            if (!stats.isTracking && stats.distanceMeters > 0f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 12.dp, start = 12.dp)
                        .background(Color(0xCC1A1A1A), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🏁", fontSize = 12.sp)
                    Text(
                        "%.2f km · %s".format(
                            stats.distanceMeters / 1000f,
                            formatDuration(elapsedSeconds)
                        ),
                        fontSize = 12.sp,
                        color    = FlowlyAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Lista de usuarios online (top-right, siempre visible) ────────
            val otrosSesiones = activeSessions.filter { it.uid != currentUid }
            val topPad        = if (stats.isTracking) 50.dp else 12.dp
            val halfScreen    = (LocalConfiguration.current.screenHeightDp / 2).dp

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = topPad, end = 10.dp)
                    .background(Color(0xBB0A120A), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .widthIn(min = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Contador — siempre visible
                Text(
                    "🌿 ${otrosSesiones.size}",
                    fontSize   = 10.sp,
                    color      = FlowlyAccent,
                    fontWeight = FontWeight.Bold
                )

                if (otrosSesiones.isEmpty()) {
                    // Estado vacío
                    Text(
                        "Sin otros\nonline",
                        fontSize   = 9.sp,
                        color      = FlowlyMuted,
                        textAlign  = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                } else {
                    // Lista scrolleable — crece hasta mitad de pantalla
                    Column(
                        modifier = Modifier
                            .heightIn(max = halfScreen - 60.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        otrosSesiones.forEach { sesion ->
                            if (sesion.profilePhotoUrl.isNotBlank()) {
                                AsyncImage(
                                    model              = sesion.profilePhotoUrl,
                                    contentDescription = sesion.nombre,
                                    contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier           = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, FlowlyAccent, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF182018))
                                        .border(1.5.dp, FlowlyAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        sesion.iniciales.take(2),
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = FlowlyAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Botón recentrar (bottom-right, sobre el sheet) ────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 200.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1C))
                    .clickable {
                        myOverlayRef.value?.enableFollowLocation()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("📍", fontSize = 18.sp)
            }

            // ── Bottom sheet oscuro ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        color = Color(0xF01C1C1C),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Handle pill
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFF444444), RoundedCornerShape(2.dp))
                )

                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MapStatItem(
                        icon  = "📏",
                        label = "Distancia",
                        value = "%.2f km".format(stats.distanceMeters / 1000f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color(0xFF333333))
                    )
                    MapStatItem(
                        icon  = "⏱",
                        label = "Tiempo",
                        value = formatDuration(elapsedSeconds)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color(0xFF333333))
                    )
                    MapStatItem(
                        icon  = "⚡",
                        label = "Velocidad",
                        value = "%.1f km/h".format(stats.speedKmh)
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (stats.isTracking) {
                    Button(
                        onClick  = { vm.stopTracking(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
                    ) {
                        Text(
                            "⏹  Detener MOVErme",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = FlowlyDanger
                        )
                    }
                } else {
                    Button(
                        onClick  = {
                            if (consentGiven) vm.startTracking(context)
                            else showConsentDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlowlyAccent)
                    ) {
                        Text(
                            "▶  Iniciar MOVErme",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.Black
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Helpers de avatar ──────────────────────────────────────────────────────

private fun makeInitialsBitmap(context: Context, iniciales: String): Bitmap {
    val size = 96
    val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    Paint(Paint.ANTI_ALIAS_FLAG).also { p ->
        p.color = 0xFF182018.toInt()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, p)
    }
    Paint(Paint.ANTI_ALIAS_FLAG).also { p ->
        p.color      = 0xFF7EE621.toInt()
        p.style      = Paint.Style.STROKE
        p.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, p)
    }
    Paint(Paint.ANTI_ALIAS_FLAG).also { p ->
        p.color        = 0xFF7EE621.toInt()
        p.textSize     = size * 0.35f
        p.textAlign    = Paint.Align.CENTER
        p.isFakeBoldText = true
        val fm   = p.fontMetrics
        val textY = size / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(iniciales.take(2), size / 2f, textY, p)
    }
    return bmp
}

private fun makeCircularBitmap(context: Context, source: Bitmap): Bitmap {
    val size   = 96
    val scaled = Bitmap.createScaledBitmap(source, size, size, true)
    val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val path = Path().apply {
        addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
    }
    canvas.clipPath(path)
    canvas.drawBitmap(scaled, 0f, 0f, clipPaint)
    Paint(Paint.ANTI_ALIAS_FLAG).also { p ->
        p.color       = 0xFF7EE621.toInt()
        p.style       = Paint.Style.STROKE
        p.strokeWidth  = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, p)
    }
    return bmp
}

@Composable
private fun MapStatItem(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Text(icon, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 10.sp, color = Color(0xFF888888))
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

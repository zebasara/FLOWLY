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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.request.ImageRequest
import com.flowly.move.services.TrackingController
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
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

    // ── Avatares de otros usuarios ────────────────────────────────────────
    val avatarBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(activeSessions) {
        val activeUids = activeSessions.map { it.uid }.toSet()
        activeSessions
            .filter { it.uid != currentUid && !avatarBitmaps.containsKey(it.uid) }
            .forEach { sesion ->
                avatarBitmaps[sesion.uid] = makeInitialsBitmap(context, sesion.iniciales)
                if (sesion.profilePhotoUrl.isNotBlank()) {
                    runCatching {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context)
                            .data(sesion.profilePhotoUrl)
                            .allowHardware(false)
                            .build()
                        val result = loader.execute(req)
                        val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bmp != null) avatarBitmaps[sesion.uid] = makeCircularBitmap(context, bmp)
                    }
                }
            }
        avatarBitmaps.keys.toList().forEach { uid ->
            if (uid !in activeUids) avatarBitmaps.remove(uid)
        }
    }

    // ── Foto de perfil propia ─────────────────────────────────────────────
    var ownAvatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val myOverlayRef    = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    LaunchedEffect(currentUser?.profilePhotoUrl) {
        val photoUrl = currentUser?.profilePhotoUrl ?: ""
        val iniciales = currentUser?.iniciales ?: "??"
        val base = if (photoUrl.isNotBlank()) {
            runCatching {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(req)
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) makeCircularBitmap(context, bmp) else null
            }.getOrNull()
        } else null
        ownAvatarBitmap = base ?: makeInitialsBitmap(context, iniciales)
        myOverlayRef.value?.setPersonIcon(ownAvatarBitmap)
    }

    // ── Referencias al MapView y markers ─────────────────────────────────
    val mapViewRef  = remember { mutableStateOf<MapView?>(null) }
    val userMarkers = remember { mutableMapOf<String, Marker>() }

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
            title = { Text("📍 Tu ubicación será visible", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Mientras MOVErme esté activo, tu nombre y posición en el mapa serán " +
                    "visibles para otros usuarios de MOVE.\n\n" +
                    "Al detener la sesión, tu ubicación desaparece del mapa.\n\n" +
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
                        userAgentValue = ctx.packageName
                        osmdroidTileCache = ctx.cacheDir
                    }
                    MapView(ctx).also { map ->
                        mapViewRef.value = map
                        map.setTileSource(CARTO_DARK)
                        map.setMultiTouchControls(true)
                        map.controller.setZoom(16.0)

                        val myLocationOverlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(ctx), map
                        ).apply {
                            enableMyLocation()
                            enableFollowLocation()
                            ownAvatarBitmap?.let { setPersonIcon(it) }
                        }
                        myOverlayRef.value = myLocationOverlay
                        map.overlays.add(myLocationOverlay)
                        map.controller.setCenter(GeoPoint(-34.6037, -58.3816))
                    }
                },
                update = { mapView ->
                    ownAvatarBitmap?.let { myOverlayRef.value?.setPersonIcon(it) }

                    val otherSessions = activeSessions.filter { it.uid != currentUid }
                    val activeUids    = otherSessions.map { it.uid }.toSet()

                    val staleUids = userMarkers.keys.filter { it !in activeUids }
                    staleUids.forEach { uid ->
                        userMarkers[uid]?.let { mapView.overlays.remove(it) }
                        userMarkers.remove(uid)
                    }

                    otherSessions.forEach { sesion ->
                        val avatar = avatarBitmaps[sesion.uid]
                            ?: makeInitialsBitmap(context, sesion.iniciales)
                        val geoPoint = GeoPoint(sesion.lat, sesion.lng)
                        val existing = userMarkers[sesion.uid]
                        if (existing != null) {
                            existing.position = geoPoint
                            existing.icon = BitmapDrawable(context.resources, avatar)
                        } else {
                            val marker = Marker(mapView).apply {
                                position = geoPoint
                                icon = BitmapDrawable(context.resources, avatar)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = sesion.nombre
                                snippet = "🌿 MOVErme activo"
                            }
                            mapView.overlays.add(marker)
                            userMarkers[sesion.uid] = marker
                        }
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

            // ── Contador usuarios activos (top-center) ────────────────────
            val otrosActivos = activeSessions.count { it.uid != currentUid }
            if (otrosActivos > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(Color(0xCC1A1A1A), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "🌿 $otrosActivos activo${if (otrosActivos > 1) "s" else ""} cerca",
                        fontSize   = 11.sp,
                        color      = FlowlyAccent,
                        fontWeight = FontWeight.SemiBold
                    )
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

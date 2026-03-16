package com.flowly.move.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(navController: NavController) {
    val vm: MapViewModel = viewModel()
    val stats   by vm.stats.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    // Estado de permisos
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    // Referencia al MapView para controlar el ciclo de vida
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef.value?.onDetach()
            if (stats.isTracking) vm.stopTracking(context)
        }
    }

    FlowlyScaffold(navController = navController, currentRoute = Routes.MAP) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasLocationPermission) {
                // Pantalla de solicitud de permiso
                Column(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📍", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Necesitamos acceso a tu ubicación para rastrear tu actividad.",
                        fontSize    = 14.sp,
                        color       = FlowlyMuted,
                        lineHeight  = 20.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    FlowlyPrimaryButton(
                        text    = "Permitir ubicación",
                        onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                    )
                }
                return@FlowlyScaffold
            }

            // Mapa OSMDroid
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { ctx ->
                    Configuration.getInstance().apply {
                        userAgentValue = ctx.packageName
                        osmdroidTileCache = ctx.cacheDir
                    }
                    MapView(ctx).also { map ->
                        mapViewRef.value = map
                        map.setTileSource(TileSourceFactory.MAPNIK)
                        map.setMultiTouchControls(true)
                        map.controller.setZoom(16.0)

                        // Overlay de "mi ubicación"
                        val myLocationOverlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(ctx), map
                        ).apply {
                            enableMyLocation()
                            enableFollowLocation()
                        }
                        map.overlays.add(myLocationOverlay)

                        // Centrar en Argentina por defecto
                        map.controller.setCenter(GeoPoint(-34.6037, -58.3816))
                    }
                },
                update = { /* El overlay GPS se actualiza solo */ }
            )

            // Panel de estadísticas (overlay)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Stats card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FlowlyCard.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Distancia",
                        value = "%.2f km".format(stats.distanceMeters / 1000f)
                    )
                    StatItem(
                        label = "Tiempo",
                        value = formatDuration(stats.durationSeconds)
                    )
                    StatItem(
                        label = "Velocidad",
                        value = "%.1f km/h".format(stats.speedKmh)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Botón inicio/parada
                if (stats.isTracking) {
                    FlowlySecondaryButton(
                        text    = "⏹ Detener rastreo",
                        onClick = { vm.stopTracking(context) }
                    )
                } else {
                    FlowlyPrimaryButton(
                        text    = "▶ Iniciar rastreo",
                        onClick = { vm.startTracking(context) }
                    )
                }
            }

            // Indicador de tracking activo
            if (stats.isTracking) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(FlowlyDanger.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        color    = Color.White,
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Rastreando", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent)
        Text(label, fontSize = 11.sp, color = FlowlyMuted)
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

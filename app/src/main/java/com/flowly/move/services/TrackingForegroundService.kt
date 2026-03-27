package com.flowly.move.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.flowly.move.FlowlyApp
import com.flowly.move.MainActivity
import com.flowly.move.R
import com.flowly.move.data.model.SesionActiva
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackingForegroundService : Service() {

    companion object {
        const val ACTION_START    = "ACTION_START"
        const val ACTION_STOP     = "ACTION_STOP"
        const val EXTRA_UID       = "extra_uid"
        const val EXTRA_NOMBRE    = "extra_nombre"
        const val EXTRA_PHOTO_URL = "extra_photo_url"
        private const val NOTIF_ID = 1001
        // Actualiza Firestore cada N updates de GPS (~16 seg con intervalo 2s)
        private const val FIRESTORE_INTERVAL = 8
        // Acredita MOVE cada X metros (= 1 MOVE por tramo)
        private const val CREDIT_INTERVAL_METERS = FlowlyRepository.METROS_POR_MOVE.toFloat()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var locationManager: LocationManager
    private lateinit var repo: FlowlyRepository

    private var lastLocation:   Location? = null
    private var totalDistance = 0f
    private var startTime     = 0L
    private var isRunning     = false

    // Datos del usuario — se reciben como Intent extras al iniciar
    private var currentUid      = ""
    private var currentNombre   = ""
    private var currentPhotoUrl = ""
    private var locationUpdateCount = 0

    // Velocidad actual: se actualiza con cada update GPS para el timer de notificación
    private var currentSpeedKmh = 0f

    // ── Crédito progresivo de MOVE ────────────────────────────────────────
    /** Metros ya acreditados en Firestore durante esta sesión */
    private var distanceCreditedMeters = 0f
    /** MOVE disponibles para acreditar hoy (leído al iniciar la sesión) */
    private var dailyLimitRestante     = FlowlyRepository.DAILY_LIMIT_MOVIMIENTO

    private val locationListener = LocationListener { location ->
        lastLocation?.let { prev -> totalDistance += prev.distanceTo(location) }
        lastLocation      = location
        currentSpeedKmh   = location.speed * 3.6f

        val durationSec = if (startTime > 0) (System.currentTimeMillis() - startTime) / 1000L else 0L

        // ── Crédito progresivo: acreditar cada CREDIT_INTERVAL_METERS ────────
        val uncredited = totalDistance - distanceCreditedMeters
        if (uncredited >= CREDIT_INTERVAL_METERS && dailyLimitRestante > 0 && currentUid.isNotBlank()) {
            val moves    = (uncredited / CREDIT_INTERVAL_METERS).toInt()
                .coerceAtMost(dailyLimitRestante)
            val credited = moves * CREDIT_INTERVAL_METERS
            distanceCreditedMeters += credited
            dailyLimitRestante     -= moves
            serviceScope.launch {
                repo.acreditarMoveProgresivo(currentUid, moves, credited / 1000f)
            }
        }

        TrackingController.update(
            TrackingStats(
                isTracking             = true,
                distanceMeters         = totalDistance,
                durationSeconds        = durationSec,
                speedKmh               = currentSpeedKmh,
                lastLat                = location.latitude,
                lastLng                = location.longitude,
                distanceCreditedMeters = distanceCreditedMeters
            )
        )

        // Publicar posición en Firestore cada FIRESTORE_INTERVAL actualizaciones
        locationUpdateCount++
        if (locationUpdateCount % FIRESTORE_INTERVAL == 0 && currentUid.isNotBlank()) {
            publishLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        repo = FlowlyRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentUid      = intent.getStringExtra(EXTRA_UID)       ?: ""
                currentNombre   = intent.getStringExtra(EXTRA_NOMBRE)    ?: ""
                currentPhotoUrl = intent.getStringExtra(EXTRA_PHOTO_URL) ?: ""
                startTracking()
            }
            ACTION_STOP -> stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        if (isRunning) return
        isRunning              = true
        startTime              = System.currentTimeMillis()
        totalDistance          = 0f
        lastLocation           = null
        locationUpdateCount    = 0
        currentSpeedKmh        = 0f
        distanceCreditedMeters = 0f
        dailyLimitRestante     = FlowlyRepository.DAILY_LIMIT_MOVIMIENTO
        TrackingController.markStart()

        // Leer cuántos MOVE ya ganó hoy para respetar el límite diario
        if (currentUid.isNotBlank()) {
            serviceScope.launch {
                val user = repo.getUser(currentUid).getOrNull()
                if (user != null) {
                    dailyLimitRestante = (FlowlyRepository.DAILY_LIMIT_MOVIMIENTO - user.tokenMovimientoHoy)
                        .coerceAtLeast(0)
                }
            }
        }

        // Publicar isTracking=true inmediatamente — el timer de MapScreen arranca ya
        TrackingController.update(
            TrackingStats(isTracking = true, distanceMeters = 0f, speedKmh = 0f)
        )

        startForeground(NOTIF_ID, buildNotification("📍 Buscando señal GPS…"))

        // ── Timer de notificación: actualiza cada segundo con datos reales ──────
        // Necesario porque la notificación solo se actualizaría al recibir GPS.
        // Así el tiempo siempre corre aunque no haya señal o el usuario esté quieto.
        serviceScope.launch {
            while (isRunning) {
                delay(1_000)
                if (isRunning) refreshNotification()
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2_000L,   // cada 2 segundos
                5f,       // o cada 5 metros
                locationListener
            )
        } catch (e: SecurityException) {
            stopTracking()
        }
    }

    private fun stopTracking() {
        isRunning = false
        locationManager.removeUpdates(locationListener)
        TrackingController.update(
            TrackingStats(
                isTracking      = false,
                distanceMeters  = totalDistance,
                durationSeconds = if (startTime > 0) (System.currentTimeMillis() - startTime) / 1000L else 0L,
                speedKmh        = 0f
            )
        )
        // Eliminar sesión activa de Firestore
        if (currentUid.isNotBlank()) {
            serviceScope.launch {
                repo.deleteSesionActiva(currentUid)
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishLocation(location: Location) {
        if (currentUid.isBlank()) return
        val sesion = SesionActiva(
            uid             = currentUid,
            nombre          = currentNombre,
            profilePhotoUrl = currentPhotoUrl,
            lat             = location.latitude,
            lng             = location.longitude,
            updatedAt       = System.currentTimeMillis()
        )
        serviceScope.launch {
            repo.upsertSesionActiva(currentUid, sesion)
        }
    }

    /**
     * Recalcula el texto de la notificación usando el tiempo real desde [startTime].
     * Se llama cada segundo desde el timer interno — los datos siempre están actualizados
     * incluso si el GPS no disparó todavía (usuario quieto, señal débil, etc.).
     */
    private fun refreshNotification() {
        val km         = totalDistance / 1000f
        val moveGanado = (distanceCreditedMeters / CREDIT_INTERVAL_METERS).toInt()
        val elapsedSec = if (startTime > 0L) (System.currentTimeMillis() - startTime) / 1000L else 0L
        val h          = elapsedSec / 3600
        val m          = (elapsedSec % 3600) / 60
        val s          = elapsedSec % 60
        val timeStr    = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        val moveStr    = if (moveGanado > 0) " · +$moveGanado MOVE ✅" else ""
        val text       = "%.2f km · %s · %.1f km/h%s".format(km, timeStr, currentSpeedKmh, moveStr)
        val nm         = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, FlowlyApp.CHANNEL_TRACKING)
            .setContentTitle("FLOWLY - MOVErme activo")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_move)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) stopTracking()
        serviceScope.cancel()
    }
}

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

class TrackingForegroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP  = "ACTION_STOP"
        private const val NOTIF_ID = 1001
    }

    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private var totalDistance = 0f
    private var startTime     = 0L
    private var isRunning     = false

    private val locationListener = LocationListener { location ->
        lastLocation?.let { prev ->
            totalDistance += prev.distanceTo(location)
        }
        lastLocation = location

        val durationSec = if (startTime > 0) (System.currentTimeMillis() - startTime) / 1000L else 0L
        val speedKmh    = location.speed * 3.6f   // m/s → km/h
        TrackingController.update(
            TrackingStats(
                isTracking      = true,
                distanceMeters  = totalDistance,
                durationSeconds = durationSec,
                speedKmh        = speedKmh
            )
        )
        updateNotification()
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP  -> stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        if (isRunning) return
        isRunning     = true
        startTime     = System.currentTimeMillis()
        totalDistance = 0f
        lastLocation  = null

        startForeground(NOTIF_ID, buildNotification("Iniciando rastreo…"))

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
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, FlowlyApp.CHANNEL_TRACKING)
            .setContentTitle("MOVE · Rastreo activo")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val stats  = TrackingController.stats.value
        val km     = stats.distanceMeters / 1000f
        val mins   = stats.durationSeconds / 60
        val text   = "%.2f km · %d min · %.1f km/h".format(km, mins, stats.speedKmh)
        val nm     = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) stopTracking()
    }
}

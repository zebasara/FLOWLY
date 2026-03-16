package com.flowly.move

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp

class FlowlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Inicializar AdMob
        MobileAds.initialize(this)
        // Canal de notificaciones para el servicio GPS
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            // Canal para el foreground service de rastreo.
            // IMPORTANCE_DEFAULT garantiza que el ícono aparezca en la status bar.
            // IMPORTANCE_LOW puede suprimir el ícono en algunos dispositivos/Android.
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TRACKING,
                    "Rastreo GPS activo",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "MOVE rastrea tu actividad física en segundo plano"
                    setSound(null, null)      // Sin sonido al aparecer
                    enableVibration(false)    // Sin vibración
                    setShowBadge(false)       // Sin badge en el ícono de la app
                }
            )
            // Canal para notificaciones generales de la app
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "Notificaciones MOVE",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Insignias, canjes y novedades de MOVE"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_TRACKING = "tracking_channel"
        const val CHANNEL_GENERAL  = "general_channel"
    }
}

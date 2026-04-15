package com.flowly.move

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.FirebaseApp
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class FlowlyApp : Application() {

    companion object {
        /** true cuando AppLovin MAX terminó su inicialización. */
        @Volatile var applovinReady = false
            private set

        /** true cuando Unity Ads terminó su inicialización. */
        @Volatile var unityAdsReady = false
            private set

        const val CHANNEL_TRACKING = "tracking_channel"
        const val CHANNEL_GENERAL  = "general_channel"
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        initCoil()
        // AdMob — inicializar en hilo secundario para no bloquear el primer frame
        Thread {
            if (BuildConfig.DEBUG) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder()
                        .setTestDeviceIds(listOf("D4CD646FEBDFD16F08459FEAD0EE09A1"))
                        .build()
                )
            }
            MobileAds.initialize(this)
        }.start()

        // ── AppLovin MAX — red principal ─────────────────────────────────────
        if (BuildConfig.USE_APPLOVIN) {
            AppLovinSdk.getInstance(this).apply {
                mediationProvider = AppLovinMediationProvider.MAX
                initializeSdk(AppLovinSdk.SdkInitializationListener {
                    applovinReady = true
                })
            }
        }

        // ── Pangle (TikTok Ads) — INACTIVO (requiere empresa registrada) ────
        // SDK no incluido. USE_PANGLE=false en secrets.properties.

        // ── Unity Ads — solo se inicializa cuando está activo ──────────────
        if (BuildConfig.USE_UNITY_ADS && BuildConfig.UNITY_GAME_ID.isNotBlank()) {
            UnityAds.initialize(
                this,
                BuildConfig.UNITY_GAME_ID,
                BuildConfig.DEBUG,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() { unityAdsReady = true }
                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) { /* fallo silencioso en producción */ }
                }
            )
        }

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

    // ── Coil — cache de 7 días para imágenes de Firebase Storage ─────────
    private fun initCoil() {
        val httpCache = Cache(File(cacheDir, "http_cache"), 50L * 1024 * 1024) // 50 MB HTTP

        val okHttp = OkHttpClient.Builder()
            .cache(httpCache)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                // Firebase Storage devuelve max-age=0 → lo sobreescribimos a 7 días
                val host = chain.request().url.host
                if (host.contains("firebasestorage") || host.contains("googleapis")) {
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=${7 * 24 * 60 * 60}")
                        .removeHeader("Pragma")
                        .build()
                } else {
                    response
                }
            }
            .build()

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(okHttp)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25) // 25% de la RAM disponible
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(cacheDir, "coil_disk"))
                        .maxSizeBytes(50L * 1024 * 1024) // 50 MB imágenes
                        .build()
                }
                .build()
        )
    }

}

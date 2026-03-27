package com.flowly.move

import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.flowly.move.ui.components.UpdateAvailableDialog
import com.flowly.move.ui.navigation.FlowlyNavGraph
import com.flowly.move.ui.theme.FlowlyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    // ── Música de fondo ───────────────────────────────────────────────────
    private var bgMusic:   MediaPlayer? = null
    private var audioMgr:  AudioManager? = null
    /** true = la música fue pausada por pérdida de foco (video), no por lifecycle */
    private var musicPausedByFocus = false

    /**
     * Listener de AudioFocus registrado junto con bgMusic.
     * Cuando otra app (o el video de YouTube en-app) toma el foco,
     * el sistema notifica aquí y pausamos/reanudamos la música en consecuencia.
     */
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Foco recuperado → reanudar música
                musicPausedByFocus = false
                bgMusic?.let { if (!it.isPlaying) it.start() }
                bgMusic?.setVolume(0.4f, 0.4f)
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Video u otra app tomó el foco → pausar música
                musicPausedByFocus = true
                bgMusic?.let { if (it.isPlaying) it.pause() }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Bajar volumen en vez de pausar (notificaciones, etc.)
                bgMusic?.setVolume(0.08f, 0.08f)
            }
        }
    }

    // ── Update via Firestore ──────────────────────────────────────────────
    private var showUpdateDialog  by mutableStateOf(false)
    private var updateVersionName  = ""
    private var updateMessage      = ""
    private var updateChecked      = false   // evita chequear múltiples veces

    // ── Auth listener ─────────────────────────────────────────────────────
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        android.util.Log.d("UpdateCheck", "AuthState: uid=${auth.currentUser?.uid} checked=$updateChecked")
        if (auth.currentUser != null && !updateChecked) {
            updateChecked = true
            checkForUpdates()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.parseColor("#0A120A"))
        )

        // Registrar listener ANTES de setContent — se dispara cuando auth esté listo
        FirebaseAuth.getInstance().addAuthStateListener(authListener)

        initBgMusic()

        setContent {
            FlowlyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    FlowlyNavGraph()

                    // Cartel de nueva versión disponible
                    if (showUpdateDialog) {
                        UpdateAvailableDialog(
                            versionName = updateVersionName,
                            message     = updateMessage,
                            onUpdate    = { openPlayStore() },
                            onDismiss   = { showUpdateDialog = false }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Solo reanudar si la música no fue pausada por el video (pérdida de foco)
        if (!musicPausedByFocus) {
            bgMusic?.let { if (!it.isPlaying) it.start() }
        }
    }

    override fun onPause() {
        super.onPause()
        bgMusic?.let { if (it.isPlaying) it.pause() }
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        @Suppress("DEPRECATION")
        audioMgr?.abandonAudioFocus(audioFocusListener)
        bgMusic?.stop()
        bgMusic?.release()
        bgMusic = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Compara BuildConfig.VERSION_CODE con config/appVersion.versionCode en Firestore.
     * Se llama DESPUÉS de que Firebase Auth confirma sesión, garantizando request.auth != null.
     *
     * Firestore → colección: config | documento: appVersion | campos:
     *   versionCode (int64)  — ej: 4
     *   versionName (string) — ej: "1.0.4"
     *   message     (string) — ej: "Mejoramos el sistema de puntos..."
     */
    private fun checkForUpdates() {
        android.util.Log.d("UpdateCheck", "Consultando config/appVersion... localCode=${BuildConfig.VERSION_CODE}")
        Firebase.firestore
            .collection("config")
            .document("appVersion")
            .get()
            .addOnSuccessListener { doc ->
                android.util.Log.d("UpdateCheck", "Doc existe=${doc.exists()} data=${doc.data}")
                if (!doc.exists()) return@addOnSuccessListener

                // Normalizamos las claves (trim) por si tienen espacios accidentales en Firestore
                val data = doc.data?.mapKeys { it.key.trim() } ?: return@addOnSuccessListener

                val remoteCode = (data["versionCode"] as? Long)?.toInt() ?: run {
                    android.util.Log.w("UpdateCheck", "Campo versionCode no encontrado. Claves: ${data.keys}")
                    return@addOnSuccessListener
                }
                val localCode = BuildConfig.VERSION_CODE
                android.util.Log.d("UpdateCheck", "remoteCode=$remoteCode localCode=$localCode → mostrarDialog=${remoteCode > localCode}")
                if (remoteCode > localCode) {
                    updateVersionName = (data["versionName"] as? String) ?: ""
                    updateMessage     = (data["message"]     as? String) ?: ""
                    showUpdateDialog  = true
                    android.util.Log.d("UpdateCheck", "¡Mostrando diálogo de actualización!")
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("UpdateCheck", "ERROR: ${e.javaClass.simpleName}: ${e.message}")
            }
    }

    /** Abre la app en Play Store para que el usuario actualice */
    private fun openPlayStore() {
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
        showUpdateDialog = false
    }

    /** Pausa la música por un video en-app (no por lifecycle de Activity). */
    fun pauseMusic() {
        musicPausedByFocus = true
        bgMusic?.let { if (it.isPlaying) it.pause() }
    }

    /** Reanuda la música después de cerrar el video en-app. */
    fun resumeMusic() {
        musicPausedByFocus = false
        bgMusic?.let { if (!it.isPlaying) it.start() }
    }

    /** Reproduce música de fondo en loop desde res/raw/startup.mp3 */
    private fun initBgMusic() {
        try {
            val soundId = resources.getIdentifier("startup", "raw", packageName)
            if (soundId == 0) return

            audioMgr = getSystemService(AUDIO_SERVICE) as AudioManager

            bgMusic = MediaPlayer.create(this, soundId)?.apply {
                isLooping = true
                setVolume(0.4f, 0.4f)
            } ?: return

            // Solicitar foco de audio registrando nuestro listener.
            // A partir de este momento el sistema nos avisará cuando otra
            // fuente (el video de YouTube, notificaciones, etc.) tome el foco.
            @Suppress("DEPRECATION")
            val granted = audioMgr?.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            if (granted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                bgMusic?.start()
            }
        } catch (_: Exception) { /* sin archivo de sonido, continúa sin música */ }
    }
}

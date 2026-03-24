package com.flowly.move.ui.screens.video

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.BuildConfig
import com.flowly.move.ui.components.FlowlyPrimaryButton
import com.flowly.move.ui.theme.*
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

@Composable
fun VideoScreen(navController: NavController) {
    val vm: VideoViewModel = viewModel()
    val uiState         by vm.uiState.collectAsStateWithLifecycle()
    val limiteAlcanzado by vm.limiteAlcanzado.collectAsStateWithLifecycle()
    val context          = LocalContext.current
    val recompensa       = if (limiteAlcanzado) 20 else 50

    var adLoaded    by remember { mutableStateOf(false) }
    var adLoading   by remember { mutableStateOf(true) }
    var adWatched   by remember { mutableStateOf(false) }
    var rewardedAd  by remember { mutableStateOf<RewardedAd?>(null) }

    val snackbar = remember { SnackbarHostState() }

    // En DEBUG: ID de prueba oficial de Google (siempre sirve ads).
    // En RELEASE: ID real de AdMob desde secrets.properties.
    val rewardedAdUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/5224354917"
    } else {
        BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
    }

    // Cargar el ad al entrar a la pantalla
    LaunchedEffect(Unit) {
        adLoading = true
        RewardedAd.load(
            context,
            rewardedAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    adLoaded   = true
                    adLoading  = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoaded  = false
                    adLoading = false
                }
            }
        )
    }

    // Configurar callback de fullscreen cuando el ad esté listo
    LaunchedEffect(rewardedAd) {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // El usuario cerró el ad sin terminar — no se acredita
                rewardedAd = null
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
            }
        }
    }

    // Reaccionar al resultado del cobro
    LaunchedEffect(uiState) {
        when (uiState) {
            is VideoUiState.Success -> {
                vm.reset()
                navController.popBackStack()
            }
            is VideoUiState.Error -> {
                snackbar.showSnackbar((uiState as VideoUiState.Error).msg)
                vm.reset()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Video box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF111111), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (adWatched) {
                    Text("✅", fontSize = 48.sp)
                } else if (adLoading) {
                    CircularProgressIndicator(color = FlowlyAccent)
                } else if (adLoaded) {
                    Text("▶", fontSize = 48.sp, color = Color(0xFF444444))
                    Text(
                        "Anuncio listo",
                        fontSize = 11.sp,
                        color    = Color(0xFF555555),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                } else {
                    // Ad no disponible — modo fallback
                    Text("📺", fontSize = 48.sp, color = Color(0xFF444444))
                    Text(
                        "Sin anuncio disponible",
                        fontSize = 11.sp,
                        color    = Color(0xFF444444),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Estado visual
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .border(3.dp, if (adWatched) FlowlySuccess else FlowlyAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (adWatched) "✓" else if (adLoading) "…" else "▶",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (adWatched) FlowlySuccess else FlowlyAccent
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                when {
                    adWatched  -> "¡Video completado!"
                    adLoading  -> "Cargando anuncio…"
                    adLoaded   -> "Tocá para ver el anuncio y ganar"
                    else       -> "Sin anuncio disponible hoy"
                },
                fontSize  = 13.sp,
                color     = Color(0xFF666666),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Caja de recompensa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111), RoundedCornerShape(10.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("vas a ganar", fontSize = 11.sp, color = Color(0xFF444444))
                    Text(
                        "+ $recompensa MOVE",
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = FlowlyAccent
                    )
                    if (limiteAlcanzado) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ya ganaste los 50 MOVE × 4 anuncios de hoy.\nCada anuncio extra suma 20 MOVE al fondo mensual.",
                            fontSize  = 11.sp,
                            color     = Color(0xFF555555),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Primeros 4 anuncios: 50 MOVE c/u · Después: 20 MOVE c/u",
                            fontSize  = 10.sp,
                            color     = Color(0xFF444444),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            when {
                uiState is VideoUiState.Loading -> {
                    CircularProgressIndicator(color = FlowlyAccent)
                }
                adWatched -> {
                    FlowlyPrimaryButton(
                        text    = "Cobrar $recompensa MOVE y volver",
                        onClick = { vm.cobrarRecompensa() }
                    )
                }
                adLoaded && !adLoading -> {
                    FlowlyPrimaryButton(
                        text  = "Ver anuncio y ganar $recompensa MOVE",
                        onClick = {
                            rewardedAd?.show(context as Activity) {
                                adWatched = true
                            }
                        }
                    )
                }
                adLoading -> {
                    FlowlyPrimaryButton(
                        text    = "Cargando…",
                        enabled = false,
                        onClick = {}
                    )
                }
                else -> {
                    // No hay ad disponible — volver sin recompensa
                    FlowlyPrimaryButton(
                        text    = "Volver (sin recompensa)",
                        onClick = { navController.popBackStack() }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

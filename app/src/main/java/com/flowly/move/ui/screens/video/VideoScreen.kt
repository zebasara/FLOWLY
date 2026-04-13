package com.flowly.move.ui.screens.video

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import com.flowly.move.FlowlyApp
import com.flowly.move.ui.components.AppLovinRewardedAd
import com.flowly.move.ui.components.PangleRewardedAd
import kotlinx.coroutines.delay
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
import com.flowly.move.ui.components.UnityRewardedAd
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
    // Cast seguro: si el contexto no es una Activity (caso extremo), no crashea
    val activity         = context as? ComponentActivity ?: return
    val recompensa       = if (limiteAlcanzado) 20 else 50

    var adLoaded  by remember { mutableStateOf(false) }
    var adLoading by remember { mutableStateOf(true) }
    var adWatched by remember { mutableStateOf(false) }

    // AppLovin MAX — activo cuando USE_APPLOVIN=true (red principal)
    val applovinAd = remember {
        AppLovinRewardedAd(BuildConfig.APPLOVIN_REWARDED_AD_UNIT_ID)
    }

    // Pangle — reservado (USE_PANGLE=true, requiere empresa)
    val pangleAd = remember { PangleRewardedAd(BuildConfig.PANGLE_REWARDED_AD_UNIT_ID) }

    // Unity — reservado (USE_UNITY_ADS=false, sin fill standalone)
    val unityAd = remember { UnityRewardedAd() }

    // AdMob — activo cuando todas las anteriores están en false (cuenta recuperada)
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }

    val snackbar = remember { SnackbarHostState() }

    // Guard: evita que callbacks de SDK actualicen estado tras dispose del composable
    var isScreenActive by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        onDispose { isScreenActive = false }
    }

    // Cargar el ad al entrar a la pantalla según el proveedor activo
    LaunchedEffect(Unit) {
        adLoading = true
        when {
            // ── AppLovin MAX ──────────────────────────────────────────────
            BuildConfig.USE_APPLOVIN -> {
                var waited = 0
                while (!FlowlyApp.applovinReady && waited < 5_000) {
                    delay(200); waited += 200
                }
                if (!isScreenActive) return@LaunchedEffect
                applovinAd.load(
                    activity = activity,
                    onLoaded = { if (isScreenActive) { adLoaded = true;  adLoading = false } },
                    onFailed = { if (isScreenActive) { adLoaded = false; adLoading = false } }
                )
            }
            // ── Pangle — STUB INACTIVO (USE_PANGLE=false siempre) ────────
            BuildConfig.USE_PANGLE -> {
                if (!isScreenActive) return@LaunchedEffect
                pangleAd.load(
                    context  = context,
                    onLoaded = { if (isScreenActive) { adLoaded = true;  adLoading = false } },
                    onFailed = { if (isScreenActive) { adLoaded = false; adLoading = false } }
                )
            }
            // ── Unity Ads ─────────────────────────────────────────────────
            BuildConfig.USE_UNITY_ADS -> {
                var waited = 0
                while (!FlowlyApp.unityAdsReady && waited < 5_000) {
                    delay(200); waited += 200
                }
                if (!isScreenActive) return@LaunchedEffect
                unityAd.load(
                    onLoaded = { if (isScreenActive) { adLoaded = true;  adLoading = false } },
                    onFailed = { if (isScreenActive) { adLoaded = false; adLoading = false } }
                )
            }
            // ── AdMob ─────────────────────────────────────────────────────
            else -> {
                val adUnitId = if (BuildConfig.DEBUG) {
                    "ca-app-pub-3940256099942544/5224354917"
                } else {
                    BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
                }
                RewardedAd.load(
                    context, adUnitId, AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            if (!isScreenActive) return
                            rewardedAd = ad; adLoaded = true; adLoading = false
                        }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (!isScreenActive) return
                            adLoaded = false; adLoading = false
                        }
                    }
                )
            }
        }
    }

    // Callback de fullscreen para AdMob
    LaunchedEffect(rewardedAd) {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (isScreenActive) rewardedAd = null
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                if (isScreenActive) rewardedAd = null
            }
        }
    }

    // Reaccionar al resultado del cobro: navegar PRIMERO, luego resetear
    LaunchedEffect(uiState) {
        when (uiState) {
            is VideoUiState.Success -> {
                navController.popBackStack()
                vm.reset()
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
            .background(FlowlyBg)
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
                    .background(FlowlyCard, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .border(1.dp, FlowlyBorder, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (adWatched) {
                    Text("✅", fontSize = 48.sp)
                    Text(
                        "¡Completado!",
                        fontSize = 11.sp,
                        color    = FlowlySuccess,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    )
                } else if (adLoading) {
                    CircularProgressIndicator(color = FlowlyAccent, strokeWidth = 2.5.dp)
                    Text(
                        "Cargando…",
                        fontSize = 11.sp,
                        color    = FlowlyMuted,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    )
                } else if (adLoaded) {
                    Text("▶", fontSize = 48.sp, color = FlowlyAccent)
                    Text(
                        "Anuncio listo",
                        fontSize = 11.sp,
                        color    = FlowlyMuted,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    )
                } else {
                    Text("📺", fontSize = 48.sp, color = FlowlyMuted)
                    Text(
                        "Sin anuncio disponible",
                        fontSize = 11.sp,
                        color    = FlowlyMuted,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(
                        2.5.dp,
                        if (adWatched) FlowlySuccess else FlowlyAccent,
                        CircleShape
                    )
                    .background(
                        if (adWatched) FlowlySuccess.copy(alpha = 0.1f)
                        else FlowlyAccentGlow,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (adWatched) "✓" else if (adLoading) "…" else "▶",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (adWatched) FlowlySuccess else FlowlyAccent
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                when {
                    adWatched -> "¡Video completado!"
                    adLoading -> "Cargando anuncio…"
                    adLoaded  -> "Tocá para ver el anuncio y ganar"
                    else      -> "Sin anuncio disponible hoy"
                },
                fontSize  = 14.sp,
                color     = FlowlyTextSub,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Caja de recompensa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FlowlyCard2, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .border(1.dp, FlowlyBorder, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "VAS A GANAR",
                        fontSize      = 10.sp,
                        fontWeight    = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color         = FlowlyMuted,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "+ $recompensa MOVE",
                        fontSize      = 32.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = FlowlyAccent,
                        letterSpacing = (-1).sp
                    )
                    if (limiteAlcanzado) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ya ganaste los 50 MOVE × 4 anuncios de hoy.\nCada anuncio extra suma 20 MOVE al fondo mensual.",
                            fontSize  = 12.sp,
                            color     = FlowlyMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Primeros 4 anuncios: 50 MOVE c/u · Después: 20 MOVE c/u",
                            fontSize  = 11.sp,
                            color     = FlowlyMuted,
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
                        text    = "Reclamar $recompensa MOVE y volver",
                        onClick = { vm.cobrarRecompensa() }
                    )
                }
                adLoaded && !adLoading -> {
                    FlowlyPrimaryButton(
                        text  = "Ver anuncio y ganar $recompensa MOVE",
                        onClick = {
                            when {
                                BuildConfig.USE_APPLOVIN -> applovinAd.show(
                                    onRewarded = { adWatched = true },
                                    onFailed   = { adLoaded = false }
                                )
                                BuildConfig.USE_PANGLE -> pangleAd.show(
                                    activity   = activity,
                                    onRewarded = { adWatched = true },
                                    onFailed   = { adLoaded = false }
                                )
                                BuildConfig.USE_UNITY_ADS -> unityAd.show(
                                    activity   = activity,
                                    onRewarded = { adWatched = true },
                                    onFailed   = { adLoaded = false }
                                )
                                else -> rewardedAd?.show(activity) { adWatched = true }
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

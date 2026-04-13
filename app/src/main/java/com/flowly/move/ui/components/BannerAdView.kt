package com.flowly.move.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.flowly.move.BuildConfig
import com.flowly.move.FlowlyApp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay

/**
 * Banner inteligente — prioridad controlada por secrets.properties:
 *   USE_APPLOVIN=true  → AppLovin MAX  (red principal)
 *   USE_UNITY_ADS=true → Unity Ads     (reservado, sin fill standalone)
 *   else               → AdMob         (fallback siempre disponible)
 *
 * Para cambiar de AppLovin a AdMob: poner USE_APPLOVIN=false en secrets.properties
 */
@Composable
fun FlowlyBannerAd(modifier: Modifier = Modifier) {
    when {
        BuildConfig.USE_APPLOVIN  -> AppLovinBannerAd(modifier = modifier)
        BuildConfig.USE_PANGLE    -> PangleBannerAd(modifier = modifier)
        BuildConfig.USE_UNITY_ADS -> AdMobBannerAd(modifier = modifier) // Unity sin banner, fallback a AdMob
        else                      -> AdMobBannerAd(modifier = modifier)
    }
}

// ── AppLovin MAX Banner ───────────────────────────────────────────────────────

@Composable
private fun AppLovinBannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }

    val adView = remember {
        MaxAdView(BuildConfig.APPLOVIN_BANNER_AD_UNIT_ID, context).apply {
            setListener(object : MaxAdViewAdListener {
                override fun onAdLoaded(ad: MaxAd)  { isLoaded = true }
                override fun onAdLoadFailed(adUnitId: String, error: MaxError) { /* sin fill */ }
                override fun onAdDisplayed(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd)    {}
                override fun onAdClicked(ad: MaxAd)   {}
                override fun onAdExpanded(ad: MaxAd)  {}
                override fun onAdCollapsed(ad: MaxAd) {}
                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {}
            })
        }
    }

    // Esperar hasta 5s a que AppLovin inicialice, luego cargar el banner
    LaunchedEffect(Unit) {
        var waited = 0
        while (!FlowlyApp.applovinReady && waited < 5_000) {
            delay(200); waited += 200
        }
        adView.loadAd()
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    if (isLoaded) {
        AndroidView(
            factory  = { adView },
            modifier = modifier.fillMaxWidth().height(50.dp)
        )
    } else {
        Box(modifier = modifier.fillMaxWidth().height(50.dp))
    }
}

// ── Pangle — STUB INACTIVO ────────────────────────────────────────────────────

/**
 * Banner de Pangle (TikTok Ads) — STUB INACTIVO.
 * USE_PANGLE=false → nunca se llama. El SDK no está incluido.
 */
@Composable
private fun PangleBannerAd(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(50.dp))
}

// ── AdMob Banner (fallback) ───────────────────────────────────────────────────

/** Banner de AdMob — fallback cuando USE_APPLOVIN=false */
@Composable
private fun AdMobBannerAd(modifier: Modifier = Modifier) {
    val adUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111" // ID de prueba oficial
    } else {
        BuildConfig.ADMOB_BANNER_AD_UNIT_ID
            .takeIf { it.isNotBlank() }
            ?: "ca-app-pub-3940256099942544/6300978111"
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

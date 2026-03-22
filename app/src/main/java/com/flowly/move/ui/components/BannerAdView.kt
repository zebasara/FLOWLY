package com.flowly.move.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.flowly.move.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner de AdMob reutilizable.
 * En DEBUG usa el ID de prueba oficial de Google (siempre sirve ads).
 * En RELEASE usa el ID real configurado en secrets.properties.
 */
@Composable
fun FlowlyBannerAd(modifier: Modifier = Modifier) {
    // IDs de prueba oficiales de Google (garantizados en DEBUG):
    //   Banner:   ca-app-pub-3940256099942544/6300978111
    //   Rewarded: ca-app-pub-3940256099942544/5224354917
    val adUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        BuildConfig.ADMOB_BANNER_AD_UNIT_ID
            .takeIf { it.isNotBlank() }
            ?: "ca-app-pub-3940256099942544/6300978111"
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

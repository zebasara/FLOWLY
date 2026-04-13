package com.flowly.move.ui.screens.referrals

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.screens.home.UserViewModel
import com.flowly.move.ui.screens.store.StoreViewModel
import com.flowly.move.ui.theme.*

@Composable
fun ReferralsScreen(navController: NavController) {
    val userVm: UserViewModel   = viewModel()
    val storeVm: StoreViewModel = viewModel()

    val user        by userVm.user.collectAsStateWithLifecycle()
    val isLoading   by userVm.isLoading.collectAsStateWithLifecycle()
    val storeConfig by storeVm.storeConfig.collectAsStateWithLifecycle()

    val context          = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // URL base configurable desde el panel admin (sin barra final)
    val baseUrl      = storeConfig?.referralBaseUrl?.trimEnd('/') ?: "https://flowly.app/r"
    val userCode     = user?.uid?.take(8) ?: ""
    val referralLink = if (userCode.isNotBlank()) "$baseUrl/$userCode" else ""

    val amigosRegistrados = user?.totalReferidos ?: 0
    val moveGanados       = amigosRegistrados * 200

    // ── Helpers de sharing ─────────────────────────────────────────────────

    fun shareViaWhatsApp() {
        if (referralLink.isBlank()) return
        val texto = "¡Sumate a Flowly y acumulá MOVE juntos! Descargá la app y registrate con mi link: $referralLink"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type    = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
            setPackage("com.whatsapp")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // WhatsApp no instalado → share genérico
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, texto)
                    },
                    "Compartir por…"
                )
            )
        }
    }

    fun shareViaInstagram() {
        if (referralLink.isBlank()) return
        // Instagram no tiene un intent estándar para texto/links.
        // Copiamos el link y abrimos la app para que el usuario lo pegue.
        clipboardManager.setText(AnnotatedString(referralLink))
        val instagramIntent = context.packageManager
            .getLaunchIntentForPackage("com.instagram.android")
        if (instagramIntent != null) {
            context.startActivity(instagramIntent)
        } else {
            context.startActivity(
                Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.instagram.android"))
            )
        }
    }

    // ── UI ─────────────────────────────────────────────────────────────────

    FlowlyScaffold(navController = navController, currentRoute = Routes.HOME) { padding ->
        if (isLoading) {
            Box(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentAlignment    = Alignment.Center
            ) { CircularProgressIndicator(color = FlowlyAccent) }
            return@FlowlyScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Referir amigos",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyText,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Hero card
            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👥", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Invitá amigos y ganás",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = FlowlyText,
                        textAlign  = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cada amigo que se registre con tu link te da 200 MOVE. A ellos también les damos 100 MOVE.",
                        fontSize   = 13.sp,
                        color      = FlowlyMuted,
                        textAlign  = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SectionTitle(
                text     = "Tu link de referido",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Link box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(FlowlyCard2, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (referralLink.isNotBlank()) referralLink else "Cargando…",
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 12.sp,
                    color      = FlowlyAccent,
                    modifier   = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(FlowlyAccent, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (referralLink.isNotBlank())
                                clipboardManager.setText(AnnotatedString(referralLink))
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FlowlyBg)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Compartir
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowlySecondaryButton(
                    text     = "📱 WhatsApp",
                    onClick  = { shareViaWhatsApp() },
                    modifier = Modifier.weight(1f)
                )
                FlowlySecondaryButton(
                    text     = "📸 Instagram",
                    onClick  = { shareViaInstagram() },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "💡 Al tocar Instagram el link se copia automáticamente — pegalo en tu bio o historia.",
                fontSize   = 11.sp,
                color      = FlowlyMuted,
                lineHeight = 16.sp,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Stats
            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Amigos registrados", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyText)
                    Text("$amigosRegistrados",  fontSize = 16.sp, fontWeight = FontWeight.Bold,   color = FlowlyAccent)
                }
                FlowlySeparator()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MOVE ganados", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyText)
                    Text("%,d".format(moveGanados), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

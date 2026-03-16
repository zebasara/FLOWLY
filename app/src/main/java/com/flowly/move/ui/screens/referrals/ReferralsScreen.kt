package com.flowly.move.ui.screens.referrals

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
import com.flowly.move.ui.theme.*

@Composable
fun ReferralsScreen(navController: NavController) {
    val vm: UserViewModel = viewModel()
    val user      by vm.user.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current

    // Link basado en UID real (primeros 8 chars para ser legible)
    val referralLink      = if (user != null) "flowly.app/r/${user!!.uid.take(8)}" else ""
    val amigosRegistrados = user?.totalReferidos ?: 0
    val moveGanados       = amigosRegistrados * 200

    FlowlyScaffold(navController = navController, currentRoute = Routes.HOME) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FlowlyAccent)
            }
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
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = FlowlyText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Hero
            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👥", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Invitá amigos y ganás",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlowlyText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cada amigo que se registre con tu link te da 200 MOVE. A ellos también les damos 100 MOVE.",
                        fontSize = 13.sp,
                        color = FlowlyMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "TU LINK DE REFERIDO",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4B6B4B),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(FlowlyCard2, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    referralLink,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = FlowlyAccent,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(FlowlyAccent, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (referralLink.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(referralLink))
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FlowlyBg)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowlySecondaryButton(
                    text     = "📱 WhatsApp",
                    onClick  = { /* TODO: Intent compartir */ },
                    modifier = Modifier.weight(1f)
                )
                FlowlySecondaryButton(
                    text     = "📸 Instagram",
                    onClick  = { /* TODO: Intent compartir */ },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Amigos registrados", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FlowlyText)
                    Text("$amigosRegistrados", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent)
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

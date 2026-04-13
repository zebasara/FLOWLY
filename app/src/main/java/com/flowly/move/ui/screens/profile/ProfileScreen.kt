package com.flowly.move.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.flowly.move.data.model.Insignia
import com.flowly.move.data.model.NIVEL_LIMITES
import com.flowly.move.data.model.TODAS_LAS_INSIGNIAS
import com.flowly.move.ui.screens.home.UserViewModel
import com.flowly.move.ui.screens.store.StoreViewModel
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

@Composable
fun ProfileScreen(navController: NavController) {
    val vm: UserViewModel       = viewModel()
    val storeVm: StoreViewModel = viewModel()
    val user        by vm.user.collectAsStateWithLifecycle()
    val isLoading   by vm.isLoading.collectAsStateWithLifecycle()
    val storeConfig by storeVm.storeConfig.collectAsStateWithLifecycle()

    val context          = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val nombre        = user?.nombre ?: ""
    val ciudad        = listOfNotNull(
        user?.ciudad?.takeIf { it.isNotBlank() },
        user?.provincia?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
    val iniciales     = nombre.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    val email         = user?.email ?: ""
    val telefono      = user?.telefono ?: ""
    val aliasMP       = user?.aliasMercadoPago ?: ""
    val isAdmin       = user?.isAdmin ?: false
    val tokensLibres  = user?.tokensActuales ?: 0
    val nivel         = user?.nivel ?: 1
    val limiteTokens  = NIVEL_LIMITES.getOrElse(nivel - 1) { NIVEL_LIMITES.first() }
    val moveEnHolding = user?.moveEnHolding ?: 0
    val rachaDias     = user?.diasConsecutivosVideos ?: 0
    val photoUrl      = user?.profilePhotoUrl ?: ""
    val badges        = user?.badges ?: emptyList()

    // Link de referido — igual que en ReferralsScreen
    val baseUrl      = storeConfig?.referralBaseUrl?.trimEnd('/') ?: "https://flowly.app/r"
    val userCode     = user?.uid?.take(8) ?: ""
    val referralLink = if (userCode.isNotBlank()) "$baseUrl/$userCode" else ""

    // Insignia seleccionada para expandir
    var selectedBadge by remember { mutableStateOf<Insignia?>(null) }

    // ── Helpers de sharing ──────────────────────────────────────────────────
    fun shareViaWhatsApp(badgeTitulo: String, emoji: String) {
        val texto = "$emoji Gané la insignia \"$badgeTitulo\" en MOVE, ¡la app que te paga por moverte!" +
                if (referralLink.isNotBlank()) " Bajate gratis: $referralLink" else ""
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
            setPackage("com.whatsapp")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
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

    fun shareViaInstagram(badgeTitulo: String, emoji: String) {
        val texto = "$emoji Gané la insignia \"$badgeTitulo\" en MOVE." +
                if (referralLink.isNotBlank()) " ¡Bajate la app: $referralLink" else ""
        clipboardManager.setText(AnnotatedString(texto))
        val instagramIntent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
        if (instagramIntent != null) {
            context.startActivity(instagramIntent)
        } else {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.instagram.android"))
            )
        }
    }

    // ── Diálogo de insignia expandida ──────────────────────────────────────
    selectedBadge?.let { insignia ->
        BadgeExpandDialog(
            insignia     = insignia,
            referralLink = referralLink,
            onShareWA    = { shareViaWhatsApp(insignia.titulo, insignia.emoji) },
            onShareIG    = { shareViaInstagram(insignia.titulo, insignia.emoji) },
            onDismiss    = { selectedBadge = null }
        )
    }

    FlowlyScaffold(navController = navController, currentRoute = Routes.PROFILE, showBanner = false) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                "Mi perfil",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyText,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )

            // ── Tarjeta de perfil ────────────────────────────────────────────
            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, FlowlyAccent, CircleShape)
                            .background(FlowlyCard2),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model              = photoUrl,
                                contentDescription = "Foto de perfil",
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop
                            )
                        } else {
                            Text(iniciales, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(nombre, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlowlyText)
                    if (ciudad.isNotBlank())
                        Text(ciudad, fontSize = 12.sp, color = FlowlyMuted, modifier = Modifier.padding(top = 4.dp))

                    FlowlySeparator()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%,d".format(tokensLibres), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlowlyAccent)
                            Text("MOVE libres", fontSize = 11.sp, color = FlowlyMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nivel $nivel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlowlyAccent2)
                            Text("%,d límite".format(limiteTokens), fontSize = 11.sp, color = FlowlyMuted)
                        }
                    }

                    FlowlySeparator()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%,d".format(moveEnHolding), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlowlyAccent2)
                            Text("en holding", fontSize = 11.sp, color = FlowlyMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$rachaDias/5", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlowlyAccent)
                            Text("días racha", fontSize = 11.sp, color = FlowlyMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Mis datos ────────────────────────────────────────────────────
            SectionLabel("mis datos", modifier = Modifier.padding(horizontal = 16.dp))

            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                DataRow("Email",              email)
                FlowlySeparator()
                DataRow("Teléfono",           telefono.ifBlank { "No configurado" })
                FlowlySeparator()
                DataRow("Alias", aliasMP.ifBlank { "No configurado" }, valueColor = FlowlyAccent)
            }

            Spacer(Modifier.height(8.dp))

            // ── Insignias ────────────────────────────────────────────────────
            SectionLabel(
                if (badges.isEmpty()) "insignias" else "insignias (${badges.size})",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (badges.isEmpty()) {
                FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Aún no tenés insignias. ¡Completá actividades para ganarlas!",
                        fontSize = 12.sp,
                        color    = FlowlyMuted
                    )
                }
            } else {
                // Grid 4 columnas — todas las insignias, cada una clickeable
                val rows = badges.chunked(4)
                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { badgeId ->
                                val insignia = TODAS_LAS_INSIGNIAS[badgeId]
                                if (insignia != null) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(FlowlyCard2, RoundedCornerShape(12.dp))
                                            .border(1.dp, FlowlyBorder, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedBadge = insignia }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(insignia.emoji, fontSize = 24.sp)
                                        Text(
                                            insignia.titulo,
                                            fontSize  = 9.sp,
                                            color     = FlowlyMuted,
                                            textAlign = TextAlign.Center,
                                            maxLines  = 2,
                                            lineHeight = 12.sp
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            // Relleno para última fila incompleta
                            repeat(4 - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Botón Admin (solo visible si isAdmin = true en Firestore) ────
            if (isAdmin) {
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            FlowlyCard2,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            FlowlyAccent.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Routes.ADMIN_CANJES) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("⚙️", fontSize = 20.sp)
                        Column {
                            Text(
                                "Panel de administrador",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = FlowlyAccent
                            )
                            Text(
                                "Recompensas · configuración",
                                fontSize = 11.sp,
                                color    = FlowlyMuted
                            )
                        }
                    }
                    Text("›", fontSize = 18.sp, color = FlowlyAccent)
                }

                Spacer(Modifier.height(8.dp))

                // Admin · Video & Quiz
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(FlowlyCard2, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .border(1.dp, FlowlyAccent.copy(alpha = 0.4f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Routes.ADMIN_STORE) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🎬", fontSize = 20.sp)
                        Column {
                            Text(
                                "Panel de administrador",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = FlowlyAccent
                            )
                            Text(
                                "Video · Quiz de preguntas",
                                fontSize = 11.sp,
                                color    = FlowlyMuted
                            )
                        }
                    }
                    Text("›", fontSize = 18.sp, color = FlowlyAccent)
                }
            }

            Spacer(Modifier.height(8.dp))

            FlowlySecondaryButton(
                text     = "Editar perfil",
                onClick  = { navController.navigate(Routes.EDIT_PROFILE) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            FlowlySecondaryButton(
                text      = "Cerrar sesión",
                onClick   = {
                    vm.signOut {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                textColor = FlowlyDanger,
                modifier  = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Diálogo de insignia expandida ────────────────────────────────────────────

@Composable
private fun BadgeExpandDialog(
    insignia    : Insignia,
    referralLink: String,
    onShareWA   : () -> Unit,
    onShareIG   : () -> Unit,
    onDismiss   : () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(FlowlyCard, RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji grande dentro de círculo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(FlowlyCard2, CircleShape)
                    .border(2.dp, FlowlyAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(insignia.emoji, fontSize = 48.sp)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "¡Insignia desbloqueada!",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = FlowlyAccent,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                insignia.titulo,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyText,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                insignia.descripcion,
                fontSize   = 13.sp,
                color      = FlowlyMuted,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Sección de compartir — solo si hay link de referido
            if (referralLink.isNotBlank()) {
                Spacer(Modifier.height(20.dp))

                HorizontalDivider(color = FlowlyBorder)

                Spacer(Modifier.height(14.dp))

                Text(
                    "Compartí tu logro e invitá amigos",
                    fontSize  = 11.sp,
                    color     = FlowlyMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                // Columna para que los botones no se corten en ningún tamaño de pantalla
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowlySecondaryButton(
                        text    = "📱 Compartir por WhatsApp",
                        onClick = onShareWA
                    )
                    FlowlySecondaryButton(
                        text    = "📸 Compartir por Instagram",
                        onClick = onShareIG
                    )
                }

                Text(
                    "💡 Instagram copia el texto al portapapeles automáticamente.",
                    fontSize   = 10.sp,
                    color      = FlowlyMuted,
                    textAlign  = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier   = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            FlowlyPrimaryButton(text = "Cerrar", onClick = onDismiss)
        }
    }
}

// ── Helpers privados ─────────────────────────────────────────────────────────

@Composable
private fun DataRow(label: String, value: String, valueColor: Color = FlowlyMuted) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = FlowlyMuted)
        Text(value, fontSize = 12.sp, color = valueColor)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.Bold,
        color      = FlowlyTextSub,
        modifier   = modifier.padding(top = 20.dp, bottom = 10.dp)
    )
}

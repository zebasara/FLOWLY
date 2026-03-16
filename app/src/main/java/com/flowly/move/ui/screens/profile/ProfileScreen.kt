package com.flowly.move.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.flowly.move.data.model.NIVEL_LIMITES
import com.flowly.move.data.model.TODAS_LAS_INSIGNIAS
import com.flowly.move.ui.screens.home.UserViewModel
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

@Composable
fun ProfileScreen(navController: NavController) {
    val vm: UserViewModel = viewModel()
    val user      by vm.user.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

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
    val tokensLibres  = user?.tokensActuales ?: 0
    val nivel         = user?.nivel ?: 1
    val limiteTokens  = NIVEL_LIMITES.getOrElse(nivel - 1) { NIVEL_LIMITES.first() }
    val moveEnHolding = user?.moveEnHolding ?: 0
    val rachaDias     = user?.diasConsecutivosVideos ?: 0
    val photoUrl      = user?.profilePhotoUrl ?: ""
    val badges        = user?.badges ?: emptyList()

    FlowlyScaffold(navController = navController, currentRoute = Routes.PROFILE) { padding ->
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
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = FlowlyText,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Profile card
            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar con foto o iniciales
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
                                model             = photoUrl,
                                contentDescription = "Foto de perfil",
                                modifier          = Modifier.fillMaxSize(),
                                contentScale      = ContentScale.Crop
                            )
                        } else {
                            Text(iniciales, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(nombre, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlowlyText)
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

            // Mis datos
            SectionLabel("mis datos", modifier = Modifier.padding(horizontal = 16.dp))

            FlowlyCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                DataRow("Email",               email)
                FlowlySeparator()
                DataRow("Teléfono",            telefono.ifBlank { "No configurado" })
                FlowlySeparator()
                DataRow("Alias Mercado Pago",  aliasMP.ifBlank { "No configurado" }, valueColor = FlowlyAccent)
            }

            Spacer(Modifier.height(8.dp))

            // Insignias
            SectionLabel("insignias", modifier = Modifier.padding(horizontal = 16.dp))

            if (badges.isEmpty()) {
                FlowlyCard2(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Aún no tenés insignias. ¡Completá actividades para ganarlas!",
                        fontSize = 12.sp,
                        color    = FlowlyMuted
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    badges.take(4).forEach { badgeId ->
                        val insignia = TODAS_LAS_INSIGNIAS[badgeId]
                        if (insignia != null) {
                            Column(
                                modifier = Modifier
                                    .background(FlowlyCard2, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(insignia.emoji, fontSize = 20.sp)
                                Text(
                                    insignia.titulo.take(8),
                                    fontSize = 9.sp,
                                    color    = FlowlyMuted
                                )
                            }
                        }
                    }
                    if (badges.size > 4) {
                        Column(
                            modifier = Modifier
                                .background(FlowlyCard2, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("+${badges.size - 4}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FlowlyAccent)
                            Text("más", fontSize = 9.sp, color = FlowlyMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

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
        text.uppercase(),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = Color(0xFF4B6B4B),
        letterSpacing = 1.sp,
        modifier      = modifier.padding(top = 14.dp, bottom = 8.dp)
    )
}

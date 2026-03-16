package com.flowly.move.ui.screens.rankings

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
import com.flowly.move.data.model.User
import com.flowly.move.ui.components.*
import com.flowly.move.ui.navigation.Routes
import com.flowly.move.ui.theme.*

@Composable
fun RankingsScreen(navController: NavController) {
    val vm: RankingsViewModel = viewModel()
    val currentUser by vm.currentUser.collectAsStateWithLifecycle()
    val rankings    by vm.rankings.collectAsStateWithLifecycle()
    val isLoading   by vm.isLoading.collectAsStateWithLifecycle()

    // Default tab 2 = Argentina ("all"), que coincide con la carga inicial del ViewModel
    var tabIndex by remember { mutableIntStateOf(2) }
    val tabs      = listOf("Mi ciudad", "Mi provincia", "Argentina")
    val scopeKeys = listOf("ciudad",    "provincia",    "all")

    val subtitle = when (tabIndex) {
        0 -> currentUser?.ciudad?.ifBlank { "tu ciudad" } ?: "tu ciudad"
        1 -> currentUser?.provincia?.ifBlank { "tu provincia" } ?: "tu provincia"
        else -> "Argentina"
    }

    FlowlyScaffold(navController = navController, currentRoute = Routes.RANKINGS) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Rankings",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = FlowlyText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(FlowlyCard2, RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                tabs.forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (tabIndex == i) FlowlyCard else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                tabIndex = i
                                vm.loadRankings(scopeKeys[i])
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tabIndex == i) FlowlyText else FlowlyMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "$subtitle · Top 50",
                fontSize = 12.sp,
                color = FlowlyMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FlowlyAccent)
                }
            } else if (rankings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay usuarios en este ranking todavía.",
                        fontSize = 14.sp,
                        color = FlowlyMuted
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    rankings.forEachIndexed { index, user ->
                        val isMe = user.uid == currentUser?.uid
                        RankingRow(
                            pos       = index + 1,
                            user      = user,
                            isMe      = isMe
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RankingRow(pos: Int, user: User, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMe)
                    Modifier
                        .background(FlowlyAccent.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                        .border(1.dp, FlowlyAccent.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                else
                    Modifier.padding(vertical = 10.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Posición
        Text(
            "#$pos",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when (pos) {
                1    -> Color(0xFFF59E0B)
                2    -> Color(0xFF9CA3AF)
                3    -> Color(0xFFCD7F32)
                else -> if (isMe) FlowlyAccent else FlowlyMuted
            },
            modifier = Modifier.width(28.dp)
        )

        // Avatar (foto de perfil si tiene, sino iniciales)
        if (user.profilePhotoUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(FlowlyCard2)
            ) {
                AsyncImage(
                    model             = user.profilePhotoUrl,
                    contentDescription = "Foto de ${user.nombre}",
                    modifier          = Modifier.fillMaxSize(),
                    contentScale      = ContentScale.Crop
                )
            }
        } else {
            FlowlyAvatar(initials = user.iniciales, size = 34.dp)
        }

        // Nombre y MOVE
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isMe) "Vos · ${user.nombre}" else user.nombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isMe) FlowlyAccent else FlowlyText
            )
            Text("%,d MOVE".format(user.tokensActuales), fontSize = 12.sp, color = FlowlyMuted)
        }

        // Medalla
        when (pos) {
            1    -> Text("🥇", fontSize = 18.sp)
            2    -> Text("🥈", fontSize = 18.sp)
            3    -> Text("🥉", fontSize = 18.sp)
            else -> if (isMe) TagGreen("tú")
        }
    }
}

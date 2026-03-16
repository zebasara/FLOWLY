package com.flowly.move.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowly.move.ui.components.*
import com.flowly.move.ui.theme.*

private data class OnboardingPage(
    val icon: String,
    val title: String,
    val body: String,
    val extra: @Composable (() -> Unit)? = null
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPage(
            icon  = "🚶",
            title = "Caminá y ganá",
            body  = "Tu movimiento genera tokens MOVE reales canjeables por plata en Mercado Pago."
        ),
        OnboardingPage(
            icon  = "📺",
            title = "Mirá videos, subí tu límite",
            body  = "2 videos por día durante 5 días consecutivos sube tu límite. También necesitás el 75% del límite actual acumulado.",
            extra = {
                Spacer(Modifier.height(16.dp))
                FlowlyCard2 {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nivel 1",      fontSize = 12.sp, color = FlowlyMuted)
                        Text("2.000 MOVE",   fontSize = 12.sp, color = FlowlyAccent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nivel 5",      fontSize = 12.sp, color = FlowlyMuted)
                        Text("28.000 MOVE",  fontSize = 12.sp, color = FlowlyAccent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nivel 10 máx.", fontSize = 12.sp, color = FlowlyMuted)
                        Text("100.000 MOVE", fontSize = 12.sp, color = FlowlyAccent)
                    }
                }
            }
        ),
        OnboardingPage(
            icon  = "💸",
            title = "Cobrá en Mercado Pago",
            body  = "Canjeá tus MOVE por transferencias reales o productos. O holdealos para ganar interés.",
            extra = {
                Spacer(Modifier.height(16.dp))
                FlowlyCard2 {
                    listOf(
                        Triple("\$2.000 ARS",  "33.600 MOVE",  FlowlySuccess),
                        Triple("\$5.000 ARS",  "84.000 MOVE",  FlowlySuccess),
                        Triple("\$10.000 ARS", "168.000 MOVE", FlowlySuccess)
                    ).forEachIndexed { i, (label, move, color) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = if (i > 0) 4.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 12.sp, color = FlowlyMuted)
                            TagGreen(move)
                        }
                    }
                }
            }
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowlyBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color(0xFF1A3A0A),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(pages[page].icon, fontSize = 36.sp)
            }

            if (page == 0) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Flowly",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowlyAccent,
                    letterSpacing = (-1).sp
                )
                Text("moverse tiene recompensa", fontSize = 13.sp, color = FlowlyMuted)
            }

            Spacer(Modifier.height(24.dp))

            // Content
            AnimatedContent(targetState = page) { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        pages[p].title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlowlyText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        pages[p].body,
                        fontSize = 14.sp,
                        color = FlowlyMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                    pages[p].extra?.invoke()
                }
            }

            Spacer(Modifier.weight(1f))

            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (i == page) 20.dp else 8.dp)
                            .background(
                                if (i == page) FlowlyAccent else FlowlyBorder,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Button
            if (page < 2) {
                FlowlyPrimaryButton(text = "Siguiente", onClick = { page++ })
            } else {
                FlowlyPrimaryButton(text = "Crear mi cuenta", onClick = onDone)
            }

            Spacer(Modifier.height(16.dp))

            if (page == 0) {
                Text(
                    "Ya tengo cuenta",
                    fontSize = 14.sp,
                    color = FlowlyMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDone)
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

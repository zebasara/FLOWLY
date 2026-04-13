package com.flowly.move.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowly.move.ui.components.*
import com.flowly.move.ui.theme.*

private val OnboardingGlow = object : ShaderBrush() {
    override fun createShader(size: androidx.compose.ui.geometry.Size) =
        RadialGradientShader(
            colors = listOf(Color(0x1E7EE621), Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
            radius = size.width * 0.8f
        )
}

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
            icon  = "⚡",
            title = "Cada paso, un MOVE",
            body  = "Caminá, completá misiones y sumá rachas. Tu actividad diaria se convierte en tokens MOVE que podés usar dentro del ecosistema Flowly."
        ),
        OnboardingPage(
            icon  = "📺",
            title = "Mirá videos, subí tu límite",
            body  = "2 videos por día durante 5 días consecutivos sube tu límite. También necesitás el 75% del límite actual acumulado.",
            extra = {
                Spacer(Modifier.height(20.dp))
                FlowlyCard {
                    listOf(
                        "Nivel 1"  to "20.000 MOVE",
                        "Nivel 5"  to "210.000 MOVE",
                        "Nivel 10" to "1.000.000 MOVE"
                    ).forEachIndexed { i, (nivel, limite) ->
                        if (i > 0) FlowlySeparator(Modifier.padding(vertical = 0.dp))
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(nivel,  fontSize = 13.sp, color = FlowlyTextSub)
                            Text(limite, fontSize = 13.sp, color = FlowlyAccent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        ),
        OnboardingPage(
            icon  = "🎁",
            title = "Tus MOVE valen en serio",
            body  = "Canjeá en la tienda, competí por el fondo mensual y accedé a recompensas exclusivas cuanto más alto llegues.",
            extra = {
                Spacer(Modifier.height(20.dp))
                FlowlyCard {
                    listOf(
                        Triple("🛍️", "Tienda",        "Productos exclusivos"),
                        Triple("🏆", "Fondo mensual", "Top 10 del mes"),
                        Triple("🔒", "Holding",       "Beneficio extra por nivel")
                    ).forEachIndexed { i, (emoji, titulo, desc) ->
                        if (i > 0) FlowlySeparator(Modifier.padding(vertical = 0.dp))
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 15.sp)
                                Text(titulo, fontSize = 13.sp, color = FlowlyTextSub)
                            }
                            TagGreen(desc)
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
        // Glow atmosférico — firma visual Flowly
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(OnboardingGlow)
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // Logo / ícono de página
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.linearGradient(listOf(FlowlyAccent, FlowlyAccentDark)),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(pages[page].icon, fontSize = 32.sp)
            }

            if (page == 0) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Flowly",
                    fontSize      = 38.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = FlowlyAccent,
                    letterSpacing = (-1.5).sp
                )
                Text(
                    "moverse tiene recompensa",
                    fontSize = 14.sp,
                    color    = FlowlyMuted
                )
            }

            Spacer(Modifier.height(28.dp))

            // Contenido con animación de slide
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(200)))
                },
                label = "onboarding"
            ) { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        pages[p].title,
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color      = FlowlyText,
                        textAlign  = TextAlign.Center,
                        lineHeight = 30.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        pages[p].body,
                        fontSize   = 15.sp,
                        color      = FlowlyMuted,
                        textAlign  = TextAlign.Center,
                        lineHeight = 23.sp
                    )
                    pages[p].extra?.invoke()
                }
            }

            Spacer(Modifier.weight(1f))

            // Indicadores de página (pill dots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (i == page) 28.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == page) FlowlyAccent else FlowlyBorder)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // CTA principal
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
                    color    = FlowlyMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDone)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            } else {
                Spacer(Modifier.height(38.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

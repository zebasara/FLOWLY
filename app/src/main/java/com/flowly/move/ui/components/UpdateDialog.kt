package com.flowly.move.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flowly.move.ui.theme.*

/**
 * Diálogo de actualización disponible.
 *
 * - Se muestra sobre cualquier pantalla al abrir la app.
 * - "Actualizar ahora" → abre Play Store.
 * - "Más tarde" → cierra por esta sesión; al próximo inicio se vuelve a mostrar.
 *
 * @param versionName  Nombre de versión a mostrar (ej: "1.2.0"). Vacío = no se muestra.
 * @param message      Mensaje personalizado desde Firestore. Vacío = texto por defecto.
 * @param onUpdate     Callback al presionar "Actualizar ahora".
 * @param onDismiss    Callback al presionar "Más tarde".
 */
@Composable
fun UpdateAvailableDialog(
    versionName : String = "",
    message     : String = "",
    onUpdate    : () -> Unit,
    onDismiss   : () -> Unit
) {
    // ── Pulso animado del ícono ────────────────────────────────────────────
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.14f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    // ── Brillo del borde animado ───────────────────────────────────────────
    val glowAlpha by pulse.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1100, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress    = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(FlowlyCard, FlowlyBg)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            FlowlyAccent.copy(alpha = glowAlpha),
                            FlowlyAccent.copy(alpha = 0.08f),
                            FlowlyAccent.copy(alpha = glowAlpha * 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // ── Ícono animado ──────────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    FlowlyAccent.copy(alpha = 0.18f),
                                    FlowlyAccent.copy(alpha = 0.04f)
                                )
                            )
                        )
                        .border(1.5.dp, FlowlyAccent.copy(alpha = glowAlpha * 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint               = FlowlyAccent,
                        modifier           = Modifier.size(42.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Badge de versión ───────────────────────────────────────
                if (versionName.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(FlowlyAccent.copy(alpha = 0.12f))
                            .border(1.dp, FlowlyAccent.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text       = "v$versionName",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = FlowlyAccent
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // ── Título ─────────────────────────────────────────────────
                Text(
                    text       = "¡Nueva versión disponible!",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FlowlyText,
                    textAlign  = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(10.dp))

                // ── Mensaje (personalizable o por defecto) ─────────────────
                Text(
                    text      = message.ifBlank {
                        "Hay mejoras, nuevas funciones y correcciones esperándote.\nActualizá para disfrutar la mejor experiencia."
                    },
                    fontSize   = 14.sp,
                    color      = FlowlyMuted,
                    textAlign  = TextAlign.Center,
                    lineHeight = 21.sp
                )

                Spacer(Modifier.height(24.dp))

                // ── Divisor ────────────────────────────────────────────────
                HorizontalDivider(
                    color     = FlowlyAccent.copy(alpha = 0.12f),
                    thickness = 1.dp
                )

                Spacer(Modifier.height(24.dp))

                // ── Botón principal — Actualizar ahora ─────────────────────
                Button(
                    onClick  = onUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlowlyAccent,
                        contentColor   = FlowlyBg
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = "Actualizar ahora",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        letterSpacing = 0.3.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Botón secundario — Más tarde ───────────────────────────
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text     = "Más tarde",
                        color    = FlowlyMuted,
                        fontSize = 14.sp
                    )
                }

                // ── Nota inferior ──────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = "Se te recordará al próximo inicio",
                    fontSize  = 11.sp,
                    color     = FlowlyMuted.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

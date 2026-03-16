package com.flowly.move.data.model

data class Insignia(
    val id: String,
    val emoji: String,
    val titulo: String,
    val descripcion: String
)

val TODAS_LAS_INSIGNIAS: Map<String, Insignia> = mapOf(
    "soy_move" to Insignia(
        id          = "soy_move",
        emoji       = "🏅",
        titulo      = "¡Soy MOVE!",
        descripcion = "Te uniste a la comunidad MOVE. ¡Bienvenido!"
    ),
    "primer_video" to Insignia(
        id          = "primer_video",
        emoji       = "🎬",
        titulo      = "Primer video",
        descripcion = "Completaste tu primer video y ganaste MOVE."
    ),
    "primer_canje" to Insignia(
        id          = "primer_canje",
        emoji       = "💸",
        titulo      = "Primer canje",
        descripcion = "Realizaste tu primer canje de MOVE."
    ),
    "primer_holding" to Insignia(
        id          = "primer_holding",
        emoji       = "🔒",
        titulo      = "Primer holding",
        descripcion = "Bloqueaste MOVE por primera vez y ganás interés."
    ),
    "primer_referido" to Insignia(
        id          = "primer_referido",
        emoji       = "👥",
        titulo      = "Primer referido",
        descripcion = "Invitaste a tu primer amigo a MOVE."
    ),
    "5_referidos" to Insignia(
        id          = "5_referidos",
        emoji       = "🌟",
        titulo      = "5 referidos",
        descripcion = "¡Invitaste a 5 amigos! Sos un embajador MOVE."
    ),
    "nivel_5" to Insignia(
        id          = "nivel_5",
        emoji       = "🚀",
        titulo      = "Nivel 5",
        descripcion = "Alcanzaste el nivel 5. ¡Seguí creciendo!"
    ),
    "100km" to Insignia(
        id          = "100km",
        emoji       = "🏃",
        titulo      = "100 km recorridos",
        descripcion = "Registraste 100 km de actividad física con MOVE."
    )
)

package com.flowly.move.data.model

data class Insignia(
    val id: String,
    val emoji: String,
    val titulo: String,
    val descripcion: String
)

val TODAS_LAS_INSIGNIAS: Map<String, Insignia> = mapOf(

    // ── Registro ────────────────────────────────────────────────────
    "soy_move" to Insignia(
        id          = "soy_move",
        emoji       = "🏅",
        titulo      = "¡Soy MOVE!",
        descripcion = "Te uniste a la comunidad MOVE. ¡Bienvenido!"
    ),

    // ── Videos ──────────────────────────────────────────────────────
    "primer_video" to Insignia(
        id          = "primer_video",
        emoji       = "🎬",
        titulo      = "Primer video",
        descripcion = "Completaste tu primer video y ganaste MOVE."
    ),
    "video_fan" to Insignia(
        id          = "video_fan",
        emoji       = "📺",
        titulo      = "Video fan",
        descripcion = "Completaste 10 videos. ¡Seguí así!"
    ),
    "video_adicto" to Insignia(
        id          = "video_adicto",
        emoji       = "🎥",
        titulo      = "Video adicto",
        descripcion = "50 videos completados. Sos un experto MOVE."
    ),

    // ── Racha de videos ─────────────────────────────────────────────
    "racha_5_videos" to Insignia(
        id          = "racha_5_videos",
        emoji       = "🔥",
        titulo      = "Racha 5 días",
        descripcion = "5 días seguidos viendo videos. ¡Imparable!"
    ),
    "racha_7_videos" to Insignia(
        id          = "racha_7_videos",
        emoji       = "⚡",
        titulo      = "Racha 7 días",
        descripcion = "¡7 días consecutivos de videos! Sos una máquina."
    ),

    // ── Canjes ──────────────────────────────────────────────────────
    "primer_canje" to Insignia(
        id          = "primer_canje",
        emoji       = "🎁",
        titulo      = "Primera recompensa",
        descripcion = "Solicitaste tu primera recompensa. ¡El ecosistema Flowly está funcionando para vos!"
    ),

    // ── Holding ─────────────────────────────────────────────────────
    "primer_holding" to Insignia(
        id          = "primer_holding",
        emoji       = "🔒",
        titulo      = "Primer holding",
        descripcion = "Bloqueaste MOVE por primera vez y ganás interés."
    ),
    "gran_inversor" to Insignia(
        id          = "gran_inversor",
        emoji       = "💰",
        titulo      = "Gran inversor",
        descripcion = "Tenés 10.000 MOVE en holding. ¡Una fortuna!"
    ),

    // ── Referidos ───────────────────────────────────────────────────
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
    "10_referidos" to Insignia(
        id          = "10_referidos",
        emoji       = "👑",
        titulo      = "10 referidos",
        descripcion = "10 amigos en MOVE gracias a vos. ¡Leyenda!"
    ),

    // ── Movimiento / GPS ────────────────────────────────────────────
    "explorador" to Insignia(
        id          = "explorador",
        emoji       = "🗺️",
        titulo      = "Explorador",
        descripcion = "Recorriste 5 km con MOVE. ¡A explorar!"
    ),
    "gran_explorador" to Insignia(
        id          = "gran_explorador",
        emoji       = "🧭",
        titulo      = "Gran explorador",
        descripcion = "50 km recorridos con MOVE. Conocés cada rincón."
    ),
    "100km" to Insignia(
        id          = "100km",
        emoji       = "🏃",
        titulo      = "100 km",
        descripcion = "¡Registraste 100 km de actividad física con MOVE!"
    ),
    "maratonista" to Insignia(
        id          = "maratonista",
        emoji       = "🥇",
        titulo      = "Maratonista",
        descripcion = "200 km recorridos. Sos un atleta MOVE."
    ),

    // ── Niveles ─────────────────────────────────────────────────────
    "nivel_5" to Insignia(
        id          = "nivel_5",
        emoji       = "🚀",
        titulo      = "Nivel 5",
        descripcion = "Alcanzaste el nivel 5. ¡La élite MOVE!"
    )
)

package com.flowly.move.data.model

data class User(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val provincia: String = "",
    val ciudad: String = "",
    val aliasMercadoPago: String = "",
    // Sistema de niveles
    val nivel: Int = 1,
    val limiteTokens: Int = 2_000,
    val tokensActuales: Int = 0,
    // Progreso de nivel (doble condición)
    val diasConsecutivosVideos: Int = 0,
    val videosAcumuladosNivel: Int = 0,   // resetea al subir de nivel
    val saldoMinimoParaSubir: Int = 1_500, // 75% del limiteTokens actual
    // Actividad diaria
    val tokenMovimientoHoy: Int = 0,
    val tokenVideosHoy: Int = 0,
    val move30Dias: Int = 0,
    // Holding
    val moveEnHolding: Int = 0,
    // Referidos
    val totalReferidos: Int = 0,
    val referidoPor: String = "",
    // Insignias
    val badges: List<String> = emptyList(),
    // Metadata
    val createdAt: Long = 0L,
    val profilePhotoUrl: String = ""
) {
    val iniciales: String get() {
        val parts = nombre.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "??"
        }
    }
}
